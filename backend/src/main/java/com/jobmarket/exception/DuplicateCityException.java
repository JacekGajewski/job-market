package com.jobmarket.exception;

public class DuplicateCityException extends RuntimeException {

    public DuplicateCityException(String slug) {
        super("City already exists with slug: " + slug);
    }
}
