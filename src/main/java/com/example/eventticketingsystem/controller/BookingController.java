package com.example.eventticketingsystem.controller;

import com.example.eventticketingsystem.dto.booking.request.BookingCreateRequest;
import com.example.eventticketingsystem.dto.booking.response.BookingResponse;
import com.example.eventticketingsystem.dto.common.PagedResponse;
import com.example.eventticketingsystem.entity.enums.BookingStatus;
import com.example.eventticketingsystem.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Single controller for all Booking operations.
 *
 * Access is controlled at the method level via @PreAuthorize:
 *   - CUSTOMER : create, view own bookings, confirm/cancel/payment-failed
 *   - ADMIN    : view all bookings, view any booking by id
 *
 * The currentUserId parameter represents the authenticated user's ID.
 * TODO: Replace @RequestParam userId with @AuthenticationPrincipal once JWT is wired up.
 */
@RestController
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // -------------------------------------------------------------------------
    // Customer: Create Booking
    // POST /api/v1/bookings
    // -------------------------------------------------------------------------

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/api/v1/bookings")
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody BookingCreateRequest request,
            @RequestParam Long userId) {                        // TODO: replace with @AuthenticationPrincipal
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(userId, request));
    }

    // -------------------------------------------------------------------------
    // Customer: List My Bookings
    // GET /api/v1/bookings
    // -------------------------------------------------------------------------

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/api/v1/bookings")
    public PagedResponse<BookingResponse> listMyBookings(
            @RequestParam Long userId,                          // TODO: replace with @AuthenticationPrincipal
            @RequestParam(defaultValue = "25") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return bookingService.listMyBookings(userId, limit, offset, sort);
    }

    // -------------------------------------------------------------------------
    // Customer (owner): Get Booking By Id
    // GET /api/v1/bookings/{bookingId}
    // -------------------------------------------------------------------------

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/api/v1/bookings/{bookingId}")
    public BookingResponse getBookingById(
            @PathVariable Long bookingId,
            @RequestParam Long userId) {                        // TODO: replace with @AuthenticationPrincipal
        return bookingService.getBookingById(bookingId, userId);
    }

    // -------------------------------------------------------------------------
    // Customer: Confirm Booking
    // POST /api/v1/bookings/{bookingId}/confirm
    // -------------------------------------------------------------------------

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/api/v1/bookings/{bookingId}/confirm")
    public BookingResponse confirmBooking(
            @PathVariable Long bookingId,
            @RequestParam Long userId) {                        // TODO: replace with @AuthenticationPrincipal
        return bookingService.confirmBooking(bookingId, userId);
    }

    // -------------------------------------------------------------------------
    // Customer: Cancel Booking
    // POST /api/v1/bookings/{bookingId}/cancel
    // -------------------------------------------------------------------------

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/api/v1/bookings/{bookingId}/cancel")
    public BookingResponse cancelBooking(
            @PathVariable Long bookingId,
            @RequestParam Long userId) {                        // TODO: replace with @AuthenticationPrincipal
        return bookingService.cancelBooking(bookingId, userId);
    }

    // -------------------------------------------------------------------------
    // Customer / System: Mark Payment Failed
    // POST /api/v1/bookings/{bookingId}/payment-failed
    // -------------------------------------------------------------------------

    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @PostMapping("/api/v1/bookings/{bookingId}/payment-failed")
    public BookingResponse markPaymentFailed(
            @PathVariable Long bookingId,
            @RequestParam Long userId) {                        // TODO: replace with @AuthenticationPrincipal
        return bookingService.markPaymentFailed(bookingId, userId);
    }

    // -------------------------------------------------------------------------
    // Admin: List All Bookings (with optional filters)
    // GET /api/v1/admin/bookings
    // -------------------------------------------------------------------------

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/v1/admin/bookings")
    public PagedResponse<BookingResponse> listAllBookings(
            @RequestParam(defaultValue = "25") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Long userId) {
        return bookingService.listAllBookings(limit, offset, sort, status, eventId, userId);
    }

    // -------------------------------------------------------------------------
    // Admin: Get Any Booking By Id
    // GET /api/v1/admin/bookings/{bookingId}
    // -------------------------------------------------------------------------

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/v1/admin/bookings/{bookingId}")
    public BookingResponse getBookingByIdAdmin(@PathVariable Long bookingId) {
        return bookingService.getBookingByIdAdmin(bookingId);
    }
}

