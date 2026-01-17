package com.jobmarket.dto;

import lombok.Builder;

@Builder
public record CategoryDto(
    Long id,
    String name,
    String slug,
    boolean active
) {}
