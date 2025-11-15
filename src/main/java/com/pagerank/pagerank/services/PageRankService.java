package com.pagerank.pagerank.services;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pagerank.pagerank.domain.model.Follow;
import com.pagerank.pagerank.domain.model.PageRankResult;
import com.pagerank.pagerank.domain.model.Person;
import com.pagerank.pagerank.domain.model.Rank;
import com.pagerank.pagerank.domain.model.RankDelta;
import com.pagerank.pagerank.domain.repository.FollowRepository;
import com.pagerank.pagerank.domain.repository.PersonRepository;
import com.pagerank.pagerank.domain.repository.RankDeltaRepository;
import com.pagerank.pagerank.domain.repository.RankRepository;
import com.pagerank.pagerank.settings.PagerankSettingsProperties;

@Service
public class PageRankService {

	private static final Logger log = LoggerFactory.getLogger(PageRankService.class);

	private final PersonRepository personRepository;
	private final FollowRepository followRepository;
	private final RankRepository rankRepository;
	private final RankDeltaRepository rankDeltaRepository;
	private final PagerankSettingsProperties settings;
	private final AtomicReference<PageRankResult> lastResult = new AtomicReference<>();

	public PageRankService(
			PersonRepository personRepository,
			FollowRepository followRepository,
			RankRepository rankRepository,
			RankDeltaRepository rankDeltaRepository,
			PagerankSettingsProperties settings) {
		this.personRepository = personRepository;
		this.followRepository = followRepository;
		this.rankRepository = rankRepository;
		this.rankDeltaRepository = rankDeltaRepository;
		this.settings = settings;
	}

	@Transactional
	public PageRankResult runBatchComputation() {
		GraphSnapshot snapshot = snapshotGraph();
		if (snapshot.nodeCount() == 0) {
			PageRankResult empty = new PageRankResult("batch", 0, 0.0, 0, true, false, Duration.ZERO);
			lastResult.set(empty);
			return empty;
		}

		ComputationOutcome outcome = compute(snapshot, null, settings.maxIters(), settings.maxUpdateDuration());
		persistRanks(snapshot.persons(), outcome.scores());

		log.info("PageRank batch completed: nodes={}, iterations={}, avgDelta={}, converged={}, elapsed={} ms",
				snapshot.nodeCount(),
				outcome.iterations(),
				String.format(Locale.US, "%.6f", outcome.averageDelta()),
				outcome.converged(),
				outcome.elapsed().toMillis());

		PageRankResult result = new PageRankResult("batch", outcome.iterations(), outcome.averageDelta(),
				snapshot.nodeCount(), outcome.converged(), outcome.timeLimited(), outcome.elapsed());
		lastResult.set(result);
		return result;
	}

	@Transactional
	public PageRankResult runIncrementalUpdate(Iterable<Long> touchedPersonIds) {
		if (rankRepository.count() == 0) {
			log.info("No ranks stored yet, running full batch instead of incremental");
			return runBatchComputation();
		}
		GraphSnapshot snapshot = snapshotGraph();
		if (snapshot.nodeCount() == 0) {
			PageRankResult empty = new PageRankResult("incremental", 0, 0.0, 0, true, false, Duration.ZERO);
			lastResult.set(empty);
			return empty;
		}

		Set<Long> touched = new HashSet<>();
		if (touchedPersonIds != null) {
			touchedPersonIds.forEach(id -> {
				if (id != null) {
					touched.add(id);
				}
			});
		}
		Set<Long> expanded = expandTouchedWithNeighbors(snapshot, touched);
		double ratio = snapshot.nodeCount() == 0 ? 0.0 : (double) touched.size() / snapshot.nodeCount();
		double expandedRatio = snapshot.nodeCount() == 0 ? 0.0 : (double) expanded.size() / snapshot.nodeCount();
		if (ratio > 0.4 || expandedRatio > 0.6) {
			log.info("Touched ratio {} too high, running full batch", ratio);
			return runBatchComputation();
		}

		double[] initialScores = buildInitialScores(snapshot);
		int maxIterations = Math.max(3, Math.min(10, settings.maxIters() / 4));
		ComputationOutcome outcome = compute(snapshot, initialScores, maxIterations, settings.maxUpdateDuration());
		persistRanks(snapshot.persons(), outcome.scores());

		log.info("Incremental PageRank executed for {} touched nodes -> iterations={}, avgDelta={}, elapsed={} ms",
				touched.size(),
				outcome.iterations(),
				String.format(Locale.US, "%.6f", outcome.averageDelta()),
				outcome.elapsed().toMillis());

		PageRankResult result = new PageRankResult("incremental", outcome.iterations(), outcome.averageDelta(),
				snapshot.nodeCount(), outcome.converged(), outcome.timeLimited(), outcome.elapsed());
		lastResult.set(result);
		return result;
	}

