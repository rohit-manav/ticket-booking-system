package com.example.eventticketingsystem.controller;

import com.example.eventticketingsystem.dto.seat.request.SeatCreateRequest;
import com.example.eventticketingsystem.dto.seat.request.SeatUpdateRequest;
import com.example.eventticketingsystem.dto.seat.response.SeatCreateResponse;
import com.example.eventticketingsystem.dto.seat.response.SeatListResponse;
import com.example.eventticketingsystem.dto.seat.response.SeatResponse;
import com.example.eventticketingsystem.entity.enums.SeatStatus;
import com.example.eventticketingsystem.service.SeatService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Single controller for all Seat operations.
 * Access is controlled via permission authorities from JWT scopes.
 */
@RestController
@Tag(
        name = "Seats",
        description = "Seat inventory endpoints."
)
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    // -------------------------------------------------------------------------
    // Admin operations  —  /api/v1/events/{eventId}/seats
    // -------------------------------------------------------------------------

    @PreAuthorize("hasAuthority('seat.create')")
    @PostMapping("/api/v1/events/{eventId}/seats")
    public ResponseEntity<SeatCreateResponse> createSeats(
            @PathVariable Long eventId,
            @Valid @RequestBody SeatCreateRequest request) {
        SeatCreateResponse response = seatService.createSeats(eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAuthority('seat.update')")
    @PutMapping("/api/v1/events/{eventId}/seats/{seatId}")
    public ResponseEntity<SeatResponse> updateSeat(
            @PathVariable Long eventId,
            @PathVariable Long seatId,
            @Valid @RequestBody SeatUpdateRequest request) {
        return ResponseEntity.ok(seatService.updateSeat(eventId, seatId, request));
    }

    // -------------------------------------------------------------------------
    // Shared operation  —  /api/v1/events/{eventId}/seats  (ADMIN + CUSTOMER)
    // -------------------------------------------------------------------------

    @PreAuthorize("hasAuthority('seat.read')")
    @GetMapping("/api/v1/events/{eventId}/seats")
    public SeatListResponse listSeats(
            @PathVariable Long eventId,
            @RequestParam(required = false) SeatStatus status,
            @RequestParam(required = false) String category) {
        return seatService.listSeatsForEvent(eventId, status, category);
    }
}



