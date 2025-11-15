package com.pagerank.pagerank.web.controller;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pagerank.pagerank.domain.model.Follow;
import com.pagerank.pagerank.domain.model.Person;
import com.pagerank.pagerank.web.dto.FollowRequest;
import com.pagerank.pagerank.web.dto.FollowResponse;
import com.pagerank.pagerank.web.dto.PersonRequest;
import com.pagerank.pagerank.web.dto.PersonResponse;
import com.pagerank.pagerank.services.IngestionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name = "Graph")
public class GraphRestController {

	private final IngestionService ingestionService;

	public GraphRestController(IngestionService ingestionService) {
		this.ingestionService = ingestionService;
	}

	@PostMapping("/persons")
	@Operation(summary = "Crea o actualiza una persona")
	public ResponseEntity<PersonResponse> registerPerson(@RequestBody PersonRequest request) {
		if (request == null || request.name() == null || request.name().isBlank()) {
			return ResponseEntity.badRequest().build();
		}
		double spamScore = Math.max(0.0, Math.min(1.0, request.spamScore()));
		Person person = ingestionService.collectPerson(
				new IngestionService.PersonObservation(request.name().trim(), spamScore, Instant.now()));
		return ResponseEntity.ok(PersonResponse.from(person));
	}

	@PostMapping("/follows")
	@Operation(summary = "Crea o actualiza un follow dirigido")
	public ResponseEntity<FollowResponse> registerFollow(@RequestBody FollowRequest request) {
		if (request == null || request.sourceId() == null || request.targetId() == null) {
			return ResponseEntity.badRequest().build();
		}
		return ingestionService.collectFollow(
				new IngestionService.FollowObservation(
						request.sourceId(),
						request.targetId(),
						request.quality(),
						Instant.now()))
				.map(follow -> ResponseEntity.ok(FollowResponse.from(follow)))
				.orElseGet(() -> ResponseEntity.status(HttpStatus.ACCEPTED).build());
	}
}
