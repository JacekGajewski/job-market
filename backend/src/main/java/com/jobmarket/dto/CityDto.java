package com.jobmarket.dto;

import lombok.Builder;

@Builder
public record CityDto(
    Long id,
    String name,
    String slug,
    boolean active
) {}
