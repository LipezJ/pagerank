package com.pagerank.pagerank.web.dto;

/**
 * Minimal data transfer object for search responses.
 */
public record SearchResult(String name, double score, String explanation) {
}
