package com.friendchat.chat.dto;

import com.friendchat.user.dto.UserDto;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value @Builder
public class ChatMemberDto {
    UserDto user;
    String  role;
    Instant joinedAt;
}
