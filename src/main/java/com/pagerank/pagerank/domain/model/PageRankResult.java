package com.pagerank.pagerank.domain.model;

import java.time.Duration;

public record PageRankResult(
		String mode,
		int iterations,
		double averageDelta,
		int nodeCount,
		boolean converged,
		boolean timeLimited,
		Duration elapsed) {
}
