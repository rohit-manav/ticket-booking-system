package com.example.eventticketingsystem.dto.booking.response;

import com.example.eventticketingsystem.entity.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class BookingResponse {

    private Long id;
    private Long eventId;
    private Long userId;
    private BookingStatus status;
    private BigDecimal totalAmount;
    private Instant bookingDate;
    private List<BookingItemResponse> items;
    private Instant createdAt;
    private Instant updatedAt;

    public BookingResponse() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Instant getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(Instant bookingDate) {
        this.bookingDate = bookingDate;
    }

    public List<BookingItemResponse> getItems() {
        return items;
    }

    public void setItems(List<BookingItemResponse> items) {
        this.items = items;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

