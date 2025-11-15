package com.pagerank.pagerank.settings;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Strongly typed access to PageRank tuning parameters defined in settings/.env.
 */
@ConfigurationProperties(prefix = "pagerank.settings")
public record PagerankSettingsProperties(
		double damping,
		double epsilon,
		int maxIters,
		int kTop,
		Duration collectionWindow,
		Duration maxUpdateDuration,
		double followQualityThreshold,
		double spamPenalty,
		String datasetPersons,
		String datasetFollows) {

	public PagerankSettingsProperties {
		Assert.isTrue(damping > 0 && damping < 1, "Damping factor must be between 0 and 1");
		Assert.isTrue(epsilon > 0, "Epsilon must be positive");
		Assert.isTrue(maxIters > 0, "Max iterations must be positive");
		Assert.isTrue(kTop > 0, "K Top must be positive");
		Assert.notNull(collectionWindow, "Collection window duration is required");
		Assert.notNull(maxUpdateDuration, "Max update duration is required");
		Assert.isTrue(followQualityThreshold >= 0, "Follow quality threshold must be non-negative");
		Assert.isTrue(spamPenalty >= 0 && spamPenalty <= 1, "Spam penalty must be between 0 and 1");
		Assert.isTrue(StringUtils.hasText(datasetPersons), "Dataset persons path required");
		Assert.isTrue(StringUtils.hasText(datasetFollows), "Dataset follows path required");
	}
}
