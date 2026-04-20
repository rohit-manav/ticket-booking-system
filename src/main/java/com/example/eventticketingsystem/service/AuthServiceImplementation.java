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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthServiceImplementation implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthServiceImplementation(UserRepository userRepository,
                                     RoleRepository roleRepository,
                                     PasswordEncoder passwordEncoder,
                                     JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(
                    "A user with email '" + request.getEmail() + "' is already registered.");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new RuntimeException("CUSTOMER role not found."));
        user.setRoles(new HashSet<>(Set.of(customerRole)));

        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password.");
        }

        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        String token = generateToken(user.getId(), roleNames);

        return new LoginResponse(token, "Bearer");
    }

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