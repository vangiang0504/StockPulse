package com.training.starter.exception;

public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(resourceName + " already exists with " + fieldName + ": " + fieldValue);
    }
}