	@Transactional(readOnly = true)
	public PageRankResult getLastResult() {
		return lastResult.get();
	}

	private GraphSnapshot snapshotGraph() {
		List<Person> persons = personRepository.findAll();
		int nodeCount = persons.size();
		Map<Long, Integer> indexMap = new HashMap<>(nodeCount);
		for (int i = 0; i < nodeCount; i++) {
			indexMap.put(persons.get(i).getId(), i);
		}
		List<List<Edge>> adjacency = new ArrayList<>(nodeCount);
		List<List<Edge>> incoming = new ArrayList<>(nodeCount);
		double[] outgoingWeight = new double[nodeCount];
		for (int i = 0; i < nodeCount; i++) {
			adjacency.add(new ArrayList<>());
			incoming.add(new ArrayList<>());
		}
		if (nodeCount > 0) {
			for (Follow follow : followRepository.findAll()) {
				Integer sourceIndex = indexMap.get(follow.getSource().getId());
				Integer targetIndex = indexMap.get(follow.getTarget().getId());
				if (sourceIndex == null || targetIndex == null) {
					continue;
				}
				double weight = Math.max(0.0, follow.getQuality());
			if (weight == 0.0) {
				continue;
			}
			adjacency.get(sourceIndex).add(new Edge(targetIndex, weight));
			incoming.get(targetIndex).add(new Edge(sourceIndex, weight));
			outgoingWeight[sourceIndex] += weight;
		}
	}
	return new GraphSnapshot(persons, indexMap, adjacency, incoming, outgoingWeight);
}

	private Set<Long> expandTouchedWithNeighbors(GraphSnapshot snapshot, Set<Long> touchedIds) {
		if (touchedIds == null || touchedIds.isEmpty()) {
			return Set.of();
		}
		Set<Long> result = new HashSet<>(touchedIds);
		for (Long id : touchedIds) {
			Integer idx = snapshot.indexMap().get(id);
			if (idx == null) {
				continue;
			}
			for (Edge e : snapshot.adjacency().get(idx)) {
				result.add(snapshot.persons().get(e.nodeIndex()).getId());
			}
			for (Edge e : snapshot.incoming().get(idx)) {
				result.add(snapshot.persons().get(e.nodeIndex()).getId());
			}
		}
		return result;
	}

	private double[] buildInitialScores(GraphSnapshot snapshot) {
		int nodeCount = snapshot.nodeCount();
		if (nodeCount == 0) {
			return new double[0];
		}

		double[] initial = new double[nodeCount];
		List<Rank> ranks = rankRepository.findAll();
		double total = 0.0;
		for (Rank rank : ranks) {
			Integer index = snapshot.indexMap().get(rank.getId());
			if (index == null) {
				continue;
			}
			double score = Math.max(0.0, rank.getScore());
			initial[index] = score;
			total += score;
		}

		if (total == 0.0) {
			Arrays.fill(initial, 1.0 / nodeCount);
			return initial;
		}

		for (int i = 0; i < nodeCount; i++) {
			initial[i] /= total;
		}
		return initial;
	}

