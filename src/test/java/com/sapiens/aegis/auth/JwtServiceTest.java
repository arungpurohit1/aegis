package com.sapiens.aegis.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.sapiens.aegis.config.AppProperties;

import io.jsonwebtoken.JwtException;

class JwtServiceTest {

    private final AppProperties properties = new AppProperties();
    private final JwtService jwtService = new JwtService(properties);

    @Test
    void generatedTokenParsesBackToTheSameUsername() {
        String token = jwtService.generateToken("admin");

        assertThat(jwtService.parseUsername(token)).isEqualTo("admin");
    }

    @Test
    void garbageTokenFailsToParse() {
        assertThatThrownBy(() -> jwtService.parseUsername("not-a-real-token"))
            .isInstanceOf(JwtException.class);
    }
}
