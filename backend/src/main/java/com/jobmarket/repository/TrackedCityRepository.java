package com.jobmarket.repository;

import com.jobmarket.entity.TrackedCity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrackedCityRepository extends JpaRepository<TrackedCity, Long> {

    List<TrackedCity> findByActiveTrue();

    Optional<TrackedCity> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
