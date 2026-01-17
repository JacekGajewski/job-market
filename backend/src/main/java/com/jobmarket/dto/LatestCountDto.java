package com.jobmarket.dto;

import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record LatestCountDto(
    String category,
    String metricType,
    Integer count,
    LocalDateTime fetchedAt,
    Integer changeFromPrevious,
    Double percentageChange
) {}
