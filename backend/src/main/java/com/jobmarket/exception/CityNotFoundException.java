package com.jobmarket.exception;

public class CityNotFoundException extends RuntimeException {

    public CityNotFoundException(Long id) {
        super("City not found with id: " + id);
    }

    public CityNotFoundException(String slug) {
        super("City not found with slug: " + slug);
    }
}
