package com.siemens.internship.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

//Exception thrown when a requested resource is not found
//Maps to HTTP status 404 (NOT_FOUND)
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message);
    }

    //Constructor with error message and cause
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}