	private ComputationOutcome compute(GraphSnapshot snapshot, double[] initialScores, int maxIterations,
			Duration maxDuration) {
		int nodeCount = snapshot.nodeCount();
		if (nodeCount == 0) {
			return new ComputationOutcome(new double[0], 0, 0.0, true, false, Duration.ZERO);
		}

		double[] current = initialScores != null && initialScores.length == nodeCount
				? Arrays.copyOf(initialScores, nodeCount)
				: uniformVector(nodeCount);
		double[] next = new double[nodeCount];

		double damping = settings.damping();
		double epsilon = settings.epsilon();
		double teleport = (1.0 - damping) / nodeCount;

		int iterations = 0;
		double lastAverageDelta = Double.MAX_VALUE;
		boolean converged = false;
		boolean timeLimited = false;
		Instant start = Instant.now();

		while (iterations < maxIterations) {
			if (maxDuration != null && Duration.between(start, Instant.now()).compareTo(maxDuration) > 0) {
				timeLimited = true;
				break;
			}
			Arrays.fill(next, teleport);
			double danglingMass = 0.0;

			for (int i = 0; i < nodeCount; i++) {
				double rankValue = current[i];
				double weightSum = snapshot.outgoingWeight()[i];
				if (weightSum <= 0.0 || snapshot.adjacency().get(i).isEmpty()) {
					danglingMass += rankValue;
					continue;
				}
				double contribution = damping * rankValue / weightSum;
				for (Edge edge : snapshot.adjacency().get(i)) {
					next[edge.nodeIndex()] += contribution * edge.weight();
				}
			}

			double danglingContribution = damping * danglingMass / nodeCount;
			for (int i = 0; i < nodeCount; i++) {
				next[i] += danglingContribution;
			}

			double deltaSum = 0.0;
			for (int i = 0; i < nodeCount; i++) {
				deltaSum += Math.abs(next[i] - current[i]);
			}
			lastAverageDelta = deltaSum / nodeCount;
			System.arraycopy(next, 0, current, 0, nodeCount);

			iterations++;
			if (lastAverageDelta <= epsilon) {
				converged = true;
				break;
			}
		}

		Duration elapsed = Duration.between(start, Instant.now());
		return new ComputationOutcome(current, iterations, lastAverageDelta, converged, timeLimited, elapsed);
	}

	private double[] uniformVector(int size) {
		double[] vector = new double[size];
		if (size == 0) {
			return vector;
		}
		double value = 1.0 / size;
		Arrays.fill(vector, value);
		return vector;
	}

	private void persistRanks(List<Person> persons, double[] scores) {
		Map<Long, Rank> existingRanks = rankRepository.findAll().stream()
				.collect(Collectors.toMap(Rank::getId, Function.identity()));
		Map<Long, RankDelta> existingDeltas = rankDeltaRepository.findAll().stream()
				.collect(Collectors.toMap(RankDelta::getId, Function.identity()));

		List<Rank> updatedRanks = new ArrayList<>(persons.size());
		List<RankDelta> updatedDeltas = new ArrayList<>(persons.size());
		Instant now = Instant.now();

		for (int i = 0; i < persons.size(); i++) {
			Person person = persons.get(i);
			double newScore = scores.length > i ? scores[i] : 0.0;
			Rank rank = existingRanks.get(person.getId());
			double previousScore = 0.0;
			if (rank == null) {
				rank = new Rank(person, newScore, now);
			}
			else {
				previousScore = rank.getScore();
				rank.setPerson(person);
				rank.setScore(newScore);
				rank.setUpdatedAt(now);
			}
			updatedRanks.add(rank);

			double delta = newScore - previousScore;
			RankDelta rankDelta = existingDeltas.get(person.getId());
			if (rankDelta == null) {
				rankDelta = new RankDelta(person, delta);
			}
			else {
				rankDelta.setPerson(person);
				rankDelta.setDelta(delta);
			}
			updatedDeltas.add(rankDelta);
		}

		rankRepository.saveAll(updatedRanks);
		rankDeltaRepository.saveAll(updatedDeltas);
	}

	/**
	 * Represents an edge to a neighbor node index with its weight.
	 */
	private record Edge(int nodeIndex, double weight) {
	}

	private record GraphSnapshot(
			List<Person> persons,
			Map<Long, Integer> indexMap,
			List<List<Edge>> adjacency,
			List<List<Edge>> incoming,
			double[] outgoingWeight) {

		int nodeCount() {
			return persons.size();
		}
	}

	private record ComputationOutcome(
			double[] scores,
			int iterations,
			double averageDelta,
			boolean converged,
			boolean timeLimited,
			Duration elapsed) {
	}
}
