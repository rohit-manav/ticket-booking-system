package com.example.eventticketingsystem.service;

import com.example.eventticketingsystem.dto.auth.LoginRequest;
import com.example.eventticketingsystem.dto.auth.LoginResponse;
import com.example.eventticketingsystem.dto.auth.RegisterRequest;

public interface AuthService {

    void register(RegisterRequest request);

    LoginResponse login(LoginRequest request);
}
