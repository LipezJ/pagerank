package com.pagerank.pagerank.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;

import com.pagerank.pagerank.domain.model.Follow;
import com.pagerank.pagerank.domain.model.Person;
import com.pagerank.pagerank.domain.model.Rank;
import com.pagerank.pagerank.domain.model.RankDelta;
import com.pagerank.pagerank.domain.repository.FollowRepository;
import com.pagerank.pagerank.domain.repository.PersonRepository;
import com.pagerank.pagerank.domain.repository.RankDeltaRepository;
import com.pagerank.pagerank.domain.repository.RankRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional
public class GraphService {

	private final PersonRepository personRepository;
	private final FollowRepository followRepository;
	private final RankRepository rankRepository;
	private final RankDeltaRepository rankDeltaRepository;

	public GraphService(
			PersonRepository personRepository,
			FollowRepository followRepository,
			RankRepository rankRepository,
			RankDeltaRepository rankDeltaRepository) {
		this.personRepository = personRepository;
		this.followRepository = followRepository;
		this.rankRepository = rankRepository;
		this.rankDeltaRepository = rankDeltaRepository;
	}

	public Person registerPerson(String name, double spamScore) {
		return registerPerson(name, spamScore, Instant.now());
	}

	public Person registerPerson(String name, double spamScore, Instant lastSeen) {
		Instant effectiveLastSeen = lastSeen != null ? lastSeen : Instant.now();
		Person person = new Person(name, spamScore, effectiveLastSeen);
		return personRepository.save(person);
	}

	@Transactional(readOnly = true)
	public Person getPerson(Long personId) {
		return requirePerson(personId);
	}

	public Person touchPerson(Long personId, Instant lastSeen) {
		Person person = requirePerson(personId);
		person.setLastSeen(lastSeen != null ? lastSeen : Instant.now());
		return personRepository.save(person);
	}

	public Follow registerFollow(Long sourceId, Long targetId, double quality, Instant lastSeen) {
		Person source = requirePerson(sourceId);
		Person target = requirePerson(targetId);
		Instant effectiveLastSeen = lastSeen != null ? lastSeen : Instant.now();

		return followRepository.findBySourceIdAndTargetId(sourceId, targetId)
				.map(existing -> updateFollow(existing, source, target, quality, effectiveLastSeen))
				.orElseGet(() -> followRepository.save(new Follow(source, target, quality, effectiveLastSeen)));
	}

	@Transactional(readOnly = true)
	public List<Person> getAllPersonsSorted() {
		return personRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
	}

	private Follow updateFollow(Follow follow, Person source, Person target, double quality, Instant lastSeen) {
		follow.setSource(source);
		follow.setTarget(target);
		follow.setQuality(quality);
		follow.setLastSeen(lastSeen);
		return followRepository.save(follow);
	}

	public Rank upsertRank(Long personId, double score) {
		Person person = requirePerson(personId);
		Instant now = Instant.now();
		Rank rank = rankRepository.findByPerson(person)
				.orElse(new Rank(person, score, now));
		rank.setPerson(person);
		rank.setScore(score);
		rank.setUpdatedAt(now);
		return rankRepository.save(rank);
	}

	public RankDelta upsertRankDelta(Long personId, double delta) {
		Person person = requirePerson(personId);
		RankDelta rankDelta = rankDeltaRepository.findByPerson(person)
				.orElse(new RankDelta(person, delta));
		rankDelta.setPerson(person);
		rankDelta.setDelta(delta);
		return rankDeltaRepository.save(rankDelta);
	}

	@Transactional(readOnly = true)
	public List<Follow> getOutgoingFollows(Long personId) {
		return followRepository.findBySourceId(personId);
	}

	@Transactional(readOnly = true)
	public List<Follow> getIncomingFollows(Long personId) {
		return followRepository.findByTargetId(personId);
	}

	public Set<Long> syncOutgoingFollows(Long personId, Set<Long> keepTargetIds) {
		Person person = requirePerson(personId);
		List<Follow> outgoing = followRepository.findBySourceId(personId);

		Set<Long> keepTargets = keepTargetIds != null ? new HashSet<>(keepTargetIds) : Set.of();
		List<Follow> toDelete = new ArrayList<>();
		for (Follow follow : outgoing) {
			if (!keepTargets.contains(follow.getTarget().getId())) {
				toDelete.add(follow);
			}
		}
		if (!toDelete.isEmpty()) {
			followRepository.deleteAll(toDelete);
		}

		Set<Long> touched = new HashSet<>();
		touched.add(person.getId());
		for (Follow follow : toDelete) {
			touched.add(follow.getTarget().getId());
		}
		return touched;
	}

	private Person requirePerson(Long personId) {
		return personRepository.findById(personId)
				.orElseThrow(() -> new EntityNotFoundException("Person " + personId + " not found"));
	}
}
