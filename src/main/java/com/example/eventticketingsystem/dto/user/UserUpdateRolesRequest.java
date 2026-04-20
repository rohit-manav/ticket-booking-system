package com.example.eventticketingsystem.dto.user;

import java.util.Set;

public class UserUpdateRolesRequest {
    private Set<String> roles;

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}

