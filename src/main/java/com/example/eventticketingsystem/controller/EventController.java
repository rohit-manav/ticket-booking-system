package com.example.eventticketingsystem.controller;

import com.example.eventticketingsystem.dto.common.PagedResponse;
import com.example.eventticketingsystem.dto.event.request.EventCreateRequest;
import com.example.eventticketingsystem.dto.event.request.EventStatusUpdateRequest;
import com.example.eventticketingsystem.dto.event.request.EventUpdateRequest;
import com.example.eventticketingsystem.dto.event.response.EventResponse;
import com.example.eventticketingsystem.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Single controller for all Event operations.
 * Access is controlled at the method level via @PreAuthorize:
 *   - ADMIN  : write operations (create / update / delete / status change)
 *   - ADMIN or CUSTOMER : read operations (browse / get details)
 */
@RestController
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // -------------------------------------------------------------------------
    // Admin operations  —  /api/v1/admin/events
    // -------------------------------------------------------------------------

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/v1/admin/events")
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody EventCreateRequest request) {
        EventResponse response = eventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/api/v1/admin/events/{eventId}")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody EventUpdateRequest request) {
        return ResponseEntity.ok(eventService.updateEvent(eventId, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/api/v1/admin/events/{eventId}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long eventId) {
        eventService.deleteEvent(eventId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/api/v1/admin/events/{eventId}/status")
    public ResponseEntity<EventResponse> updateEventStatus(
            @PathVariable Long eventId,
            @Valid @RequestBody EventStatusUpdateRequest request) {
        return ResponseEntity.ok(eventService.updateEventStatus(eventId, request));
    }

    // -------------------------------------------------------------------------
    // Shared operations  —  /api/v1/events  (ADMIN + CUSTOMER)
    // -------------------------------------------------------------------------

    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @GetMapping("/api/v1/events")
    public PagedResponse<EventResponse> listEvents(
            @RequestParam(defaultValue = "25") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "eventDateTime,asc") String sort) {
        return eventService.listActiveEvents(limit, offset, sort);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @GetMapping("/api/v1/events/{eventId}")
    public EventResponse getEvent(@PathVariable Long eventId) {
        return eventService.getEventById(eventId);
    }
}