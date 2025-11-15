package com.pagerank.pagerank.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "rank_deltas")
public class RankDelta {

	@Id
	@Column(name = "person_id")
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId
	@JoinColumn(name = "person_id")
	private Person person;

	@Column(nullable = false)
	private double delta;

	protected RankDelta() {
	}

	public RankDelta(Person person, double delta) {
		this.person = person;
		this.delta = delta;
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

	public double getDelta() {
		return delta;
	}

	public void setDelta(double delta) {
		this.delta = delta;
	}
}
