package com.friendchat.scheduler;

import com.friendchat.auth.RefreshTokenRepository;
import com.friendchat.message.entity.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class MaintenanceScheduler {

    private final MessageRepository      messageRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    /** Hard-delete expired messages every 30 minutes. */
    @Scheduled(cron = "0 */30 * * * *")
    @Transactional
    public void deleteExpiredMessages() {
        try {
            int count = messageRepository.deleteExpired(Instant.now());
            if (count > 0) log.info("[Scheduler] Deleted {} expired message(s)", count);
        } catch (Exception e) {
            log.error("[Scheduler] Failed to delete expired messages: {}", e.getMessage(), e);
        }
    }

    /** Purge stale refresh tokens daily at 03:00. */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredRefreshTokens() {
        try {
            int count = refreshTokenRepository.deleteExpiredAndRevoked();
            log.info("[Scheduler] Purged {} stale refresh token(s)", count);
        } catch (Exception e) {
            log.error("[Scheduler] Failed to purge refresh tokens: {}", e.getMessage(), e);
        }
    }
}
