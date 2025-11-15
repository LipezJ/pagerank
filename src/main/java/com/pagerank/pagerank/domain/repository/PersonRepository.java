package com.pagerank.pagerank.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pagerank.pagerank.domain.model.Person;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {

	Page<Person> findByNameContainingIgnoreCase(String name, Pageable pageable);

	Optional<Person> findByNameIgnoreCase(String name);
}
