package com.example.eventticketingsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration.
 *
 * HTTP-level rules: all requests are permitted for now — JWT filter will be wired here
 * once the auth module is implemented.
 *
 * Method-level security is active via @EnableMethodSecurity:
 *   @PreAuthorize("hasRole('ADMIN')")            — admin-only operations
 *   @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER')") — shared operations
 *   @PreAuthorize("hasRole('CUSTOMER')")         — customer-only operations
 *
 * When a new role (e.g. MODERATOR) needs access to an endpoint, simply update
 * the @PreAuthorize expression on the relevant method — no new controller or URL needed.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // enables @PreAuthorize / @PostAuthorize
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth ->
                // TODO: Replace with JWT filter and restrict accordingly once auth is implemented.
                // For now every request is permitted so development can continue.
                auth.anyRequest().permitAll()
            );
        return http.build();
    }
}

