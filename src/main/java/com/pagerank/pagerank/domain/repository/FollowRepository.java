package com.pagerank.pagerank.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pagerank.pagerank.domain.model.Follow;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {

	List<Follow> findBySourceId(Long sourceId);

	List<Follow> findByTargetId(Long targetId);

	Optional<Follow> findBySourceIdAndTargetId(Long sourceId, Long targetId);
}
