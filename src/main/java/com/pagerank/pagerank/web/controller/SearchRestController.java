package com.pagerank.pagerank.web.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pagerank.pagerank.web.dto.SearchResult;
import com.pagerank.pagerank.services.SearchService;
import com.pagerank.pagerank.settings.PagerankSettingsProperties;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/search")
@Tag(name = "Search")
public class SearchRestController {

	private final SearchService searchService;
	private final PagerankSettingsProperties settings;

	public SearchRestController(SearchService searchService, PagerankSettingsProperties settings) {
		this.searchService = searchService;
		this.settings = settings;
	}

	@GetMapping
	@Operation(summary = "Busca personas ordenadas por PageRank")
	public List<SearchResult> search(
			@RequestParam(value = "q", required = false) String query,
			@RequestParam(value = "k", required = false) Integer limitOverride) {
		int effectiveLimit = limitOverride != null ? limitOverride : settings.kTop();
		String sanitizedQuery = query == null ? "" : query.trim();
		return searchService.search(sanitizedQuery, effectiveLimit);
	}
}
