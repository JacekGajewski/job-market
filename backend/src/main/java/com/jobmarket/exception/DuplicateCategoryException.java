package com.jobmarket.exception;

public class DuplicateCategoryException extends RuntimeException {

    public DuplicateCategoryException(String slug) {
        super("Category already exists with slug: " + slug);
    }
}
