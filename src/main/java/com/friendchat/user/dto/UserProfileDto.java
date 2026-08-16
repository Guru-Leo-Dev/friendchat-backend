package com.friendchat.user.dto;

import com.friendchat.user.entity.UserStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class UserProfileDto {
    UUID       id;
    String     name;
    String     username;
    String     email;
    String     avatarUrl;
    UserStatus status;
    Instant    lastSeen;
    String     bio;
    int        friendsCount;
    int        mutualFriendsCount;
    String     friendshipStatus;
}
