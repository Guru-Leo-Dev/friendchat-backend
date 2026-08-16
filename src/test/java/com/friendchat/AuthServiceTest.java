package com.friendchat;

import com.friendchat.auth.RefreshToken;
import com.friendchat.auth.RefreshTokenRepository;
import com.friendchat.auth.controller.dto.TokenPair;
import com.friendchat.auth.service.AuthService;
import com.friendchat.common.exception.UnauthorizedException;
import com.friendchat.user.entity.User;
import com.friendchat.user.entity.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {

    @Autowired AuthService            authService;
    @Autowired UserRepository         userRepository;
    @Autowired RefreshTokenRepository tokenRepository;

    @Test
    void refresh_withValidToken_returnsNewTokenPair() {
        User user = userRepository.save(User.builder()
                .email("test@example.com").username("testuser").name("Test User").build());

        String raw = "dummyRawToken1234567890123456789012345678901234567890123456789012";
        tokenRepository.save(RefreshToken.builder()
                .user(user).tokenHash(AuthService.sha256(raw))
                .expiresAt(Instant.now().plusSeconds(3600)).build());

        TokenPair pair = authService.refresh(raw);
        assertThat(pair.getAccessToken()).isNotBlank();
        assertThat(pair.getRefreshToken()).isNotBlank().isNotEqualTo(raw);
        assertThat(pair.getExpiresIn()).isGreaterThan(0);
    }

    @Test
    void refresh_withRevokedToken_throwsUnauthorized() {
        User user = userRepository.save(User.builder()
                .email("revoked@example.com").username("revokeduser").name("Revoked").build());

        String raw = "revokedToken12345678901234567890123456789012345678901234567890123";
        tokenRepository.save(RefreshToken.builder()
                .user(user).tokenHash(AuthService.sha256(raw))
                .expiresAt(Instant.now().plusSeconds(3600)).revoked(true).build());

        assertThatThrownBy(() -> authService.refresh(raw))
                .isInstanceOf(UnauthorizedException.class);
    }
}
