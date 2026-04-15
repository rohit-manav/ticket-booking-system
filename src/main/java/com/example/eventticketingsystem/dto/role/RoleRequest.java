package com.example.eventticketingsystem.dto.role;

import jakarta.validation.constraints.NotBlank;

public class RoleRequest {

    @NotBlank(message = "The 'name' field is required.")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

