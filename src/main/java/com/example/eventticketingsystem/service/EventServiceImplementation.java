package com.example.eventticketingsystem.service;

import com.example.eventticketingsystem.dto.common.PagedResponse;
import com.example.eventticketingsystem.dto.event.request.EventCreateRequest;
import com.example.eventticketingsystem.dto.event.request.EventStatusUpdateRequest;
import com.example.eventticketingsystem.dto.event.request.EventUpdateRequest;
import com.example.eventticketingsystem.dto.event.response.EventResponse;
import com.example.eventticketingsystem.entity.Booking;
import com.example.eventticketingsystem.entity.BookingItem;
import com.example.eventticketingsystem.entity.Event;
import com.example.eventticketingsystem.entity.Seat;
import com.example.eventticketingsystem.entity.enums.BookingStatus;
import com.example.eventticketingsystem.entity.enums.EventStatus;
import com.example.eventticketingsystem.entity.enums.SeatStatus;
import com.example.eventticketingsystem.exception.ConflictException;
import com.example.eventticketingsystem.exception.ResourceNotFoundException;
import com.example.eventticketingsystem.repository.EventRepository;
import com.example.eventticketingsystem.repository.SeatRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class EventServiceImplementation implements EventService {

    private static final int DEFAULT_LIMIT = 25;
    private static final int MAX_LIMIT = 100;

    private static final Map<String, String> SORT_MAPPING = Map.of(
            "eventDateTime", "eventDateTime",
            "name", "name",
            "createdAt", "createdAt",
            "updatedAt", "updatedAt"
    );

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;

    public EventServiceImplementation(EventRepository eventRepository,
                                      SeatRepository seatRepository) {
        this.eventRepository = eventRepository;
        this.seatRepository = seatRepository;
    }

    @Override
    public EventResponse createEvent(EventCreateRequest request) {
        validateFutureDate(request.getEventDateTime());

        Event event = new Event();
        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setVenue(request.getVenue());
        event.setEventDateTime(request.getEventDateTime());
        event.setStatus(EventStatus.INACTIVE);

        return toResponse(eventRepository.save(event));
    }

    @Override
    public EventResponse updateEvent(Long eventId, EventUpdateRequest request) {
        validateFutureDate(request.getEventDateTime());

        Event event = getEventEntity(eventId);
        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setVenue(request.getVenue());
        event.setEventDateTime(request.getEventDateTime());

        return toResponse(eventRepository.save(event));
    }

    @Override
    public void deleteEvent(Long eventId) {
        Event event = getEventEntity(eventId);

        // Soft-delete all bookings and their booking items for this event
        for (Booking booking : event.getBookings()) {
            for (BookingItem item : booking.getItems()) {
                item.setDeleted(true);
            }
            booking.setDeleted(true);
        }

        // Soft-delete all seats for this event
        List<Seat> seats = seatRepository.findByEvent_Id(eventId);
        for (Seat seat : seats) {
            seat.setDeleted(true);
        }
        seatRepository.saveAll(seats);

        // Soft-delete the event itself
        event.setDeleted(true);
        eventRepository.save(event);
    }

    @Override
    public EventResponse updateEventStatus(Long eventId, EventStatusUpdateRequest request) {
        Event event = getEventEntity(eventId);

        if (event.getStatus() == request.getStatus()) {
            if (request.getStatus() == EventStatus.ACTIVE) {
                throw new ConflictException("EventAlreadyActive", "Event '" + event.getName() + "' is already in ACTIVE status.");
            }
            throw new ConflictException("EventAlreadyInactive", "Event '" + event.getName() + "' is already in INACTIVE status.");
        }

        if (request.getStatus() == EventStatus.ACTIVE && !seatRepository.existsByEvent_Id(eventId)) {
            throw new ConflictException("EventHasNoSeats", "Event '" + event.getName() + "' cannot be activated without seats.");
        }

        if (request.getStatus() == EventStatus.INACTIVE) {
            for (Booking booking : event.getBookings()) {
                booking.setStatus(BookingStatus.CANCELLED);
            }

            List<Seat> seats = seatRepository.findByEvent_Id(eventId);
            for (Seat seat : seats) {
                seat.setStatus(SeatStatus.DISABLED);
            }
            seatRepository.saveAll(seats);
        }

        event.setStatus(request.getStatus());
        return toResponse(eventRepository.save(event));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<EventResponse> listActiveEvents(int limit, int offset, String sort) {
        int resolvedLimit = resolveLimit(limit);
        int resolvedOffset = Math.max(offset, 0);
        SortSpec sortSpec = resolveSort(sort);

        Sort springSort = sortSpec.ascending()
                ? Sort.by(sortSpec.sortBy()).ascending()
                : Sort.by(sortSpec.sortBy()).descending();

        int pageNumber = resolvedOffset / resolvedLimit;
        PageRequest pageRequest = PageRequest.of(pageNumber, resolvedLimit, springSort);

        Page<Event> page = eventRepository.findByStatus(EventStatus.ACTIVE, pageRequest);

        List<EventResponse> items = page.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new PagedResponse<>(items, resolvedLimit, resolvedOffset, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse getEventById(Long eventId) {
        Event event = getEventEntity(eventId);
        if (event.getStatus() != EventStatus.ACTIVE) {
            throw new ResourceNotFoundException("The event with id '" + eventId + "' was not found.");
        }
        return toResponse(event);
    }

    private Event getEventEntity(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("The event with id '" + eventId + "' was not found."));
    }

    private void validateFutureDate(Instant eventDateTime) {
        if (eventDateTime == null || !eventDateTime.isAfter(Instant.now())) {
            throw new IllegalArgumentException("Event date/time must be in the future.");
        }
    }

    private int resolveLimit(int limit) {
        if (limit <= 0) return DEFAULT_LIMIT;
        return Math.min(limit, MAX_LIMIT);
    }

    private SortSpec resolveSort(String sort) {
        String raw = (sort == null || sort.isBlank()) ? "eventDateTime,asc" : sort;
        String[] tokens = raw.split(",");
        String apiField = tokens[0].trim();
        String sortBy = SORT_MAPPING.getOrDefault(apiField, "eventDateTime");
        boolean ascending = tokens.length < 2 || !"desc".equalsIgnoreCase(tokens[1].trim());
        return new SortSpec(sortBy, ascending);
    }

    private EventResponse toResponse(Event event) {
        EventResponse response = new EventResponse();
        response.setId(event.getId());
        response.setName(event.getName());
        response.setDescription(event.getDescription());
        response.setVenue(event.getVenue());
        response.setEventDateTime(event.getEventDateTime());
        response.setStatus(event.getStatus());
        return response;
    }

    private record SortSpec(String sortBy, boolean ascending) {}
}
