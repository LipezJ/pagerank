package com.pagerank.pagerank.domain.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "persons", indexes = {
		@Index(name = "idx_person_name", columnList = "name")
})
public class Person {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 160)
	private String name;

	@Column(name = "spam_score", nullable = false)
	private double spamScore;

	@Column(name = "last_seen", nullable = false)
	private Instant lastSeen;

	protected Person() {
	}

	public Person(String name, double spamScore, Instant lastSeen) {
		this.name = name;
		this.spamScore = spamScore;
		this.lastSeen = lastSeen;
	}

	@PrePersist
	void prePersist() {
		if (lastSeen == null) {
			lastSeen = Instant.now();
		}
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getSpamScore() {
		return spamScore;
	}

	public void setSpamScore(double spamScore) {
		this.spamScore = spamScore;
	}

	public Instant getLastSeen() {
		return lastSeen;
	}

	public void setLastSeen(Instant lastSeen) {
		this.lastSeen = lastSeen;
	}
}
