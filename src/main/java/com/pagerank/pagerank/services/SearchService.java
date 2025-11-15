package com.pagerank.pagerank.services;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.pagerank.pagerank.domain.model.Person;
import com.pagerank.pagerank.domain.model.Rank;
import com.pagerank.pagerank.domain.repository.PersonRepository;
import com.pagerank.pagerank.domain.repository.RankRepository;
import com.pagerank.pagerank.web.dto.SearchResult;

@Service
@Transactional(readOnly = true)
public class SearchService {

	private final PersonRepository personRepository;
	private final RankRepository rankRepository;
	private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm")
			.withLocale(Locale.getDefault());
	private final ZoneId zoneId = ZoneId.systemDefault();

	public SearchService(PersonRepository personRepository, RankRepository rankRepository) {
		this.personRepository = personRepository;
		this.rankRepository = rankRepository;
	}

	public List<SearchResult> search(String query, int limit) {
		int effectiveLimit = Math.max(limit, 1);
		Pageable pageable = PageRequest.of(0, effectiveLimit);

		if (!StringUtils.hasText(query)) {
			Page<Rank> ranks = rankRepository.findAllByOrderByScoreDesc(pageable);
			return ranks.getContent().stream()
					.map(this::toResultFromRank)
					.collect(Collectors.toList());
		}

		Page<Rank> rankedMatches = rankRepository.findByPersonNameContainingIgnoreCaseOrderByScoreDesc(query, pageable);
		List<SearchResult> ordered = rankedMatches.getContent().stream()
				.map(this::toResultFromRank)
				.collect(Collectors.toList());

		if (ordered.size() < effectiveLimit) {
			// relleno con personas sin rank aún, manteniendo el límite
			Page<Person> persons = personRepository.findByNameContainingIgnoreCase(query,
					PageRequest.of(0, effectiveLimit - ordered.size()));
			var rankedIds = rankedMatches.getContent().stream()
					.map(r -> r.getPerson().getId())
					.collect(Collectors.toSet());
			persons.stream()
					.filter(p -> !rankedIds.contains(p.getId()))
					.map(this::toResultFromPerson)
					.forEach(ordered::add);
		}
		return ordered;
	}

	private SearchResult toResultFromRank(Rank rank) {
		Person person = rank.getPerson();
		String explanation = "Score " + formatScore(rank.getScore());
		if (rank.getUpdatedAt() != null) {
			explanation += " - actualizado " + formatter.format(rank.getUpdatedAt().atZone(zoneId));
		}
		return new SearchResult(person.getName(), rank.getScore(), explanation);
	}

	private SearchResult toResultFromPerson(Person person) {
		double score = rankRepository.findById(person.getId())
				.map(Rank::getScore)
				.orElse(0.0);
		String explanation = score > 0
				? "Score " + formatScore(score)
				: "Aun sin calculo de PageRank";
		return new SearchResult(person.getName(), score, explanation);
	}

	private String formatScore(double score) {
		return String.format(Locale.US, "%.5f", score);
	}
}
