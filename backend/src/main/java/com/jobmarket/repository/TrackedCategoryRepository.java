package com.jobmarket.repository;

import com.jobmarket.entity.TrackedCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrackedCategoryRepository extends JpaRepository<TrackedCategory, Long> {

    List<TrackedCategory> findByActiveTrue();

    Optional<TrackedCategory> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
