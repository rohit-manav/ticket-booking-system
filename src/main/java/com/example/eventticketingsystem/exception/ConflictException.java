package com.example.eventticketingsystem.exception;

import java.util.List;

public class ConflictException extends RuntimeException {
    private final String errorCode;
    private final List<FieldErrorDetail> details;

    public ConflictException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = null;
    }

    public ConflictException(String errorCode, String message, List<FieldErrorDetail> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public List<FieldErrorDetail> getDetails() {
        return details;
    }
}
