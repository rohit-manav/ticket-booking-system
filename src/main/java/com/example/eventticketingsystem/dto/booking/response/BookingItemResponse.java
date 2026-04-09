package com.example.eventticketingsystem.dto.booking.response;

import java.math.BigDecimal;
import java.time.Instant;

public class BookingItemResponse {

    private Long id;
    private Long seatId;
    private BigDecimal priceAtBooking;
    private Instant createdAt;

    public BookingItemResponse() {}

    public BookingItemResponse(Long id, Long seatId, BigDecimal priceAtBooking, Instant createdAt) {
        this.id = id;
        this.seatId = seatId;
        this.priceAtBooking = priceAtBooking;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSeatId() {
        return seatId;
    }

    public void setSeatId(Long seatId) {
        this.seatId = seatId;
    }

    public BigDecimal getPriceAtBooking() {
        return priceAtBooking;
    }

    public void setPriceAtBooking(BigDecimal priceAtBooking) {
        this.priceAtBooking = priceAtBooking;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

