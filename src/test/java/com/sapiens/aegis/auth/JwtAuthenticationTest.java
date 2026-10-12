package com.sapiens.aegis.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * End-to-end checks of M13: real tokens through the real filter chain.
 * Every "bad token" case must look identical to a missing one: 401, our ApiError shape.
 */
@SpringBootTest
@AutoConfigureMockMvc
class JwtAuthenticationTest {

    private static final String DOCUMENTS = "/api/v1/documents";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void noTokenReturns401InApiErrorShape() throws Exception {
        mockMvc.perform(get(DOCUMENTS))
            .andExpect(status().isUnauthorized())
            .andExpect(header().exists("X-Correlation-Id"))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.error").value("Unauthorized"))
            .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    @Test
    void validTokenIsAccepted() throws Exception {
        mockMvc.perform(get(DOCUMENTS).header("Authorization", "Bearer " + jwtService.generateToken("admin")))
            .andExpect(status().isOk());
    }

    @Test
    void garbageTokenReturns401() throws Exception {
        mockMvc.perform(get(DOCUMENTS).header("Authorization", "Bearer not-a-jwt"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSignedWithAnotherKeyReturns401() throws Exception {
        SecretKey other = Keys.hmacShaKeyFor(Base64.getDecoder()
            .decode("b3RoZXItc2VjcmV0LWtleS1vdGhlci1zZWNyZXQta2V5LTEyMzQ="));
        String forged = Jwts.builder().subject("admin")
            .expiration(new Date(System.currentTimeMillis() + 60_000)).signWith(other).compact();
        mockMvc.perform(get(DOCUMENTS).header("Authorization", "Bearer " + forged))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenReturns401() throws Exception {
        // Signed with the real key but already expired: build via the same key the service uses.
        String expired = Jwts.builder().subject("admin")
            .expiration(new Date(System.currentTimeMillis() - 1_000))
            .signWith(Keys.hmacShaKeyFor(Base64.getDecoder()
                .decode("akj8z7/6sN4ux2FgMnbYFdHGeXa211rqa+kODmiJ/vg=")))
            .compact();
        mockMvc.perform(get(DOCUMENTS).header("Authorization", "Bearer " + expired))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenForUnknownUserReturns401() throws Exception {
        mockMvc.perform(get(DOCUMENTS).header("Authorization", "Bearer " + jwtService.generateToken("ghost")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void nonBearerSchemeReturns401() throws Exception {
        mockMvc.perform(get(DOCUMENTS).header("Authorization", "Basic YWRtaW46dGVtcG9yYXJ5LXBhc3N3b3Jk"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void publicEndpointsNeedNoToken() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
