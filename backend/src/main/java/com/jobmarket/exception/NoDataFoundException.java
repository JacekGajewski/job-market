package com.jobmarket.exception;

public class NoDataFoundException extends RuntimeException {

    public NoDataFoundException(String category) {
        super("No data found for category: " + category);
    }
}
