package com.pagerank.pagerank.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pagerank.pagerank.domain.model.Person;
import com.pagerank.pagerank.domain.model.Rank;

@Repository
public interface RankRepository extends JpaRepository<Rank, Long> {

	@EntityGraph(attributePaths = "person")
	Page<Rank> findAllByOrderByScoreDesc(Pageable pageable);

	@EntityGraph(attributePaths = "person")
	Page<Rank> findByPersonNameContainingIgnoreCaseOrderByScoreDesc(String name, Pageable pageable);

	Optional<Rank> findByPerson(Person person);
}
