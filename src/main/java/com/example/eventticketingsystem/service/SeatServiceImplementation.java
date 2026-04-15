package com.example.eventticketingsystem.service;

import com.example.eventticketingsystem.dto.seat.request.SeatCreateRequest;
import com.example.eventticketingsystem.dto.seat.request.SeatUpdateRequest;
import com.example.eventticketingsystem.dto.seat.response.SeatCreateResponse;
import com.example.eventticketingsystem.dto.seat.response.SeatListResponse;
import com.example.eventticketingsystem.dto.seat.response.SeatResponse;
import com.example.eventticketingsystem.entity.Event;
import com.example.eventticketingsystem.entity.Seat;
import com.example.eventticketingsystem.entity.enums.EventStatus;
import com.example.eventticketingsystem.entity.enums.SeatStatus;
import com.example.eventticketingsystem.exception.ConflictException;
import com.example.eventticketingsystem.exception.EventNotActiveException;
import com.example.eventticketingsystem.exception.FieldErrorDetail;
import com.example.eventticketingsystem.exception.ResourceNotFoundException;
import com.example.eventticketingsystem.repository.EventRepository;
import com.example.eventticketingsystem.repository.SeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SeatServiceImplementation implements SeatService {

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;

    public SeatServiceImplementation(EventRepository eventRepository, SeatRepository seatRepository) {
        this.eventRepository = eventRepository;
        this.seatRepository = seatRepository;
    }

    @Override
    @Transactional
    public SeatCreateResponse createSeats(Long eventId, SeatCreateRequest request) {
        Event event = findEventOrThrow(eventId);

        List<String> requestedNumbers = request.getSeats().stream()
                .map(s -> s.getSeatNumber().trim())
                .toList();

        List<Seat> duplicates = seatRepository.findByEvent_IdAndSeatNumberIn(eventId, requestedNumbers);
        if (!duplicates.isEmpty()) {
            List<FieldErrorDetail> details = duplicates.stream()
                    .map(s -> new FieldErrorDetail(
                            "seats[].seatNumber",
                            "Seat number '" + s.getSeatNumber() + "' already exists for event id '" + eventId + "'."))
                    .toList();
            throw new ConflictException(
                    "DuplicateSeatNumber",
                    "One or more seat numbers already exist for this event. The entire batch has been rejected.",
                    details
            );
        }

        List<Seat> seats = request.getSeats().stream().map(item -> {
            Seat seat = new Seat();
            seat.setEvent(event);
            seat.setSeatNumber(item.getSeatNumber().trim());
            seat.setCategory(item.getCategory().trim());
            seat.setPrice(item.getPrice());
            seat.setStatus(SeatStatus.AVAILABLE);
            return seat;
        }).toList();

        List<Seat> saved = seatRepository.saveAll(seats);
        List<SeatResponse> items = saved.stream().map(this::toResponse).toList();
        return new SeatCreateResponse(items);
    }

    @Override
    @Transactional
    public SeatResponse updateSeat(Long eventId, Long seatId, SeatUpdateRequest request) {
        findEventOrThrow(eventId);

        if (request.getStatus() == SeatStatus.BOOKED) {
            throw new IllegalArgumentException(
                    "Seat status cannot be manually set to 'BOOKED'. Only the booking system can set this status."
            );
        }

        Seat seat = seatRepository.findByIdAndEvent_Id(seatId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "The seat with id '" + seatId + "' was not found for event '" + eventId + "'."));

        seat.setCategory(request.getCategory().trim());
        seat.setPrice(request.getPrice());
        seat.setStatus(request.getStatus());

        return toResponse(seatRepository.save(seat));
    }

    @Override
    @Transactional(readOnly = true)
    public SeatListResponse listSeatsForEvent(Long eventId, SeatStatus status, String category) {
        Event event = findEventOrThrow(eventId);

        if (event.getStatus() != EventStatus.ACTIVE) {
            throw new EventNotActiveException("Seats cannot be viewed for an inactive event.");
        }

        List<Seat> seats;
        if (status != null && category != null) {
            seats = seatRepository.findByEvent_IdAndStatusAndCategory(eventId, status, category);
        } else if (status != null) {
            seats = seatRepository.findByEvent_IdAndStatus(eventId, status);
        } else if (category != null) {
            seats = seatRepository.findByEvent_IdAndCategory(eventId, category);
        } else {
            seats = seatRepository.findByEvent_Id(eventId);
        }

        List<SeatResponse> items = seats.stream().map(this::toResponse).toList();
        return new SeatListResponse(items);
    }

    private Event findEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "The event with id '" + eventId + "' was not found."));
    }

    private SeatResponse toResponse(Seat seat) {
        SeatResponse response = new SeatResponse();
        response.setId(seat.getId());
        response.setEventId(seat.getEvent().getId());
        response.setSeatNumber(seat.getSeatNumber());
        response.setCategory(seat.getCategory());
        response.setPrice(seat.getPrice());
        response.setStatus(seat.getStatus());
        response.setCreatedAt(seat.getCreatedAt());
        response.setUpdatedAt(seat.getUpdatedAt());
        return response;
    }
}

