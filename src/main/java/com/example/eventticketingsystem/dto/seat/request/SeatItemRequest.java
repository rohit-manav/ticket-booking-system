package com.example.eventticketingsystem.dto.seat.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class SeatItemRequest {

    @NotBlank(message = "The 'seatNumber' field is required.")
    private String seatNumber;

    @NotBlank(message = "The 'category' field is required.")
    private String category;

    @NotNull(message = "The 'price' field is required.")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0.")
    private BigDecimal price;

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}

