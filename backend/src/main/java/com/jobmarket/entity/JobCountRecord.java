package com.jobmarket.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_count_record", indexes = {
    @Index(name = "idx_job_count_category", columnList = "category"),
    @Index(name = "idx_job_count_fetched_at", columnList = "fetched_at"),
    @Index(name = "idx_job_count_category_location", columnList = "category, location"),
    @Index(name = "idx_job_count_category_metric", columnList = "category, metric_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobCountRecord extends BaseEntity {

    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @Column(name = "count", nullable = false)
    private Integer count;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    @Column(name = "location", nullable = false, length = 100)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false, length = 50)
    @Builder.Default
    private MetricType metricType = MetricType.TOTAL;

    @Column(name = "city", length = 100)
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_level", length = 20)
    private ExperienceLevel experienceLevel;

    @Column(name = "salary_min")
    private Integer salaryMin;

    @Column(name = "salary_max")
    private Integer salaryMax;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;
}
