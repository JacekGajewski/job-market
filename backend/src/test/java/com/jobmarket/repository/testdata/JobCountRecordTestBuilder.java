package com.jobmarket.repository.testdata;

import com.jobmarket.entity.ExperienceLevel;
import com.jobmarket.entity.JobCountRecord;
import com.jobmarket.entity.MetricType;
import com.jobmarket.entity.SalaryRange;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fluent test builder for creating JobCountRecord instances in tests.
 * Provides sensible defaults while allowing full customization.
 */
public class JobCountRecordTestBuilder {

    private String category = "java";
    private Integer count = 100;
    // Default to a fixed time for consistent test data
    private LocalDateTime fetchedAt = LocalDateTime.of(2024, 6, 15, 12, 0, 0);
    private String location = "all-locations";
    private MetricType metricType = MetricType.TOTAL;
    private String city = null;
    private ExperienceLevel experienceLevel = null;
    private Integer salaryMin = null;
    private Integer salaryMax = null;

    private JobCountRecordTestBuilder() {
    }

    public static JobCountRecordTestBuilder aRecord() {
        return new JobCountRecordTestBuilder();
    }

    public JobCountRecordTestBuilder withCategory(String category) {
        this.category = category;
        return this;
    }

    public JobCountRecordTestBuilder withCount(int count) {
        this.count = count;
        return this;
    }

    public JobCountRecordTestBuilder fetchedAt(LocalDateTime fetchedAt) {
        this.fetchedAt = fetchedAt;
        return this;
    }

    public JobCountRecordTestBuilder fetchedDaysAgo(int days) {
        this.fetchedAt = LocalDateTime.now().minusDays(days);
        return this;
    }

    public JobCountRecordTestBuilder withLocation(String location) {
        this.location = location;
        return this;
    }

    public JobCountRecordTestBuilder withMetricType(MetricType metricType) {
        this.metricType = metricType;
        return this;
    }

    public JobCountRecordTestBuilder withCity(String city) {
        this.city = city;
        return this;
    }

    public JobCountRecordTestBuilder withExperienceLevel(ExperienceLevel experienceLevel) {
        this.experienceLevel = experienceLevel;
        return this;
    }

    public JobCountRecordTestBuilder withSalaryRange(SalaryRange salaryRange) {
        if (salaryRange != null) {
            this.salaryMin = salaryRange.getMin();
            this.salaryMax = salaryRange.getMax();
        } else {
            this.salaryMin = null;
            this.salaryMax = null;
        }
        return this;
    }

    public JobCountRecordTestBuilder withSalaryMin(Integer salaryMin) {
        this.salaryMin = salaryMin;
        return this;
    }

    public JobCountRecordTestBuilder withSalaryMax(Integer salaryMax) {
        this.salaryMax = salaryMax;
        return this;
    }

    /**
     * Convenience method: sets location to city slug (for city-specific records)
     */
    public JobCountRecordTestBuilder forCity(String citySlug) {
        this.location = citySlug;
        this.city = citySlug;
        return this;
    }

    /**
     * Convenience method: sets up record for all locations
     */
    public JobCountRecordTestBuilder forAllLocations() {
        this.location = "all-locations";
        this.city = null;
        return this;
    }

    /**
     * Convenience method: sets up record for remote work
     */
    public JobCountRecordTestBuilder forRemote() {
        this.location = "remote";
        this.metricType = MetricType.REMOTE;
        return this;
    }

    public JobCountRecord build() {
        return JobCountRecord.builder()
                .category(category)
                .count(count)
                .fetchedAt(fetchedAt)
                .location(location)
                .metricType(metricType)
                .city(city)
                .experienceLevel(experienceLevel)
                .salaryMin(salaryMin)
                .salaryMax(salaryMax)
                .recordDate(fetchedAt.toLocalDate())
                .build();
    }
}
