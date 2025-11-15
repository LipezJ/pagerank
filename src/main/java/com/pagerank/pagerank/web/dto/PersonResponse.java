package com.pagerank.pagerank.web.dto;

import java.time.Instant;

import com.pagerank.pagerank.domain.model.Person;

public record PersonResponse(Long id, String name, double spamScore, Instant lastSeen) {
	public static PersonResponse from(Person person) {
		return new PersonResponse(person.getId(), person.getName(), person.getSpamScore(), person.getLastSeen());
	}
}
