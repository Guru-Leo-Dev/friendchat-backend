package com.friendchat.friend.dto;

import com.friendchat.user.dto.UserDto;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value @Builder
public class FriendRequestDto {
    UUID    id;
    UserDto sender;
    UserDto receiver;
    String  status;
    Instant createdAt;
}
