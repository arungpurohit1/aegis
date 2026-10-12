package com.sapiens.aegis.auth;

import java.util.Date;
import java.util.Base64;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.sapiens.aegis.config.AppProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Signs and parses our own JWTs (HS256, symmetric key). Issuing (sign) is
 * used by AuthController today; parsing (parseUsername) exists for the
 * incoming-request validation filter landing in M13 — building it now keeps
 * the round trip (issue a token, then successfully read it back) provable
 * in this milestone's tests.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(AppProperties properties) {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(properties.getJwt().getSecret()));
        this.expirationMinutes = properties.getJwt().getExpirationMinutes();
    }

    public String generateToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMinutes * 60_000);
        return Jwts.builder()
            .subject(username)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
            .compact();
    }

    /** @throws JwtException if the token is malformed, expired, or the signature doesn't verify. */
    public String parseUsername(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return claims.getSubject();
    }
}
