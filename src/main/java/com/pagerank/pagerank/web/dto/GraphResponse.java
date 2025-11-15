package com.pagerank.pagerank.web.dto;

import java.util.List;

public record GraphResponse(List<GraphNodeDto> nodes, List<GraphLinkDto> links) {
}
