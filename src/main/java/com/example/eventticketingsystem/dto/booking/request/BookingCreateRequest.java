package com.example.eventticketingsystem.dto.booking.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class BookingCreateRequest {

    @NotNull(message = "The 'eventId' field is required.")
    private Long eventId;

    @NotNull(message = "The 'seatIds' field is required.")
    @NotEmpty(message = "At least one seat ID is required.")
    private List<Long> seatIds;

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public List<Long> getSeatIds() {
        return seatIds;
    }

    public void setSeatIds(List<Long> seatIds) {
        this.seatIds = seatIds;
    }
}

