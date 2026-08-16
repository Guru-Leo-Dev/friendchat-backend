package com.friendchat.friend.dto;

import com.friendchat.user.dto.UserDto;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value @Builder
public class FriendshipDto {
    UUID    id;
    UserDto friend;
    Instant since;
}
