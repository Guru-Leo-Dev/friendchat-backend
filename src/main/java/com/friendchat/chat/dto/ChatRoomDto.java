package com.friendchat.chat.dto;

import com.friendchat.message.dto.MessageDto;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Value @Builder
public class ChatRoomDto {
    UUID               id;
    String             type;
    String             name;
    String             avatarUrl;
    List<ChatMemberDto> members;
    MessageDto         lastMessage;
    Instant            lastActivity;
    int                unreadCount;
    Instant            createdAt;
}
