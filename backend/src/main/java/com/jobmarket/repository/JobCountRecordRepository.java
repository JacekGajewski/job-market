package com.jobmarket.repository;

import com.jobmarket.entity.ExperienceLevel;
import com.jobmarket.entity.JobCountRecord;
import com.jobmarket.entity.MetricType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobCountRecordRepository extends JpaRepository<JobCountRecord, Long> {

    List<JobCountRecord> findByCategoryOrderByFetchedAtDesc(String category);

    List<JobCountRecord> findByCategoryAndLocationOrderByFetchedAtDesc(String category, String location);

    Optional<JobCountRecord> findFirstByCategoryOrderByFetchedAtDesc(String category);

    Optional<JobCountRecord> findTopByCategoryOrderByFetchedAtDesc(String category);

    Optional<JobCountRecord> findTopByCategoryAndMetricTypeOrderByFetchedAtDesc(String category, MetricType metricType);

    @Query("SELECT j FROM JobCountRecord j WHERE j.category = :category " +
           "AND j.fetchedAt >= :startDate ORDER BY j.fetchedAt ASC")
    List<JobCountRecord> findByCategoryAndFetchedAtAfter(
        @Param("category") String category,
        @Param("startDate") LocalDateTime startDate);

    @Query("SELECT j FROM JobCountRecord j WHERE j.category = :category " +
           "AND j.fetchedAt BETWEEN :startDate AND :endDate ORDER BY j.fetchedAt ASC")
    List<JobCountRecord> findByCategoryAndFetchedAtBetweenOrderByFetchedAtAsc(
        @Param("category") String category,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);

    @Query("SELECT j FROM JobCountRecord j WHERE j.category = :category " +
           "AND j.metricType = :metricType " +
           "AND j.fetchedAt BETWEEN :startDate AND :endDate ORDER BY j.fetchedAt ASC")
    List<JobCountRecord> findByCategoryAndMetricTypeAndFetchedAtBetweenOrderByFetchedAtAsc(
        @Param("category") String category,
        @Param("metricType") MetricType metricType,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);

    @Query("SELECT j FROM JobCountRecord j WHERE j.category = :category " +
           "AND j.fetchedAt < :currentFetchedAt ORDER BY j.fetchedAt DESC LIMIT 1")
    Optional<JobCountRecord> findPreviousRecord(
        @Param("category") String category,
        @Param("currentFetchedAt") LocalDateTime currentFetchedAt);

    @Query("SELECT j FROM JobCountRecord j WHERE j.category = :category " +
           "AND j.metricType = :metricType " +
           "AND j.fetchedAt < :currentFetchedAt ORDER BY j.fetchedAt DESC LIMIT 1")
    Optional<JobCountRecord> findPreviousRecordByMetricType(
        @Param("category") String category,
        @Param("metricType") MetricType metricType,
        @Param("currentFetchedAt") LocalDateTime currentFetchedAt);

    @Query("SELECT DISTINCT j.category FROM JobCountRecord j")
    List<String> findDistinctCategories();

    @Query("SELECT j FROM JobCountRecord j WHERE j.category = :category " +
           "AND j.metricType = :metricType " +
           "AND j.location = :location " +
           "AND ((:experienceLevel IS NULL AND j.experienceLevel IS NULL) OR j.experienceLevel = :experienceLevel) " +
           "AND ((:salaryMin IS NULL AND j.salaryMin IS NULL) OR j.salaryMin = :salaryMin) " +
           "AND ((:salaryMax IS NULL AND j.salaryMax IS NULL) OR j.salaryMax = :salaryMax) " +
           "AND j.fetchedAt BETWEEN :startDate AND :endDate ORDER BY j.fetchedAt ASC")
    List<JobCountRecord> findByFilters(
        @Param("category") String category,
        @Param("metricType") MetricType metricType,
        @Param("location") String location,
        @Param("experienceLevel") ExperienceLevel experienceLevel,
        @Param("salaryMin") Integer salaryMin,
        @Param("salaryMax") Integer salaryMax,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);

    @Query("SELECT j FROM JobCountRecord j WHERE j.category = :category " +
           "AND j.metricType = :metricType " +
           "AND j.location = :location " +
           "AND ((:experienceLevel IS NULL AND j.experienceLevel IS NULL) OR j.experienceLevel = :experienceLevel) " +
           "AND ((:salaryMin IS NULL AND j.salaryMin IS NULL) OR j.salaryMin = :salaryMin) " +
           "AND ((:salaryMax IS NULL AND j.salaryMax IS NULL) OR j.salaryMax = :salaryMax) " +
           "ORDER BY j.fetchedAt DESC LIMIT 1")
    Optional<JobCountRecord> findLatestByFilters(
        @Param("category") String category,
        @Param("metricType") MetricType metricType,
        @Param("location") String location,
        @Param("experienceLevel") ExperienceLevel experienceLevel,
        @Param("salaryMin") Integer salaryMin,
        @Param("salaryMax") Integer salaryMax);

    @Query("SELECT j FROM JobCountRecord j WHERE j.category = :category " +
           "AND j.metricType = :metricType " +
           "AND j.location = :location " +
           "AND ((:experienceLevel IS NULL AND j.experienceLevel IS NULL) OR j.experienceLevel = :experienceLevel) " +
           "AND ((:salaryMin IS NULL AND j.salaryMin IS NULL) OR j.salaryMin = :salaryMin) " +
           "AND ((:salaryMax IS NULL AND j.salaryMax IS NULL) OR j.salaryMax = :salaryMax) " +
           "AND j.fetchedAt < :currentFetchedAt ORDER BY j.fetchedAt DESC LIMIT 1")
    Optional<JobCountRecord> findPreviousByFilters(
        @Param("category") String category,
        @Param("metricType") MetricType metricType,
        @Param("location") String location,
        @Param("experienceLevel") ExperienceLevel experienceLevel,
        @Param("salaryMin") Integer salaryMin,
        @Param("salaryMax") Integer salaryMax,
        @Param("currentFetchedAt") LocalDateTime currentFetchedAt);

    @Query("SELECT j FROM JobCountRecord j WHERE j.category = :category " +
           "AND j.metricType = :metricType " +
           "AND ((:city IS NULL AND j.city IS NULL) OR j.city = :city) " +
           "AND ((:experienceLevel IS NULL AND j.experienceLevel IS NULL) OR j.experienceLevel = :experienceLevel) " +
           "AND ((:salaryMin IS NULL AND j.salaryMin IS NULL) OR j.salaryMin = :salaryMin) " +
           "AND ((:salaryMax IS NULL AND j.salaryMax IS NULL) OR j.salaryMax = :salaryMax) " +
           "AND j.recordDate = :recordDate")
    Optional<JobCountRecord> findExistingRecord(
        @Param("category") String category,
        @Param("metricType") MetricType metricType,
        @Param("city") String city,
        @Param("experienceLevel") ExperienceLevel experienceLevel,
        @Param("salaryMin") Integer salaryMin,
        @Param("salaryMax") Integer salaryMax,
        @Param("recordDate") LocalDate recordDate);
}
