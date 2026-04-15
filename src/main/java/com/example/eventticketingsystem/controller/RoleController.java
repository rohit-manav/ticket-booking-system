package com.example.eventticketingsystem.controller;

import com.example.eventticketingsystem.dto.common.PagedResponse;
import com.example.eventticketingsystem.dto.role.AssignPermissionsRequest;
import com.example.eventticketingsystem.dto.role.RoleRequest;
import com.example.eventticketingsystem.dto.role.RoleResponse;
import com.example.eventticketingsystem.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/roles")
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public PagedResponse<RoleResponse> listRoles(
            @RequestParam(defaultValue = "25") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return roleService.listRoles(limit, offset);
    }

    @PostMapping
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody RoleRequest request) {
        RoleResponse response = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{roleId}")
    public RoleResponse getRoleById(@PathVariable Long roleId) {
        return roleService.getRoleById(roleId);
    }

    @PutMapping("/{roleId}")
    public RoleResponse updateRole(@PathVariable Long roleId, @Valid @RequestBody RoleRequest request) {
        return roleService.updateRole(roleId, request);
    }

    @DeleteMapping("/{roleId}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long roleId) {
        roleService.deleteRole(roleId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{roleId}/permissions")
    public RoleResponse assignPermissions(
            @PathVariable Long roleId,
            @Valid @RequestBody AssignPermissionsRequest request) {
        return roleService.assignPermissions(roleId, request);
    }
}

