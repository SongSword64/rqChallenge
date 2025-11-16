package com.reliaquest.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an employee cannot be found by ID or by search criteria.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class EmployeeNotFoundException extends RuntimeException {

    public EmployeeNotFoundException(String id) {
        super("Employee not found with id: " + id);
    }

    public EmployeeNotFoundException(String field, String value) {
        super("Employee not found with " + field + ": " + value);
    }

    public EmployeeNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
