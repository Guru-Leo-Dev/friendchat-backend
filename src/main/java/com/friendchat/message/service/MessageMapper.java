package com.friendchat.message.service;

import com.friendchat.message.dto.MessageDto;
import com.friendchat.message.dto.ReactionGroupDto;
import com.friendchat.message.entity.Message;
import com.friendchat.message.entity.MessageReaction;
import com.friendchat.user.dto.UserDto;
import com.friendchat.user.dto.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MessageMapper {

    private final UserMapper userMapper;

    public MessageDto toDto(Message message) {
        return MessageDto.builder()
                .id(message.getId())
                .chatRoomId(message.getChatRoom().getId())
                .sender(userMapper.toDto(message.getSender()))
                .content(message.getDeletedAt() != null ? "" : message.getContent())
                .type(message.getType())
                .status(message.getStatus())
                .reactions(groupReactions(message.getReactions()))
                .replyTo(message.getReplyTo() != null ? toDtoShallow(message.getReplyTo()) : null)
                .fileUrl(message.getFileUrl())
                .fileName(message.getFileName())
                .fileSize(message.getFileSize())
                .expiresAt(message.getExpiresAt())
                .createdAt(message.getCreatedAt())
                .editedAt(message.getEditedAt())
                .deletedAt(message.getDeletedAt())
                .build();
    }

    private MessageDto toDtoShallow(Message message) {
        return MessageDto.builder()
                .id(message.getId())
                .chatRoomId(message.getChatRoom().getId())
                .sender(userMapper.toDto(message.getSender()))
                .content(message.getContent())
                .type(message.getType())
                .status(message.getStatus())
                .reactions(List.of())
                .expiresAt(message.getExpiresAt())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private List<ReactionGroupDto> groupReactions(List<MessageReaction> reactions) {
        if (reactions == null || reactions.isEmpty()) return List.of();
        return reactions.stream()
                .collect(Collectors.groupingBy(MessageReaction::getEmoji))
                .entrySet().stream()
                .map(e -> {
                    List<UserDto> users = e.getValue().stream()
                            .map(r -> userMapper.toDto(r.getUser()))
                            .toList();
                    return ReactionGroupDto.builder()
                            .emoji(e.getKey()).users(users).count(users.size()).build();
                })
                .toList();
    }
}
