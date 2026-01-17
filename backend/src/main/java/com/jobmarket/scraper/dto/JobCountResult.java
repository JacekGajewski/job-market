package com.jobmarket.scraper.dto;

import com.jobmarket.entity.ExperienceLevel;
import com.jobmarket.entity.MetricType;
import com.jobmarket.entity.SalaryRange;
import com.jobmarket.entity.TrackedCategory;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class JobCountResult {

    private TrackedCategory category;
    private MetricType metricType;
    private String city;
    private ExperienceLevel experienceLevel;
    private Integer salaryMin;
    private Integer salaryMax;
    private Integer count;
    private LocalDateTime fetchedAt;
    private boolean success;
    private String errorMessage;
    private String source;

    public static JobCountResult success(TrackedCategory category, MetricType metricType,
                                          int count, LocalDateTime fetchedAt, String source) {
        return JobCountResult.builder()
                .category(category)
                .metricType(metricType)
                .count(count)
                .fetchedAt(fetchedAt)
                .success(true)
                .source(source)
                .build();
    }

    public static JobCountResult success(TrackedCategory category, MetricType metricType,
                                          String city, ExperienceLevel experienceLevel,
                                          SalaryRange salaryRange,
                                          int count, LocalDateTime fetchedAt, String source) {
        JobCountResultBuilder builder = JobCountResult.builder()
                .category(category)
                .metricType(metricType)
                .city(city)
                .experienceLevel(experienceLevel)
                .count(count)
                .fetchedAt(fetchedAt)
                .success(true)
                .source(source);

        if (salaryRange != null) {
            builder.salaryMin(salaryRange.getMin());
            builder.salaryMax(salaryRange.getMax());
        }

        return builder.build();
    }

    public static JobCountResult failed(TrackedCategory category, MetricType metricType, String errorMessage) {
        return JobCountResult.builder()
                .category(category)
                .metricType(metricType)
                .fetchedAt(LocalDateTime.now())
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }

    public static JobCountResult failed(TrackedCategory category, MetricType metricType,
                                         String city, ExperienceLevel experienceLevel,
                                         SalaryRange salaryRange, String errorMessage) {
        JobCountResultBuilder builder = JobCountResult.builder()
                .category(category)
                .metricType(metricType)
                .city(city)
                .experienceLevel(experienceLevel)
                .fetchedAt(LocalDateTime.now())
                .success(false)
                .errorMessage(errorMessage);

        if (salaryRange != null) {
            builder.salaryMin(salaryRange.getMin());
            builder.salaryMax(salaryRange.getMax());
        }

        return builder.build();
    }
}
