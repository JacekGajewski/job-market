package com.jobmarket.mapper;

import com.jobmarket.dto.CategoryDto;
import com.jobmarket.entity.TrackedCategory;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryDto toDto(TrackedCategory entity) {
        return CategoryDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .slug(entity.getSlug())
                .active(entity.getActive())
                .build();
    }
}
