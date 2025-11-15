package com.pagerank.pagerank.domain.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "ranks", indexes = {
		@Index(name = "idx_rank_score", columnList = "score DESC")
})
public class Rank {

	@Id
	@Column(name = "person_id")
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId
	@JoinColumn(name = "person_id")
	private Person person;

	@Column(nullable = false)
	private double score;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Rank() {
	}

	public Rank(Person person, double score, Instant updatedAt) {
		this.person = person;
		this.score = score;
		this.updatedAt = updatedAt;
	}

	@PrePersist
	@PreUpdate
	void touch() {
		if (updatedAt == null) {
			updatedAt = Instant.now();
		}
	}

	public Long getId() {
		return id;
	}

	public Person getPerson() {
		return person;
	}

	public void setPerson(Person person) {
		this.person = person;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
