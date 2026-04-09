package com.example.eventticketingsystem.dto.event.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class EventCreateRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 1000)
    private String description;

    @NotBlank
    @Size(max = 300)
    private String venue;

    @NotNull
    @Future
    private Instant eventDateTime;

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
}

