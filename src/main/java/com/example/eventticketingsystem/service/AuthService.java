package com.example.eventticketingsystem.service;

import com.example.eventticketingsystem.dto.auth.LoginRequest;
import com.example.eventticketingsystem.dto.auth.LoginResponse;
import com.example.eventticketingsystem.dto.auth.RegisterRequest;

public interface AuthService {

    /**
     * Register a new user.
     * Password will be BCrypt-hashed and stored securely.
     * User will be auto-assigned to CUSTOMER role.
     *
     * @param request register request with name, email, password
     * @throws DuplicateEmailException if email already exists
     */
    void register(RegisterRequest request);

    /**
     * Authenticate user and generate JWT token.
     *
     * @param request login request with email and password
     * @return login response with JWT token, user ID, and roles
     * @throws InvalidCredentialsException if email or password is invalid
     */
    LoginResponse login(LoginRequest request);
}

