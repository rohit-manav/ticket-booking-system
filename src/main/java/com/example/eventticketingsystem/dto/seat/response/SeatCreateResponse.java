package com.example.eventticketingsystem.dto.seat.response;

import java.util.List;

public class SeatCreateResponse {
    private List<SeatResponse> items;
    private int created;

    public SeatCreateResponse(List<SeatResponse> items) {
        this.items = items;
        this.created = items.size();
    }

    public List<SeatResponse> getItems() {
        return items;
    }

    public int getCreated() {
        return created;
    }
}

