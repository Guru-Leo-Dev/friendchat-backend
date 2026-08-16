package com.friendchat.message.dto;

import com.friendchat.message.entity.MessageStatus;
import com.friendchat.message.entity.MessageType;
import com.friendchat.user.dto.UserDto;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class MessageDto {
    UUID                  id;
    UUID                  chatRoomId;
    UserDto               sender;
    String                content;
    MessageType           type;
    MessageStatus         status;
    List<ReactionGroupDto> reactions;
    MessageDto            replyTo;
    String                fileUrl;
    String                fileName;
    Long                  fileSize;
    Instant               expiresAt;
    Instant               createdAt;
    Instant               editedAt;
    Instant               deletedAt;
}
