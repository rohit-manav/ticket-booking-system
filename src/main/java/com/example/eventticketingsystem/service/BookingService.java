package com.example.eventticketingsystem.service;

import com.example.eventticketingsystem.dto.booking.request.BookingCreateRequest;
import com.example.eventticketingsystem.dto.booking.response.BookingResponse;
import com.example.eventticketingsystem.dto.common.PagedResponse;
import com.example.eventticketingsystem.entity.enums.BookingStatus;

public interface BookingService {

    // Customer operations
    BookingResponse createBooking(Long currentUserId, BookingCreateRequest request);

    PagedResponse<BookingResponse> listMyBookings(Long currentUserId, int limit, int offset, String sort);

    BookingResponse getBookingById(Long bookingId, Long currentUserId);

    BookingResponse confirmBooking(Long bookingId, Long currentUserId);

    BookingResponse cancelBooking(Long bookingId, Long currentUserId);

    BookingResponse markPaymentFailed(Long bookingId, Long currentUserId);

    // Admin operations
    PagedResponse<BookingResponse> listAllBookings(int limit, int offset, String sort,
                                                    BookingStatus status, Long eventId, Long userId);

    BookingResponse getBookingByIdAdmin(Long bookingId);
}

