package com.pagerank.pagerank.web.dto;

import java.time.Instant;
import java.util.List;

/**
 * DTO para respuestas de búsqueda, incluyendo aportantes destacados.
 */
public record SearchResult(
		Long id,
		String name,
		double score,
		String explanation,
		List<Contributor> contributors,
		Instant updatedAt) {

	public record Contributor(String name, double contribution) {
	}
}
