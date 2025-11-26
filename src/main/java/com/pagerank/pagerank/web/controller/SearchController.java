package com.pagerank.pagerank.web.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.pagerank.pagerank.web.dto.SearchResult;
import com.pagerank.pagerank.services.PageRankService;
import com.pagerank.pagerank.services.SearchService;
import com.pagerank.pagerank.settings.PagerankSettingsProperties;

/**
 * MVC controller that delegates to the service layer to retrieve ranked persons.
 */
@Controller
public class SearchController {

	private final SearchService searchService;
	private final PagerankSettingsProperties settings;
	private final PageRankService pageRankService;

	public SearchController(SearchService searchService, PagerankSettingsProperties settings, PageRankService pageRankService) {
		this.searchService = searchService;
		this.settings = settings;
		this.pageRankService = pageRankService;
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
		model.addAttribute("metrics", pageRankService.getLastResult());

		return "search/index";
	}

	@PostMapping("/search/run-pagerank")
	public String runPageRank(
			@RequestParam(value = "q", required = false) String query,
			@RequestParam(value = "k", required = false) Integer limitOverride,
			RedirectAttributes redirectAttributes) {

		int effectiveLimit = limitOverride != null ? limitOverride : settings.kTop();
		String sanitizedQuery = query == null ? "" : query.trim();
		pageRankService.runBatchComputation();
		redirectAttributes.addFlashAttribute("message", "PageRank ejecutado");

		StringBuilder redirect = new StringBuilder("redirect:/search");
		boolean hasParam = false;
		if (!sanitizedQuery.isEmpty()) {
			redirect.append("?q=").append(URLEncoder.encode(sanitizedQuery, StandardCharsets.UTF_8));
			hasParam = true;
		}
		if (limitOverride != null) {
			redirect.append(hasParam ? "&" : "?").append("k=").append(effectiveLimit);
		}
		return redirect.toString();
	}
}
