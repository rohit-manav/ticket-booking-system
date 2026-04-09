package com.example.eventticketingsystem.dto.seat.response;

import java.util.List;

public class SeatListResponse {
    private List<SeatResponse> items;
    private long totalCount;

    public SeatListResponse(List<SeatResponse> items) {
        this.items = items;
        this.totalCount = items.size();
    }

    public List<SeatResponse> getItems() {
        return items;
    }

    public long getTotalCount() {
        return totalCount;
    }
}

