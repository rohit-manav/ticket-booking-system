package com.example.eventticketingsystem.dto.role;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class AssignPermissionsRequest {

    @NotNull(message = "The 'permissionIds' field is required.")
    @NotEmpty(message = "At least one permission ID is required.")
    private List<Long> permissionIds;

    public List<Long> getPermissionIds() {
        return permissionIds;
    }

    public void setPermissionIds(List<Long> permissionIds) {
        this.permissionIds = permissionIds;
    }
}

