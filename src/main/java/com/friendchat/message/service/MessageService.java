package com.friendchat.message.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.friendchat.chat.service.ChatService;
import com.friendchat.common.dto.PageResponse;
import com.friendchat.common.exception.ForbiddenException;
import com.friendchat.common.exception.ResourceNotFoundException;
import com.friendchat.common.util.S3Service;
import com.friendchat.message.dto.MessageDto;
import com.friendchat.message.entity.Message;
import com.friendchat.message.entity.MessageReaction;
import com.friendchat.message.entity.MessageReactionRepository;
import com.friendchat.message.entity.MessageRepository;
import com.friendchat.message.entity.MessageStatus;
import com.friendchat.message.entity.MessageType;
import com.friendchat.user.entity.User;
import com.friendchat.user.service.UserService;
import com.friendchat.websocket.event.WsEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository         messageRepository;
    private final MessageReactionRepository reactionRepository;
    private final ChatService               chatService;
    private final UserService               userService;
    private final MessageMapper             messageMapper;
    private final WsEventPublisher          wsPublisher;
    private final S3Service                 s3Service;

    @Value("${app.message.ttl-hours:24}")
    private int messageTtlHours;

    @Transactional(readOnly = true)
    public PageResponse<MessageDto> getMessages(UUID chatId, UUID userId, int page, int size) {
        chatService.assertMember(chatId, userId);
        Page<Message> msgs = messageRepository.findByChatRoomId(chatId, PageRequest.of(page, size));
        var dtos = msgs.getContent().stream()
                .sorted(Comparator.comparing(Message::getCreatedAt))
                .map(messageMapper::toDto)
                .toList();
        return PageResponse.<MessageDto>builder()
                .data(dtos).page(page).size(size)
                .total(msgs.getTotalElements()).hasMore(msgs.hasNext())
                .build();
    }

    @Transactional
    public MessageDto send(UUID chatId, UUID senderId, String content, UUID replyToId) {
        chatService.assertMember(chatId, senderId);
        User sender = userService.findByIdOrThrow(senderId);
        Message replyTo = null;
        if (replyToId != null)
            replyTo = messageRepository.findById(replyToId)
                    .orElseThrow(() -> new ResourceNotFoundException("Reply message not found"));

        Message message = messageRepository.save(Message.builder()
                .chatRoom(chatService.findChatOrThrow(chatId))
                .sender(sender).content(content).type(MessageType.text)
                .status(MessageStatus.sent).replyTo(replyTo)
                .expiresAt(Instant.now().plus(messageTtlHours, ChronoUnit.HOURS))
                .build());

        chatService.touchLastActivity(chatId);
        MessageDto dto = messageMapper.toDto(message);
        wsPublisher.publishNewMessage(chatId, dto, senderId);
        return dto;
    }

    @Transactional
    public MessageDto sendFile(UUID chatId, UUID senderId, MultipartFile file) {
        chatService.assertMember(chatId, senderId);
        User sender = userService.findByIdOrThrow(senderId);
        String fileUrl  = s3Service.uploadMessageFile(chatId, senderId, file);
        String mimeType = file.getContentType();
        MessageType type = (mimeType != null && mimeType.startsWith("image/"))
                ? MessageType.image : MessageType.file;

        Message message = messageRepository.save(Message.builder()
                .chatRoom(chatService.findChatOrThrow(chatId))
                .sender(sender).content("").type(type)
                .fileUrl(fileUrl).fileName(file.getOriginalFilename()).fileSize(file.getSize())
                .expiresAt(Instant.now().plus(messageTtlHours, ChronoUnit.HOURS))
                .build());

        chatService.touchLastActivity(chatId);
        MessageDto dto = messageMapper.toDto(message);
        wsPublisher.publishNewMessage(chatId, dto, senderId);
        return dto;
    }

    @Transactional
    public MessageDto react(UUID messageId, UUID userId, String emoji) {
        Message message = findMessageOrThrow(messageId);
        chatService.assertMember(message.getChatRoom().getId(), userId);
        User user = userService.findByIdOrThrow(userId);

        var existing = reactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, userId, emoji);
        if (existing.isPresent()) reactionRepository.delete(existing.get());
        else reactionRepository.save(
                MessageReaction.builder().message(message).user(user).emoji(emoji).build());

        message = findMessageOrThrow(messageId);
        MessageDto dto = messageMapper.toDto(message);
        wsPublisher.publishReaction(message.getChatRoom().getId(), messageId, dto.getReactions());
        return dto;
    }

    @Transactional
    public void deleteMessage(UUID messageId, UUID userId) {
        Message message = findMessageOrThrow(messageId);
        if (!message.getSender().getId().equals(userId))
            throw new ForbiddenException("Cannot delete another user's message");
        message.setDeletedAt(Instant.now());
        message.setContent("");
        messageRepository.save(message);
        wsPublisher.publishMessageDeleted(message.getChatRoom().getId(), messageId);
    }

    private Message findMessageOrThrow(UUID id) {
        return messageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found: " + id));
    }
}
