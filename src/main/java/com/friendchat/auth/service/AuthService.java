package com.friendchat.auth.service;

import com.friendchat.auth.RefreshToken;
import com.friendchat.auth.RefreshTokenRepository;
import com.friendchat.auth.controller.dto.AuthResponse;
import com.friendchat.auth.controller.dto.TokenPair;
import com.friendchat.common.exception.UnauthorizedException;
import com.friendchat.security.JwtService;
import com.friendchat.user.dto.UserMapper;
import com.friendchat.user.entity.*;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository         userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService             jwtService;
    private final UserMapper             userMapper;

    @Value("${app.google.client-id}")
    private String googleClientId;

    @Transactional
    public AuthResponse googleLogin(String idToken, String deviceInfo) {
        GoogleIdToken.Payload payload = verifyGoogleToken(idToken);
        String googleId = payload.getSubject();
        String email    = payload.getEmail();
        String name     = (String) payload.get("name");
        String picture  = (String) payload.get("picture");

        User user = userRepository.findByGoogleId(googleId)
                .orElseGet(() -> userRepository.findByEmail(email)
                        .map(existing -> {
                            existing.setGoogleId(googleId);
                            if (existing.getAvatarUrl() == null) existing.setAvatarUrl(picture);
                            return existing;
                        })
                        .orElseGet(() -> createUser(googleId, email, name, picture)));

        user.setStatus(UserStatus.online);
        user = userRepository.save(user);

        TokenPair tokens = issueTokens(user, deviceInfo);
        log.info("Google login successful for user {}", user.getId());
        return new AuthResponse(userMapper.toDto(user), tokens);
    }

    @Transactional
    public TokenPair refresh(String rawRefreshToken) {
        String hash = sha256(rawRefreshToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        if (!token.isValid()) {
            refreshTokenRepository.revokeAllByUserId(token.getUser().getId());
            throw new UnauthorizedException("Refresh token expired or revoked");
        }
        token.setRevoked(true);
        refreshTokenRepository.save(token);
        return issueTokens(token.getUser(), null);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        String hash = sha256(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(t -> {
            t.setRevoked(true);
            refreshTokenRepository.save(t);
        });
    }

    @Transactional
    public void logoutAll(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        log.info("All sessions revoked for user {}", userId);
    }

    private GoogleIdToken.Payload verifyGoogleToken(String idToken) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) throw new UnauthorizedException("Invalid Google ID token");
            return token.getPayload();
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new UnauthorizedException("Google token verification failed: " + e.getMessage());
        }
    }

    private User createUser(String googleId, String email, String name, String picture) {
        String username = generateUsername(email);
        return userRepository.save(User.builder()
                .googleId(googleId).email(email)
                .name(name != null ? name : email.split("@")[0])
                .username(username).avatarUrl(picture)
                .status(UserStatus.online).build());
    }

    private String generateUsername(String email) {
        String base = email.split("@")[0].replaceAll("[^a-z0-9_]", "").toLowerCase();
        if (base.length() < 3) base = base + "user";
        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) candidate = base + suffix++;
        return candidate;
    }

    private TokenPair issueTokens(User user, String deviceInfo) {
        String rawRefresh  = jwtService.generateOpaqueRefreshToken();
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user).tokenHash(sha256(rawRefresh)).deviceInfo(deviceInfo)
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTokenSeconds()))
                .build());
        return new TokenPair(accessToken, rawRefresh, jwtService.getAccessTokenExpiresIn());
    }

    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 failed", e);
        }
    }
}
