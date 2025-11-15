package com.pagerank.pagerank.web.controller;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pagerank.pagerank.domain.model.PageRankResult;
import com.pagerank.pagerank.web.dto.IncrementalRequest;
import com.pagerank.pagerank.services.PageRankService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/pagerank")
@Tag(name = "PageRank")
public class RankingRestController {

	private final PageRankService pageRankService;

	public RankingRestController(PageRankService pageRankService) {
		this.pageRankService = pageRankService;
	}

	@PostMapping("/batch")
	@Operation(summary = "Ejecuta PageRank batch")
	public PageRankResult runBatch() {
		return pageRankService.runBatchComputation();
	}

	@PostMapping("/incremental")
	@Operation(summary = "Ejecuta una actualización incremental")
	public PageRankResult runIncremental(@RequestBody(required = false) IncrementalRequest request) {
		Set<Long> touched = request != null && request.personIds() != null
				? new HashSet<>(request.personIds())
				: Collections.emptySet();
		return pageRankService.runIncrementalUpdate(touched);
	}
}
