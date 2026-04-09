package com.example.eventticketingsystem.exception;

public class InvalidStateTransitionException extends RuntimeException {

    private final String errorCode;

    public InvalidStateTransitionException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

