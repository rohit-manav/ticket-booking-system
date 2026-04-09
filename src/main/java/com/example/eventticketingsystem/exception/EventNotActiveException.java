package com.example.eventticketingsystem.exception;

public class EventNotActiveException extends RuntimeException {

    public EventNotActiveException(String message) {
        super(message);
    }
}

