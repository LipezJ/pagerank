package com.pagerank.pagerank.domain.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "follows", uniqueConstraints = {
		@UniqueConstraint(name = "uk_follow_src_dst", columnNames = { "src_id", "dst_id" })
}, indexes = {
		@Index(name = "idx_follow_src", columnList = "src_id"),
		@Index(name = "idx_follow_dst", columnList = "dst_id")
})
public class Follow {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "src_id", nullable = false)
	private Person source;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "dst_id", nullable = false)
	private Person target;

	@Column(nullable = false)
	private double quality;

	@Column(name = "last_seen", nullable = false)
	private Instant lastSeen;

	protected Follow() {
	}

	public Follow(Person source, Person target, double quality, Instant lastSeen) {
		this.source = source;
		this.target = target;
		this.quality = quality;
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

	public Person getSource() {
		return source;
	}

	public void setSource(Person source) {
		this.source = source;
	}

	public Person getTarget() {
		return target;
	}

	public void setTarget(Person target) {
		this.target = target;
	}

	public double getQuality() {
		return quality;
	}

	public void setQuality(double quality) {
		this.quality = quality;
	}

	public Instant getLastSeen() {
		return lastSeen;
	}

	public void setLastSeen(Instant lastSeen) {
		this.lastSeen = lastSeen;
	}
}
