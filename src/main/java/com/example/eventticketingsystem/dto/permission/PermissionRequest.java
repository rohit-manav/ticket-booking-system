package com.example.eventticketingsystem.dto.permission;

import jakarta.validation.constraints.NotBlank;

public class PermissionRequest {

    @NotBlank(message = "The 'name' field is required.")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

