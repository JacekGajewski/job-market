package com.jobmarket.exception;

public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(Long id) {
        super("Category not found with id: " + id);
    }

    public CategoryNotFoundException(String slug) {
        super("Category not found with slug: " + slug);
    }
}
