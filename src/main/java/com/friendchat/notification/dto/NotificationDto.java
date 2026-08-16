package com.friendchat.notification.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class NotificationDto {
    UUID    id;
    String  type;
    String  title;
    String  body;
    String  imageUrl;
    boolean read;
    String  actionUrl;
    Instant createdAt;
}
