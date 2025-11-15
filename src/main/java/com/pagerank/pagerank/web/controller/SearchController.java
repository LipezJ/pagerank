package com.pagerank.pagerank.web.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.pagerank.pagerank.web.dto.SearchResult;
import com.pagerank.pagerank.services.SearchService;
import com.pagerank.pagerank.settings.PagerankSettingsProperties;

/**
 * MVC controller that delegates to the service layer to retrieve ranked persons.
 */
@Controller
public class SearchController {

	private final SearchService searchService;
	private final PagerankSettingsProperties settings;

	public SearchController(SearchService searchService, PagerankSettingsProperties settings) {
		this.searchService = searchService;
		this.settings = settings;
	}

	@GetMapping({ "/", "/search" })
	public String search(
			@RequestParam(value = "q", required = false) String query,
			@RequestParam(value = "k", required = false) Integer limitOverride,
			Model model) {

		int effectiveLimit = limitOverride != null ? limitOverride : settings.kTop();
		String sanitizedQuery = query == null ? "" : query.trim();

		List<SearchResult> results = searchService.search(sanitizedQuery, effectiveLimit);

		model.addAttribute("query", sanitizedQuery);
		model.addAttribute("results", results);
		model.addAttribute("limit", effectiveLimit);
		model.addAttribute("settings", settings);

		return "search/index";
	}
}
