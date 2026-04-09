package com.example.eventticketingsystem.service;

import com.example.eventticketingsystem.dto.common.PagedResponse;
import com.example.eventticketingsystem.dto.event.request.EventCreateRequest;
import com.example.eventticketingsystem.dto.event.request.EventStatusUpdateRequest;
import com.example.eventticketingsystem.dto.event.response.EventResponse;
import com.example.eventticketingsystem.entity.Event;
import com.example.eventticketingsystem.entity.enums.BookingStatus;
import com.example.eventticketingsystem.entity.enums.EventStatus;
import com.example.eventticketingsystem.exception.ConflictException;
import com.example.eventticketingsystem.exception.ResourceNotFoundException;
import com.example.eventticketingsystem.repository.BookingRepository;
import com.example.eventticketingsystem.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private EventServiceImpl eventService;

    @Test
    void createEvent_setsInactiveStatusAndPersists() {
        EventCreateRequest request = new EventCreateRequest();
        request.setName("Concert");
        request.setDescription("Live show");
        request.setVenue("Arena");
        request.setEventDateTime(Instant.now().plus(2, ChronoUnit.DAYS));

        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            ReflectionTestUtils.setField(e, "id", 42L);
            return e;
        });

        EventResponse response = eventService.createEvent(request);

        assertEquals(EventStatus.INACTIVE, response.getStatus());
        assertEquals("Concert", response.getName());
        assertEquals(42L, response.getId());
    }

    @Test
    void listActiveEvents_clampsLimitAndOffset_andUsesSortFallback() {
        Event event = new Event();
        event.setName("Event1");
        event.setVenue("Venue1");
        event.setEventDateTime(Instant.now().plus(5, ChronoUnit.DAYS));
        event.setStatus(EventStatus.ACTIVE);
        ReflectionTestUtils.setField(event, "id", 1L);

        when(eventRepository.findByStatus(eq(EventStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event)));

        PagedResponse<EventResponse> response = eventService.listActiveEvents(500, -9, "unknown,desc");

        assertEquals(100, response.getLimit());
        assertEquals(0, response.getOffset());
        assertEquals(1, response.getItems().size());
        verify(eventRepository).findByStatus(eq(EventStatus.ACTIVE), any(Pageable.class));
    }

    @Test
    void updateEventStatus_throwsConflictWhenAlreadyActive() {
        Event event = new Event();
        event.setName("Already Active");
        event.setStatus(EventStatus.ACTIVE);
        ReflectionTestUtils.setField(event, "id", 10L);

        EventStatusUpdateRequest request = new EventStatusUpdateRequest();
        request.setStatus(EventStatus.ACTIVE);

        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

        ConflictException ex = assertThrows(
                ConflictException.class,
                () -> eventService.updateEventStatus(10L, request)
        );

        assertEquals("EventAlreadyActive", ex.getErrorCode());
    }

    @Test
    void deleteEvent_throwsConflictWhenConfirmedBookingsExist() {
        Event event = new Event();
        event.setName("Booked Event");
        ReflectionTestUtils.setField(event, "id", 20L);

        when(eventRepository.findById(20L)).thenReturn(Optional.of(event));
        when(bookingRepository.existsByEvent_IdAndStatus(20L, BookingStatus.CONFIRMED)).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> eventService.deleteEvent(20L));

        assertEquals("EventHasBookings", ex.getErrorCode());
        verify(eventRepository, never()).delete(any(Event.class));
    }

    @Test
    void getEventById_throwsNotFoundForInactiveEvent() {
        Event event = new Event();
        event.setStatus(EventStatus.INACTIVE);
        ReflectionTestUtils.setField(event, "id", 33L);

        when(eventRepository.findById(33L)).thenReturn(Optional.of(event));

        assertThrows(ResourceNotFoundException.class, () -> eventService.getEventById(33L));
    }
}
