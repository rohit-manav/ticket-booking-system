package com.example.eventticketingsystem.controller;

import com.example.eventticketingsystem.dto.common.PagedResponse;
import com.example.eventticketingsystem.dto.event.request.EventCreateRequest;
import com.example.eventticketingsystem.dto.event.request.EventStatusUpdateRequest;
import com.example.eventticketingsystem.dto.event.request.EventUpdateRequest;
import com.example.eventticketingsystem.dto.event.response.EventResponse;
import com.example.eventticketingsystem.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * Event management API controller.
 *
 * Permission-based access control using JWT scope authorities.
 */
@RestController
@Tag(
        name = "Events",
        description = "Event management endpoints."
)
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // =========================================================================
    // ADMIN OPERATIONS: /api/v1/events
    // =========================================================================

    @Operation(
            summary = "Create event",
            description = "Creates a new event in the system with event details"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('event.create')")
    @PostMapping("/api/v1/events")
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody EventCreateRequest request) {
        EventResponse response = eventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Update event",
            description = "Updates event details (name, description, venue, eventDateTime). " +
                    "Does not change status; use PATCH /status to activate or deactivate."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('event.update')")
    @PutMapping("/api/v1/events/{eventId}")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody EventUpdateRequest request) {
        return ResponseEntity.ok(eventService.updateEvent(eventId, request));
    }

    @Operation(
            summary = "Delete event",
            description = "Deletes an event from the system"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('event.delete')")
    @DeleteMapping("/api/v1/events/{eventId}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long eventId) {
        eventService.deleteEvent(eventId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Update event status",
            description = "Sets event lifecycle per PRD: ACTIVE (bookable, visible to customers) or INACTIVE (default after create; not bookable)."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('event.update')")
    @PatchMapping("/api/v1/events/{eventId}/status")
    public ResponseEntity<EventResponse> updateEventStatus(
            @PathVariable Long eventId,
            @Valid @RequestBody EventStatusUpdateRequest request) {
        return ResponseEntity.ok(eventService.updateEventStatus(eventId, request));
    }

    // =========================================================================
    // CUSTOMER OPERATIONS: /api/v1/events
    // =========================================================================

    @Operation(
            summary = "List active events",
            description = "Retrieves paginated list of active events. " +
                    "Supports filtering, pagination (limit/offset), and sorting"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('event.read')")
    @GetMapping("/api/v1/events")
    public PagedResponse<EventResponse> listEvents(
            @RequestParam(defaultValue = "25") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "eventDateTime,asc") String sort) {
        return eventService.listActiveEvents(limit, offset, sort);
    }

    @Operation(
            summary = "Get event details",
            description = "Retrieves detailed information about a specific event including seats and availability"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('event.read')")
    @GetMapping("/api/v1/events/{eventId}")
    public EventResponse getEvent(@PathVariable Long eventId) {
        return eventService.getEventById(eventId);
    }
}