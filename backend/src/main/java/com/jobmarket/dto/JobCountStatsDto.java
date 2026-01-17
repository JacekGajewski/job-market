package com.jobmarket.dto;

import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record JobCountStatsDto(
    Long id,
    String category,
    Integer count,
    LocalDateTime fetchedAt,
    String location,
    String metricType
) {}
