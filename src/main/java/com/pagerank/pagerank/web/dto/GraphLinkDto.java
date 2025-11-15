package com.pagerank.pagerank.web.dto;

public record GraphLinkDto(Long sourceId, Long targetId, double weight) {
}
