package com.pagerank.pagerank.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pagerank.pagerank.domain.model.Person;
import com.pagerank.pagerank.domain.model.RankDelta;

@Repository
public interface RankDeltaRepository extends JpaRepository<RankDelta, Long> {

	Optional<RankDelta> findByPerson(Person person);
}
