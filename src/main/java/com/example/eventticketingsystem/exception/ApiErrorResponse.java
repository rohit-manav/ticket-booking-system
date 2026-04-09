package com.example.eventticketingsystem.exception;

import java.time.Instant;
import java.util.List;

public class ApiErrorResponse {
    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private List<FieldErrorDetail> details;

    public ApiErrorResponse(Instant timestamp, int status, String error, String message, String path, List<FieldErrorDetail> details) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.details = details;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public List<FieldErrorDetail> getDetails() {
        return details;
    }
}

