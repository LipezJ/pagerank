package com.pagerank.pagerank.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.pagerank.pagerank.domain.model.PageRankResult;
import com.pagerank.pagerank.domain.repository.PersonRepository;
import com.pagerank.pagerank.domain.repository.RankRepository;

@Component
@Order(1)
public class PageRankBootstrapper implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(PageRankBootstrapper.class);

	private final PageRankService pageRankService;
	private final PersonRepository personRepository;
	private final RankRepository rankRepository;

	public PageRankBootstrapper(
			PageRankService pageRankService,
			PersonRepository personRepository,
			RankRepository rankRepository) {
		this.pageRankService = pageRankService;
		this.personRepository = personRepository;
		this.rankRepository = rankRepository;
	}

	@Override
	public void run(ApplicationArguments args) {
		long personCount = personRepository.count();
		if (personCount == 0) {
			log.info("Skipping PageRank batch: no persons stored yet");
			return;
		}
		if (rankRepository.count() > 0) {
			log.info("Skipping PageRank batch: existing ranks detected");
			return;
		}

		PageRankResult result = pageRankService.runBatchComputation();
		log.info("Initial PageRank finished (nodes={}, iterations={}, converged={}, elapsed={} ms)",
				result.nodeCount(),
				result.iterations(),
				result.converged(),
				result.elapsed().toMillis());
	}
}
