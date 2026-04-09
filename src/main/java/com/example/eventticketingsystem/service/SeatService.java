package com.example.eventticketingsystem.service;

import com.example.eventticketingsystem.dto.seat.request.SeatCreateRequest;
import com.example.eventticketingsystem.dto.seat.request.SeatUpdateRequest;
import com.example.eventticketingsystem.dto.seat.response.SeatCreateResponse;
import com.example.eventticketingsystem.dto.seat.response.SeatListResponse;
import com.example.eventticketingsystem.dto.seat.response.SeatResponse;
import com.example.eventticketingsystem.entity.enums.SeatStatus;

public interface SeatService {
    SeatCreateResponse createSeats(Long eventId, SeatCreateRequest request);

    SeatResponse updateSeat(Long eventId, Long seatId, SeatUpdateRequest request);

    SeatListResponse listSeatsForEvent(Long eventId, SeatStatus status, String category);
}
