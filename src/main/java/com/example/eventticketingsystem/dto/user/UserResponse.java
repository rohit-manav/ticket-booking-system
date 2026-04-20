package com.example.eventticketingsystem.dto.user;

import java.util.Set;

public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private Set<String> roles;

    public UserResponse(Long id, String name, String email, Set<String> roles) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.roles = roles;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Set<String> getRoles() {
        return roles;
    }
}

