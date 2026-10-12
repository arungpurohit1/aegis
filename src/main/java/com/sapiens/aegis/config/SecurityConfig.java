package com.sapiens.aegis.config;

import java.time.Instant;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapiens.aegis.auth.JwtAuthenticationFilter;
import com.sapiens.aegis.auth.JwtService;
import com.sapiens.aegis.common.ApiError;
import com.sapiens.aegis.common.CorrelationIdFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/**
 * Foundation for securing the API: default-deny (anyRequest().authenticated())
 * with explicit exceptions, rather than the starter's own default (everything
 * behind a single generated password with a login form).
 *
 * Only health/info and /api/v1/auth/** are public. Everything else, including
 * /api/v1/documents/**, needs a valid JWT, checked by JwtAuthenticationFilter
 * (M13). The in-memory user below is checked at login time by the
 * AuthenticationManager and is itself replaced once real user storage lands.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtService jwtService,
                                                   UserDetailsService userDetailsService,
                                                   AuthenticationEntryPoint unauthorizedEntryPoint)
            throws Exception {
        return http
            // Stateless JSON API: no browser session/cookie to forge against,
            // so CSRF protection (built for cookie-based session auth) doesn't apply.
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(requests -> requests
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/api/v1/auth/**").permitAll()
                .anyRequest().authenticated())
            // Our JWT filter replaces HTTP Basic: every protected call carries a bearer token.
            .addFilterBefore(new JwtAuthenticationFilter(jwtService, userDetailsService),
                UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(handling -> handling.authenticationEntryPoint(unauthorizedEntryPoint))
            .build();
    }

    /**
     * Anonymous request to a protected URL -> 401 in our standard ApiError shape.
     * Filters run before MVC, so ApiExceptionHandler can't see this one; the
     * body is written here. The message is the same for missing, expired and
     * forged tokens so a caller learns nothing about why.
     */
    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint(ObjectMapper json) {
        return (request, response, ex) -> {
            ApiError body = new ApiError(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Authentication required: send a valid 'Authorization: Bearer <token>' header",
                CorrelationIdFilter.currentCorrelationId(),
                null);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            json.writeValue(response.getOutputStream(), body);
        };
    }

    /**
     * Used by AuthController to check a login attempt's username/password
     * against the UserDetailsService below, via the standard
     * DaoAuthenticationProvider (looks up the user, verifies the password
     * with the configured PasswordEncoder).
     */
    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    /**
     * Placeholder credential store so "anyRequest().authenticated()" has
     * something to authenticate against. Hardcoded username/password is
     * only acceptable because this is scaffolding for a real auth mechanism
     * (JWT) arriving in the next milestone, not a design meant to ship.
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
            User.withUsername("admin")
                .password(encoder.encode("temporary-password"))
                .roles("ADMIN")
                .build());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
