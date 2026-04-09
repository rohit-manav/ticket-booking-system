package com.example.eventticketingsystem.service;

import com.example.eventticketingsystem.dto.common.PagedResponse;
import com.example.eventticketingsystem.dto.event.request.EventCreateRequest;
import com.example.eventticketingsystem.dto.event.request.EventStatusUpdateRequest;
import com.example.eventticketingsystem.dto.event.request.EventUpdateRequest;
import com.example.eventticketingsystem.dto.event.response.EventResponse;

public interface EventService {
    EventResponse createEvent(EventCreateRequest request);

    EventResponse updateEvent(Long eventId, EventUpdateRequest request);

    void deleteEvent(Long eventId);

    EventResponse updateEventStatus(Long eventId, EventStatusUpdateRequest request);

    PagedResponse<EventResponse> listActiveEvents(int limit, int offset, String sort);

    EventResponse getEventById(Long eventId);
}
