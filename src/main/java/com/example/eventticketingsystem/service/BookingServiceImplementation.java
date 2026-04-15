package com.example.eventticketingsystem.service;

import com.example.eventticketingsystem.dto.booking.request.BookingCreateRequest;
import com.example.eventticketingsystem.dto.booking.response.BookingItemResponse;
import com.example.eventticketingsystem.dto.booking.response.BookingResponse;
import com.example.eventticketingsystem.dto.common.PagedResponse;
import com.example.eventticketingsystem.entity.Booking;
import com.example.eventticketingsystem.entity.BookingItem;
import com.example.eventticketingsystem.entity.Event;
import com.example.eventticketingsystem.entity.Seat;
import com.example.eventticketingsystem.entity.User;
import com.example.eventticketingsystem.entity.enums.BookingStatus;
import com.example.eventticketingsystem.entity.enums.EventStatus;
import com.example.eventticketingsystem.entity.enums.SeatStatus;
import com.example.eventticketingsystem.exception.ConflictException;
import com.example.eventticketingsystem.exception.FieldErrorDetail;
import com.example.eventticketingsystem.exception.InvalidStateTransitionException;
import com.example.eventticketingsystem.exception.ResourceNotFoundException;
import com.example.eventticketingsystem.repository.BookingRepository;
import com.example.eventticketingsystem.repository.EventRepository;
import com.example.eventticketingsystem.repository.SeatRepository;
import com.example.eventticketingsystem.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class BookingServiceImplementation implements BookingService {

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;

    public BookingServiceImplementation(BookingRepository bookingRepository,
                                        EventRepository eventRepository,
                                        SeatRepository seatRepository,
                                        UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.eventRepository = eventRepository;
        this.seatRepository = seatRepository;
        this.userRepository = userRepository;
    }

    // -------------------------------------------------------------------------
    // Customer: Create Booking
    // POST /api/v1/bookings
    // -------------------------------------------------------------------------
    @Override
    @Transactional
    public BookingResponse createBooking(Long currentUserId, BookingCreateRequest request) {
        // Validate event exists.
        // Validate event is ACTIVE.
        // Validate requested seats and ensure all are AVAILABLE.
        // Resolve the authenticated user.
        // Calculate the total booking amount from current seat prices.
        // Create the booking in PENDING_PAYMENT state.
        // Create booking items, snapshot seat prices, and mark seats as BOOKED.

        // Validate event exists.
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "The event with id '" + request.getEventId() + "' was not found."));

        // Validate event is ACTIVE.
        if (event.getStatus() != EventStatus.ACTIVE) {
            throw new ConflictException("EventNotActive",
                    "Cannot create a booking for event '" + event.getId() + "' because it is not in ACTIVE status.");
        }

        // Validate requested seats and ensure all are AVAILABLE.
        List<FieldErrorDetail> unavailableDetails = new ArrayList<>();
        List<Seat> seatsToBook = new ArrayList<>();

        for (int i = 0; i < request.getSeatIds().size(); i++) {
            Long seatId = request.getSeatIds().get(i);
            Seat seat = seatRepository.findByIdAndEvent_Id(seatId, event.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "The seat with id '" + seatId + "' was not found for event '" + event.getId() + "'."));

            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                unavailableDetails.add(new FieldErrorDetail(
                        "seatIds[" + i + "]",
                        "Seat '" + seat.getSeatNumber() + "' (id: " + seat.getId() + ") has status '"
                                + seat.getStatus() + "' and cannot be reserved."
                ));
            } else {
                seatsToBook.add(seat);
            }
        }

        if (!unavailableDetails.isEmpty()) {
            throw new ConflictException("SeatUnavailable",
                    "One or more selected seats are not available. The entire booking has been rolled back.",
                    unavailableDetails);
        }

        // Resolve the authenticated user.
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "The user with id '" + currentUserId + "' was not found."));

        // Calculate the total booking amount from current seat prices.
        BigDecimal totalAmount = seatsToBook.stream()
                .map(Seat::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Create the booking in PENDING_PAYMENT state.
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setEvent(event);
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setTotalAmount(totalAmount);

        // Create booking items, snapshot seat prices, and mark seats as BOOKED.
        for (Seat seat : seatsToBook) {
            BookingItem item = new BookingItem();
            item.setBooking(booking);
            item.setSeat(seat);
            item.setPriceAtBooking(seat.getPrice());  // price snapshot
            booking.getItems().add(item);

            seat.setStatus(SeatStatus.BOOKED);        // mark seat — triggers optimistic lock check
            seatRepository.save(seat);
        }

        Booking saved = bookingRepository.save(booking);
        return toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Customer: List My Bookings
    // GET /api/v1/bookings
    // -------------------------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public PagedResponse<BookingResponse> listMyBookings(Long currentUserId, int limit, int offset, String sort) {
        Pageable pageable = buildPageable(limit, offset, sort);
        Page<Booking> page = bookingRepository.findByUser_Id(currentUserId, pageable);
        List<BookingResponse> items = page.getContent().stream().map(this::toResponse).toList();
        return new PagedResponse<>(items, limit, offset, page.getTotalElements());
    }

    // -------------------------------------------------------------------------
    // Customer (owner): Get Booking By Id
    // GET /api/v1/bookings/{bookingId}
    // -------------------------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long bookingId, Long currentUserId) {
        Booking booking = findBookingOrThrow(bookingId);

        if (!booking.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException(
                    "You do not have permission to access this booking. Only the booking owner can view it.");
        }
        return toResponse(booking);
    }

    // -------------------------------------------------------------------------
    // Customer: Confirm Booking
    // POST /api/v1/bookings/{bookingId}/confirm
    // -------------------------------------------------------------------------
    @Override
    @Transactional
    public BookingResponse confirmBooking(Long bookingId, Long currentUserId) {
        Booking booking = findBookingOrThrow(bookingId);

        if (!booking.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException(
                    "You do not have permission to confirm this booking. Only the booking owner can confirm it.");
        }

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new InvalidStateTransitionException("InvalidStateTransition",
                    "Cannot confirm a booking with current status '" + booking.getStatus()
                            + "'. Only PENDING_PAYMENT bookings can be confirmed.");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        return toResponse(bookingRepository.save(booking));
    }

    // -------------------------------------------------------------------------
    // Customer: Cancel Booking
    // POST /api/v1/bookings/{bookingId}/cancel
    // -------------------------------------------------------------------------
    @Override
    @Transactional
    public BookingResponse cancelBooking(Long bookingId, Long currentUserId) {
        Booking booking = findBookingOrThrow(bookingId);

        if (!booking.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException(
                    "You do not have permission to cancel this booking. Only the booking owner can cancel it.");
        }

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new InvalidStateTransitionException("InvalidStateTransition",
                    "Cannot cancel a booking with current status '" + booking.getStatus()
                            + "'. Only PENDING_PAYMENT bookings can be cancelled.");
        }

        booking.setStatus(BookingStatus.CANCELLED);

        // Release seats back to AVAILABLE
        booking.getItems().forEach(item -> {
            Seat seat = item.getSeat();
            seat.setStatus(SeatStatus.AVAILABLE);
            seatRepository.save(seat);
        });

        return toResponse(bookingRepository.save(booking));
    }

    // -------------------------------------------------------------------------
    // Customer / System: Mark Payment Failed
    // POST /api/v1/bookings/{bookingId}/payment-failed
    // -------------------------------------------------------------------------
    @Override
    @Transactional
    public BookingResponse markPaymentFailed(Long bookingId, Long currentUserId) {
        Booking booking = findBookingOrThrow(bookingId);

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new InvalidStateTransitionException("InvalidStateTransition",
                    "Cannot report payment failure for a booking with current status '" + booking.getStatus()
                            + "'. Only PENDING_PAYMENT bookings can transition to PAYMENT_FAILED.");
        }

        booking.setStatus(BookingStatus.PAYMENT_FAILED);

        // Release seats back to AVAILABLE
        booking.getItems().forEach(item -> {
            Seat seat = item.getSeat();
            seat.setStatus(SeatStatus.AVAILABLE);
            seatRepository.save(seat);
        });

        return toResponse(bookingRepository.save(booking));
    }

    // -------------------------------------------------------------------------
    // Admin: List All Bookings (with optional filters)
    // GET /api/v1/admin/bookings
    // -------------------------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public PagedResponse<BookingResponse> listAllBookings(int limit, int offset, String sort,
                                                          BookingStatus status, Long eventId, Long userId) {
        Pageable pageable = buildPageable(limit, offset, sort);

        Specification<Booking> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null)  predicates.add(cb.equal(root.get("status"), status));
            if (eventId != null) predicates.add(cb.equal(root.get("event").get("id"), eventId));
            if (userId != null)  predicates.add(cb.equal(root.get("user").get("id"), userId));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Booking> page = bookingRepository.findAll(spec, pageable);
        List<BookingResponse> items = page.getContent().stream().map(this::toResponse).toList();
        return new PagedResponse<>(items, limit, offset, page.getTotalElements());
    }

    // -------------------------------------------------------------------------
    // Admin: Get Booking By Id (no ownership check)
    // GET /api/v1/admin/bookings/{bookingId}
    // -------------------------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingByIdAdmin(Long bookingId) {
        return toResponse(findBookingOrThrow(bookingId));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------
    private Booking findBookingOrThrow(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "The booking with id '" + bookingId + "' was not found."));
    }

    private Pageable buildPageable(int limit, int offset, String sort) {
        int page = (limit > 0) ? offset / limit : 0;
        Sort sortObj = parseSort(sort);
        return PageRequest.of(page, limit, sortObj);
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) return Sort.by(Sort.Direction.DESC, "createdAt");
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        Sort.Direction direction = (parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, field);
    }

    private BookingResponse toResponse(Booking booking) {
        BookingResponse response = new BookingResponse();
        response.setId(booking.getId());
        response.setEventId(booking.getEvent().getId());
        response.setUserId(booking.getUser().getId());
        response.setStatus(booking.getStatus());
        response.setTotalAmount(booking.getTotalAmount());
        response.setBookingDate(booking.getBookingDate());
        response.setCreatedAt(booking.getCreatedAt());
        response.setUpdatedAt(booking.getUpdatedAt());

        List<BookingItemResponse> items = new ArrayList<>();
        for (BookingItem item : booking.getItems()) {
            items.add(new BookingItemResponse(
                    item.getId(),
                    item.getSeat().getId(),
                    item.getPriceAtBooking(),
                    item.getCreatedAt()
            ));
        }
        response.setItems(items);
        return response;
    }
}

