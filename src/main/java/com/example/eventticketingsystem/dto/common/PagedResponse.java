package com.example.eventticketingsystem.dto.common;

import java.util.List;

public class PagedResponse<T> {
    private List<T> items;
    private int limit;
    private int offset;
    private long totalCount;

    public PagedResponse(List<T> items, int limit, int offset, long totalCount) {
        this.items = items;
        this.limit = limit;
        this.offset = offset;
        this.totalCount = totalCount;
    }

    public List<T> getItems() {
        return items;
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    public long getTotalCount() {
        return totalCount;
    }
}
