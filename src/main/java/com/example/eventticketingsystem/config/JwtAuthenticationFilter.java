package com.example.eventticketingsystem.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * JWT Authentication Filter.
 *
 * Runs once per request before the controller is invoked.
 * Reads the "Authorization: Bearer <token>" header, validates the JWT,
 * and sets the SecurityContext so @PreAuthorize annotations work.
 *
 * Flow:
 *   Request → Extract token from header → Parse & validate JWT
 *   → Extract userId + roles → Build Authentication object
 *   → Set SecurityContext → Continue filter chain
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Extract token from Authorization header
        String token = extractToken(request);

        if (token != null) {
            try {
                // Parse and validate token
                Claims claims = jwtTokenProvider.parseToken(token);

                // Extract user ID and roles
                Long userId = jwtTokenProvider.getUserId(claims);
                Set<String> roles = jwtTokenProvider.getRoles(claims);

                // Convert roles to Spring Security authorities (prefix with ROLE_)
                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList();

                // Build authentication token and set in security context
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (ExpiredJwtException ex) {
                // Token expired — don't set auth, let Spring Security return 401
                SecurityContextHolder.clearContext();
            } catch (JwtException ex) {
                // Invalid token — don't set auth
                SecurityContextHolder.clearContext();
            }
        }

        // Continue to next filter / controller
        filterChain.doFilter(request, response);
    }

    /**
     * Extract Bearer token from the Authorization header.
     * Returns null if header is absent or doesn't start with "Bearer ".
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}

