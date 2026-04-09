package com.example.eventticketingsystem.dto.event.request;

import com.example.eventticketingsystem.entity.enums.EventStatus;
import jakarta.validation.constraints.NotNull;

public class EventStatusUpdateRequest {

    @NotNull
    private EventStatus status;

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }
}

