package com.example.eventticketingsystem.service;

import com.example.eventticketingsystem.config.JwtTokenProvider;
import com.example.eventticketingsystem.dto.auth.LoginRequest;
import com.example.eventticketingsystem.dto.auth.LoginResponse;
import com.example.eventticketingsystem.dto.auth.RefreshRequest;
import com.example.eventticketingsystem.dto.auth.RegisterRequest;
import com.example.eventticketingsystem.entity.Permission;
import com.example.eventticketingsystem.entity.Role;
import com.example.eventticketingsystem.entity.User;
import com.example.eventticketingsystem.exception.DuplicateEmailException;
import com.example.eventticketingsystem.exception.InvalidCredentialsException;
import com.example.eventticketingsystem.repository.RoleRepository;
import com.example.eventticketingsystem.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthServiceImplementation implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final MessageSource messageSource;
    private final Environment environment;

    public AuthServiceImplementation(UserRepository userRepository,
                                     RoleRepository roleRepository,
                                     PasswordEncoder passwordEncoder,
                                     JwtTokenProvider jwtTokenProvider,
                                     MessageSource messageSource,
                                     Environment environment) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.messageSource = messageSource;
        this.environment = environment;
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            String message = messageSource.getMessage(
                    "error.user.duplicateEmail",
                    new Object[]{request.getEmail()},
                    LocaleContextHolder.getLocale());
            throw new DuplicateEmailException(message);
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        String customerRoleName = environment.getProperty("auth.default-customer-role", "CUSTOMER");
        Role customerRole = roleRepository.findByName(customerRoleName)
                .orElseThrow(() -> new RuntimeException(messageSource.getMessage(
                        "error.role.customerNotFound",
                        new Object[]{customerRoleName},
                        LocaleContextHolder.getLocale())));
        user.setRoles(new HashSet<>(Set.of(customerRole)));

        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String invalidCredentialsMessage = messageSource.getMessage(
                "error.auth.invalidCredentials",
                null,
                LocaleContextHolder.getLocale());
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException(invalidCredentialsMessage));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException(invalidCredentialsMessage);
        }

        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        Set<String> scopes = extractScopes(user);
        String scope = String.join(" ", scopes);

        String accessToken = generateAccessToken(user.getId(), roleNames, scope);
        String refreshToken = generateRefreshToken(user.getId(), roleNames, scope);
        long expiresIn = jwtTokenProvider.getJwtExpiration() / 1000; // in seconds

        return new LoginResponse(accessToken, refreshToken, "Bearer", expiresIn, scope);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse refresh(RefreshRequest request) {
        try {
            Claims claims = jwtTokenProvider.parseToken(request.getRefreshToken());
            Long userId = jwtTokenProvider.getUserId(claims);
            Set<String> roles = jwtTokenProvider.getRoles(claims);
            String scope = jwtTokenProvider.getScope(claims);

            if (scope == null || scope.isBlank()) {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));
                scope = String.join(" ", extractScopes(user));
            }

            String newAccessToken = generateAccessToken(userId, roles, scope);
            String newRefreshToken = generateRefreshToken(userId, roles, scope);
            long expiresIn = jwtTokenProvider.getJwtExpiration() / 1000;

            return new LoginResponse(newAccessToken, newRefreshToken, "Bearer", expiresIn, scope);
        } catch (Exception e) {
            throw new InvalidCredentialsException("Invalid refresh token");
        }
    }

    private String generateAccessToken(Long userId, Set<String> roles, String scope) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtTokenProvider.getJwtExpiration());

        return Jwts.builder()
                .subject(userId.toString())
                .claim("roles", roles)
                .claim("scope", scope)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(jwtTokenProvider.getKey())
                .compact();
    }

    private String generateRefreshToken(Long userId, Set<String> roles, String scope) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtTokenProvider.getJwtRefreshExpiration());

        return Jwts.builder()
                .subject(userId.toString())
                .claim("roles", roles)
                .claim("scope", scope)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(jwtTokenProvider.getKey())
                .compact();
    }

    private Set<String> extractScopes(User user) {
        return user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .sorted(Comparator.comparing(Permission::getId))
                .map(Permission::getName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}