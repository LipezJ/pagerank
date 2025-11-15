package com.pagerank.pagerank.web.dto;

import java.time.Instant;

import com.pagerank.pagerank.domain.model.Follow;

public record FollowResponse(
		Long id,
		Long sourceId,
		Long targetId,
		double quality,
		Instant lastSeen) {

	public static FollowResponse from(Follow follow) {
		return new FollowResponse(
				follow.getId(),
				follow.getSource().getId(),
				follow.getTarget().getId(),
				follow.getQuality(),
				follow.getLastSeen());
	}
}
