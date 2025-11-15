package com.pagerank.pagerank.web.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pagerank.pagerank.domain.model.Follow;
import com.pagerank.pagerank.domain.model.Person;
import com.pagerank.pagerank.domain.model.Rank;
import com.pagerank.pagerank.domain.repository.FollowRepository;
import com.pagerank.pagerank.domain.repository.PersonRepository;
import com.pagerank.pagerank.domain.repository.RankRepository;
import com.pagerank.pagerank.web.dto.GraphLinkDto;
import com.pagerank.pagerank.web.dto.GraphNodeDto;
import com.pagerank.pagerank.web.dto.GraphResponse;

@RestController
@RequestMapping("/api/graph")
public class GraphDataController {

	private final PersonRepository personRepository;
	private final RankRepository rankRepository;
	private final FollowRepository followRepository;

	public GraphDataController(PersonRepository personRepository, RankRepository rankRepository,
			FollowRepository followRepository) {
		this.personRepository = personRepository;
		this.rankRepository = rankRepository;
		this.followRepository = followRepository;
	}

	@GetMapping
	public GraphResponse graph() {
		List<Person> persons = personRepository.findAll();
		List<Rank> ranks = rankRepository.findAll();
		Map<Long, Double> scoreMap = ranks.stream()
				.collect(Collectors.toMap(Rank::getId, Rank::getScore));

		double totalScore = scoreMap.values().stream().mapToDouble(Double::doubleValue).sum();
		double defaultScore = persons.isEmpty() ? 0.0 : 1.0 / persons.size();

		List<GraphNodeDto> nodes = new ArrayList<>(persons.size());
		for (Person person : persons) {
			double score = scoreMap.getOrDefault(person.getId(), defaultScore);
			double pct = totalScore > 0 ? (score / totalScore) * 100.0 : (defaultScore * 100.0);
			nodes.add(new GraphNodeDto(person.getId(), person.getName(), score, pct));
		}

		List<GraphLinkDto> links = new ArrayList<>();
		for (Follow follow : followRepository.findAll()) {
			links.add(new GraphLinkDto(
					follow.getSource().getId(),
					follow.getTarget().getId(),
					follow.getQuality()));
		}

		return new GraphResponse(nodes, links);
	}
}
