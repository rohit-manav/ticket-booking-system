package com.example.eventticketingsystem.dto.seat.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class SeatCreateRequest {

    @NotEmpty(message = "At least one seat is required.")
    @Valid
    private List<SeatItemRequest> seats;

    public List<SeatItemRequest> getSeats() {
        return seats;
    }

    public void setSeats(List<SeatItemRequest> seats) {
        this.seats = seats;
    }
}

