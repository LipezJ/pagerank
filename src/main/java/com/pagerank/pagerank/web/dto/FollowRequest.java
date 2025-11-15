package com.pagerank.pagerank.web.dto;

public record FollowRequest(Long sourceId, Long targetId, double quality) {
}
