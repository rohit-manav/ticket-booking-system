package com.example.eventticketingsystem.dto.event.response;

import com.example.eventticketingsystem.entity.enums.EventStatus;

import java.time.Instant;

public class EventResponse {
    private Long id;
    private String name;
    private String description;
    private String venue;
    private Instant eventDateTime;
    private EventStatus status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public Instant getEventDateTime() {
        return eventDateTime;
    }

    public void setEventDateTime(Instant eventDateTime) {
        this.eventDateTime = eventDateTime;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }
}
