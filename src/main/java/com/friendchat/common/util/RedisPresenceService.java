package com.friendchat.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
public class RedisPresenceService {

    private static final String   ONLINE_KEY    = "presence:online:";
    private static final String   LAST_SEEN_KEY = "presence:lastseen:";
    private static final Duration ONLINE_TTL    = Duration.ofSeconds(90);

    private final RedisTemplate<String, String> redis;

    public RedisPresenceService(
            @Qualifier("presenceRedisTemplate") RedisTemplate<String, String> redis) {
        this.redis = redis;
    }

    public void setOnline(UUID userId) {
        redis.opsForValue().set(ONLINE_KEY + userId, "1", ONLINE_TTL);
        log.debug("User {} online", userId);
    }

    public void heartbeat(UUID userId) {
        String key = ONLINE_KEY + userId;
        if (Boolean.TRUE.equals(redis.hasKey(key))) redis.expire(key, ONLINE_TTL);
        else redis.opsForValue().set(key, "1", ONLINE_TTL);
    }

    public void setOffline(UUID userId) {
        redis.delete(ONLINE_KEY + userId);
        redis.opsForValue().set(LAST_SEEN_KEY + userId, Instant.now().toString(), Duration.ofDays(7));
        log.debug("User {} offline", userId);
    }

    public boolean isOnline(UUID userId) {
        return Boolean.TRUE.equals(redis.hasKey(ONLINE_KEY + userId));
    }

    public String getLastSeen(UUID userId) {
        return redis.opsForValue().get(LAST_SEEN_KEY + userId);
    }
}
