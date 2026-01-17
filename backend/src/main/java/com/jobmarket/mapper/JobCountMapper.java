package com.jobmarket.mapper;

import com.jobmarket.dto.JobCountStatsDto;
import com.jobmarket.entity.JobCountRecord;
import org.springframework.stereotype.Component;

@Component
public class JobCountMapper {

    public JobCountStatsDto toDto(JobCountRecord entity) {
        return JobCountStatsDto.builder()
                .id(entity.getId())
                .category(entity.getCategory())
                .count(entity.getCount())
                .fetchedAt(entity.getFetchedAt())
                .location(entity.getLocation())
                .metricType(entity.getMetricType().name())
                .build();
    }
}
