package com.pagerank.pagerank.services;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.pagerank.pagerank.domain.model.Follow;
import com.pagerank.pagerank.domain.model.Person;
import com.pagerank.pagerank.domain.repository.FollowRepository;
import com.pagerank.pagerank.domain.repository.PersonRepository;
import com.pagerank.pagerank.domain.repository.RankRepository;
import com.pagerank.pagerank.settings.PagerankSettingsProperties;

@Service
@Transactional
public class IngestionService {

	private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

	private final GraphService graphService;
	private final PersonRepository personRepository;
	private final FollowRepository followRepository;
	private final RankRepository rankRepository;
	private final PageRankService pageRankService;
	private final Duration collectionWindow;
	private final double qualityThreshold;
	private final double spamPenalty;

	public IngestionService(
			GraphService graphService,
			PersonRepository personRepository,
			FollowRepository followRepository,
			RankRepository rankRepository,
			PageRankService pageRankService,
			PagerankSettingsProperties settings) {
		this.graphService = graphService;
		this.personRepository = personRepository;
		this.followRepository = followRepository;
		this.rankRepository = rankRepository;
		this.pageRankService = pageRankService;
		this.collectionWindow = settings.collectionWindow();
		this.qualityThreshold = settings.followQualityThreshold();
		this.spamPenalty = settings.spamPenalty();
	}

	/**
	 * Crea o refresca una persona observada y dispara PageRank incremental si cambia.
	 *
	 * @param observation datos observados (nombre, spamScore, timestamp).
	 * @return persona creada o actualizada.
	 */
	public Person collectPerson(PersonObservation observation) {
		String normalizedName = normalizeName(observation.name());
		Instant observedAt = observation.observedAt() != null ? observation.observedAt() : Instant.now();

		Person existing = personRepository.findByNameIgnoreCase(normalizedName).orElse(null);
		boolean changed = false;
		Person result;
		if (existing == null) {
			result = graphService.registerPerson(normalizedName, observation.spamScore(), observedAt);
			changed = true;
		}
		else if (!shouldRefresh(existing.getLastSeen(), observedAt)) {
			return existing;
		}
		else {
			existing.setSpamScore(observation.spamScore());
			existing.setLastSeen(observedAt);
			result = personRepository.save(existing);
			changed = true;
		}

		if (changed) {
			triggerIncrementalUpdate(result.getId());
		}
		return result;
	}

	/**
	 * Crea o refresca un follow observado si supera el umbral de calidad y dispara incremental.
	 *
	 * @param observation follow observado (origen, destino, calidad, timestamp).
	 * @return follow persistido, si fue aceptado.
	 */
	public Optional<Follow> collectFollow(FollowObservation observation) {
		Assert.notNull(observation, "Observation cannot be null");
		Long sourceId = observation.sourcePersonId();
		Long targetId = observation.targetPersonId();
		if (sourceId == null || targetId == null || sourceId.equals(targetId)) {
			return Optional.empty();
		}
		Instant observedAt = observation.observedAt() != null ? observation.observedAt() : Instant.now();
		double rawQuality = observation.quality();

		if (rawQuality < qualityThreshold) {
			log.debug("Ignoring follow {} -> {} due to low quality {}", sourceId, targetId, rawQuality);
			return Optional.empty();
		}

		Person source = personRepository.findById(sourceId).orElse(null);
		Person target = personRepository.findById(targetId).orElse(null);
		if (source == null || target == null) {
			return Optional.empty();
		}

		Optional<Follow> existing = followRepository.findBySourceIdAndTargetId(sourceId, targetId);
		if (existing.isPresent() && !shouldRefresh(existing.get().getLastSeen(), observedAt)) {
			return existing;
		}

		double adjustedQuality = adjustQuality(rawQuality, source.getSpamScore());
		Follow saved = graphService.registerFollow(sourceId, targetId, adjustedQuality, observedAt);
		triggerIncrementalUpdate(sourceId, targetId);
		return Optional.of(saved);
	}

	/**
	 * Variante de collectFollow que busca por nombres, valida y delega.
	 *
	 * @param sourceName nombre del origen.
	 * @param targetName nombre del destino.
	 * @param quality calidad observada.
	 * @param observedAt instante observado; si es nulo se usa ahora.
	 * @return follow persistido, si fue aceptado.
	 */
	public Optional<Follow> collectFollowByNames(String sourceName, String targetName, double quality, Instant observedAt) {
		if (!StringUtils.hasText(sourceName) || !StringUtils.hasText(targetName)) {
			return Optional.empty();
		}
		return personRepository.findByNameIgnoreCase(sourceName.trim())
				.flatMap(source -> personRepository.findByNameIgnoreCase(targetName.trim())
						.flatMap(target -> collectFollow(new FollowObservation(source.getId(), target.getId(), quality, observedAt))));
	}

	private boolean shouldRefresh(Instant previous, Instant now) {
		if (previous == null) {
			return true;
		}
		Duration delta = Duration.between(previous, now);
		return delta.compareTo(collectionWindow) >= 0;
	}

	private String normalizeName(String raw) {
		Assert.hasText(raw, "Name cannot be blank");
		return raw.trim();
	}

	private double adjustQuality(double quality, double spamScore) {
		double clampedSpam = Math.max(0.0, Math.min(1.0, spamScore));
		double penaltyFactor = 1.0 - (spamPenalty * clampedSpam);
		if (penaltyFactor < 0) {
			penaltyFactor = 0;
		}
		return quality * penaltyFactor;
	}

	private void triggerIncrementalUpdate(Long... ids) {
		if (rankRepository.count() == 0) {
			return;
		}
		Set<Long> touched = Arrays.stream(ids)
				.filter(id -> id != null && id > 0)
				.collect(Collectors.toSet());
		if (touched.isEmpty()) {
			return;
		}
		pageRankService.runIncrementalUpdate(touched);
	}

	public record PersonObservation(String name, double spamScore, Instant observedAt) {
	}

	public record FollowObservation(Long sourcePersonId, Long targetPersonId, double quality, Instant observedAt) {
	}
}
