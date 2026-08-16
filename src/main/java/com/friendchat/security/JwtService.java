package com.friendchat.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
@Slf4j
public class JwtService {

    private final SecretKey key;
    private final long      accessTokenSeconds;
    private final long      refreshTokenSeconds;

    public JwtService(
            @Value("${app.jwt.secret:friendchat-default-secret-key-please-change-in-production-min-64-chars}")
            String secret,
            @Value("${app.jwt.access-token-expiration:900}")
            long accessTokenSeconds,
            @Value("${app.jwt.refresh-token-expiration:2592000}")
            long refreshTokenSeconds) {
        this.key                 = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenSeconds  = accessTokenSeconds;
        this.refreshTokenSeconds = refreshTokenSeconds;
    }

    public String generateAccessToken(UUID userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenSeconds)))
                .signWith(key)
                .compact();
    }

    public Claims parseAccessToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateAccessToken(String token) {
        try {
            parseAccessToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseAccessToken(token).getSubject());
    }

    public String generateOpaqueRefreshToken() {
        return UUID.randomUUID().toString().replace("-", "")
             + UUID.randomUUID().toString().replace("-", "");
    }

    public long getAccessTokenExpiresIn() { return accessTokenSeconds; }
    public long getRefreshTokenSeconds()  { return refreshTokenSeconds; }
}
