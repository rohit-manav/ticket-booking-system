package com.example.eventticketingsystem.controller;

import com.example.eventticketingsystem.dto.common.PagedResponse;
import com.example.eventticketingsystem.dto.user.UserResponse;
import com.example.eventticketingsystem.dto.user.UserUpdateRequest;
import com.example.eventticketingsystem.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public PagedResponse<UserResponse> listUsers(
            @RequestParam(defaultValue = "25") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return userService.listUsers(limit, offset);
    }

    @GetMapping("/{userId}")
    public UserResponse getUserById(@PathVariable Long userId) {
        return userService.getUserById(userId);
    }

    @PutMapping("/{userId}")
    public UserResponse updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequest request) {
        return userService.updateUser(userId, request.getName(), request.getEmail());
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
