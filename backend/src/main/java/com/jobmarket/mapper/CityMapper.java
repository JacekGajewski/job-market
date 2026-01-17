package com.jobmarket.mapper;

import com.jobmarket.dto.CityDto;
import com.jobmarket.entity.TrackedCity;
import org.springframework.stereotype.Component;

@Component
public class CityMapper {

    public CityDto toDto(TrackedCity entity) {
        return CityDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .slug(entity.getSlug())
                .active(entity.getActive())
                .build();
    }
}
