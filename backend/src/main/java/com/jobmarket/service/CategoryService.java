package com.jobmarket.service;

import com.jobmarket.dto.CategoryDto;
import com.jobmarket.dto.CreateCategoryRequest;
import com.jobmarket.entity.TrackedCategory;
import com.jobmarket.exception.CategoryNotFoundException;
import com.jobmarket.exception.DuplicateCategoryException;
import com.jobmarket.mapper.CategoryMapper;
import com.jobmarket.repository.TrackedCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CategoryService {

    private final TrackedCategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public List<CategoryDto> findAll() {
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toDto)
                .toList();
    }

    public List<CategoryDto> findAllActive() {
        return categoryRepository.findByActiveTrue().stream()
                .map(categoryMapper::toDto)
                .toList();
    }

    @Transactional
    public CategoryDto create(CreateCategoryRequest request) {
        if (categoryRepository.existsBySlug(request.slug())) {
            throw new DuplicateCategoryException(request.slug());
        }

        TrackedCategory category = TrackedCategory.builder()
                .name(request.name())
                .slug(request.slug())
                .active(true)
                .build();

        TrackedCategory saved = categoryRepository.save(category);
        log.info("Created new category: {}", saved.getSlug());
        return categoryMapper.toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new CategoryNotFoundException(id);
        }
        categoryRepository.deleteById(id);
        log.info("Deleted category with id: {}", id);
    }

    @Transactional
    public CategoryDto setActive(Long id, boolean active) {
        TrackedCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
        category.setActive(active);
        TrackedCategory saved = categoryRepository.save(category);
        log.info("Set category {} active={}", saved.getSlug(), active);
        return categoryMapper.toDto(saved);
    }
}
