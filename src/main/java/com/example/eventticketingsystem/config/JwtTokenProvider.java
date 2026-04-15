package com.example.eventticketingsystem.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility to generate, parse, and validate JWT tokens.
 * Used by both AuthServiceImpl (generation) and JwtAuthenticationFilter (validation).
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long jwtExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.expiration:86400000}") long jwtExpiration) {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpiration = jwtExpiration;
    }

    public SecretKey getKey() {
        return key;
    }

    public long getJwtExpiration() {
        return jwtExpiration;
    }

    /**
     * Parse and validate the JWT token. Returns claims if valid.
     *
     * @throws ExpiredJwtException if token is expired
     * @throws JwtException if token is malformed or signature is invalid
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extract user ID from token claims.
     */
    public Long getUserId(Claims claims) {
        return Long.parseLong(claims.getSubject());
    }

    /**
     * Extract roles from token claims.
     */
    @SuppressWarnings("unchecked")
    public Set<String> getRoles(Claims claims) {
        List<String> roles = claims.get("roles", List.class);
        if (roles == null) {
            return Collections.emptySet();
        }
        return roles.stream().collect(Collectors.toSet());
    }
}

