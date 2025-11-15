package com.pagerank.pagerank.domain.model;

import java.time.Duration;

public record PageRankResult(
		int iterations,
		double averageDelta,
		int nodeCount,
		boolean converged,
		Duration elapsed) {
}
