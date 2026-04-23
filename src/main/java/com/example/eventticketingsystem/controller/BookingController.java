package com.example.eventticketingsystem.controller;

import com.example.eventticketingsystem.dto.booking.request.BookingCreateRequest;
import com.example.eventticketingsystem.dto.booking.response.BookingResponse;
import com.example.eventticketingsystem.dto.common.PagedResponse;
import com.example.eventticketingsystem.entity.enums.BookingStatus;
import com.example.eventticketingsystem.service.BookingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Single controller for all Booking operations.
 * Access is controlled via permission authorities from JWT scopes.
 * The currentUserId parameter is resolved from JWT via @AuthenticationPrincipal.
 */
@RestController
@Tag(
        name = "Bookings",
        description = "Booking lifecycle endpoints."
)
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // -------------------------------------------------------------------------
    // Customer: Create Booking
    // POST /api/v1/bookings
    // -------------------------------------------------------------------------

    @PreAuthorize("hasAuthority('booking')")
    @PostMapping("/api/v1/bookings")
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody BookingCreateRequest request,
            @AuthenticationPrincipal Long currentUserId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(currentUserId, request));
    }

    // -------------------------------------------------------------------------
    // Shared: List Bookings (CUSTOMER = own bookings, ADMIN = all bookings)
    // GET /api/v1/bookings
    // -------------------------------------------------------------------------

    @PreAuthorize("hasAuthority('booking')")
    @GetMapping("/api/v1/bookings")
    public PagedResponse<BookingResponse> listBookings(
            @AuthenticationPrincipal Long currentUserId,
            @RequestParam(defaultValue = "25") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Long userId,
            Authentication authentication) {
        if (isAdmin(authentication)) {
            return bookingService.listAllBookings(limit, offset, sort, status, eventId, userId);
        }
        return bookingService.listMyBookings(currentUserId, limit, offset, sort);
    }

    // -------------------------------------------------------------------------
    // Shared: Get Booking By Id (CUSTOMER = own booking, ADMIN = any booking)
    // GET /api/v1/bookings/{bookingId}
    // -------------------------------------------------------------------------

    @PreAuthorize("hasAuthority('booking')")
    @GetMapping("/api/v1/bookings/{bookingId}")
    public BookingResponse getBookingById(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal Long currentUserId,
            Authentication authentication) {
        if (isAdmin(authentication)) {
            return bookingService.getBookingByIdAdmin(bookingId);
        }
        return bookingService.getBookingById(bookingId, currentUserId);
    }

    // -------------------------------------------------------------------------
    // Customer: Confirm Booking
    // POST /api/v1/bookings/{bookingId}/confirm
    // -------------------------------------------------------------------------

    @PreAuthorize("hasAuthority('booking')")
    @PostMapping("/api/v1/bookings/{bookingId}/confirm")
    public BookingResponse confirmBooking(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal Long currentUserId) {
        return bookingService.confirmBooking(bookingId, currentUserId);
    }

    // -------------------------------------------------------------------------
    // Customer: Cancel Booking
    // POST /api/v1/bookings/{bookingId}/cancel
    // -------------------------------------------------------------------------

    @PreAuthorize("hasAuthority('booking')")
    @PostMapping("/api/v1/bookings/{bookingId}/cancel")
    public BookingResponse cancelBooking(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal Long currentUserId) {
        return bookingService.cancelBooking(bookingId, currentUserId);
    }

    // -------------------------------------------------------------------------
    // Customer / System: Mark Payment Failed
    // POST /api/v1/bookings/{bookingId}/payment-failed
    // -------------------------------------------------------------------------

    @PreAuthorize("hasAuthority('booking')")
    @PostMapping("/api/v1/bookings/{bookingId}/payment-failed")
    public BookingResponse markPaymentFailed(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal Long currentUserId) {
        return bookingService.markPaymentFailed(bookingId, currentUserId);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null
                && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
}

