package com.example.eventticketingsystem.controller;

import com.example.eventticketingsystem.dto.auth.LoginRequest;
import com.example.eventticketingsystem.dto.auth.LoginResponse;
import com.example.eventticketingsystem.dto.auth.RegisterRequest;
import com.example.eventticketingsystem.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * POST /api/v1/auth/register
     * Public endpoint for user registration.
     * Returns 201 Created with no response body on success.
     */
    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * POST /api/v1/auth/login
     * Public endpoint for user authentication.
     * Returns 200 OK with JWT token and user details on success.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}

