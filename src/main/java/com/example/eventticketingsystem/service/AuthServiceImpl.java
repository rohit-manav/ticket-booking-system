package com.example.eventticketingsystem.service;

import com.example.eventticketingsystem.config.JwtTokenProvider;
import com.example.eventticketingsystem.dto.auth.LoginRequest;
import com.example.eventticketingsystem.dto.auth.LoginResponse;
import com.example.eventticketingsystem.dto.auth.RegisterRequest;
import com.example.eventticketingsystem.entity.Role;
import com.example.eventticketingsystem.entity.User;
import com.example.eventticketingsystem.exception.DuplicateEmailException;
import com.example.eventticketingsystem.exception.InvalidCredentialsException;
import com.example.eventticketingsystem.repository.RoleRepository;
import com.example.eventticketingsystem.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(
                    "A user with email '" + request.getEmail() + "' is already registered.");
        }

        // Create new user
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // Auto-assign CUSTOMER role
        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new RuntimeException("CUSTOMER role not found. Run bootstrap script."));
        user.setRoles(new HashSet<>(Set.of(customerRole)));

        // Save user
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password."));

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password.");
        }

        // Extract role names
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        // Generate JWT token
        String token = generateToken(user.getId(), roleNames);

        // Return login response
        long expiresInSeconds = jwtTokenProvider.getJwtExpiration() / 1000;
        return new LoginResponse(token, "Bearer", expiresInSeconds, user.getId(), roleNames);
    }

    /**
     * Generate JWT token using the shared JwtTokenProvider key.
     */
    private String generateToken(Long userId, Set<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtTokenProvider.getJwtExpiration());

        return Jwts.builder()
                .subject(userId.toString())
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(jwtTokenProvider.getKey())
                .compact();
    }
}

