package com.pagerank.pagerank.web.dto;

import java.util.List;

public record IncrementalRequest(List<Long> personIds) {
}
