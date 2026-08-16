package com.friendchat.chat.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.friendchat.chat.dto.ChatMemberDto;
import com.friendchat.chat.dto.ChatRoomDto;
import com.friendchat.chat.entity.ChatRoom;
import com.friendchat.chat.entity.ChatRoomMember;
import com.friendchat.chat.entity.ChatRoomMemberRepository;
import com.friendchat.chat.entity.ChatRoomRepository;
import com.friendchat.chat.entity.ChatType;
import com.friendchat.chat.entity.MemberRole;
import com.friendchat.common.exception.BadRequestException;
import com.friendchat.common.exception.ForbiddenException;
import com.friendchat.common.exception.ResourceNotFoundException;
import com.friendchat.message.dto.MessageDto;
import com.friendchat.message.entity.MessageRepository;
import com.friendchat.message.service.MessageMapper;
import com.friendchat.user.dto.UserDto;
import com.friendchat.user.entity.User;
import com.friendchat.user.service.UserService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ChatService {

    private final ChatRoomRepository       chatRoomRepository;
    private final ChatRoomMemberRepository memberRepository;
    private final MessageRepository        messageRepository;
    private final UserService              userService;
    private final MessageMapper            messageMapper;

    // @Lazy breaks the potential circular: FriendService -> ChatService -> MessageMapper -> ChatService
    public ChatService(
            ChatRoomRepository chatRoomRepository,
            ChatRoomMemberRepository memberRepository,
            MessageRepository messageRepository,
            @Lazy UserService userService,
            MessageMapper messageMapper) {
        this.chatRoomRepository = chatRoomRepository;
        this.memberRepository   = memberRepository;
        this.messageRepository  = messageRepository;
        this.userService        = userService;
        this.messageMapper      = messageMapper;
    }

    @Transactional(readOnly = true)
    public List<ChatRoomDto> getChatsForUser(UUID userId) {
        return chatRoomRepository.findAllByMemberUserId(userId).stream()
                .map(c -> toChatRoomDto(c, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatRoomDto getChat(UUID chatId, UUID userId) {
        assertMember(chatId, userId);
        return toChatRoomDto(findChatOrThrow(chatId), userId);
    }

    @Transactional
    public ChatRoom findOrCreateDirectChat(UUID userAId, UUID userBId) {
        return chatRoomRepository.findDirectChat(userAId, userBId).orElseGet(() -> {
            User userA = userService.findByIdOrThrow(userAId);
            User userB = userService.findByIdOrThrow(userBId);
            ChatRoom chat = chatRoomRepository.save(
                    ChatRoom.builder().type(ChatType.direct).createdBy(userA).build());
            memberRepository.saveAll(List.of(
                    ChatRoomMember.builder().chatRoom(chat).user(userA).role(MemberRole.admin).build(),
                    ChatRoomMember.builder().chatRoom(chat).user(userB).role(MemberRole.member).build()));
            log.info("Direct chat created between {} and {}", userAId, userBId);
            return chat;
        });
    }

    @Transactional
    public ChatRoomDto createDirectChat(UUID currentUserId, UUID otherUserId) {
        return toChatRoomDto(findOrCreateDirectChat(currentUserId, otherUserId), currentUserId);
    }

    @Transactional
    public ChatRoomDto createGroupChat(UUID creatorId, String name, List<UUID> memberIds) {
        User creator = userService.findByIdOrThrow(creatorId);
        ChatRoom chat = chatRoomRepository.save(
                ChatRoom.builder().type(ChatType.group).name(name).createdBy(creator).build());
        List<ChatRoomMember> members = new ArrayList<>();
        members.add(ChatRoomMember.builder().chatRoom(chat).user(creator).role(MemberRole.admin).build());
        for (UUID id : memberIds) {
            if (!id.equals(creatorId))
                members.add(ChatRoomMember.builder().chatRoom(chat)
                        .user(userService.findByIdOrThrow(id)).role(MemberRole.member).build());
        }
        memberRepository.saveAll(members);
        log.info("Group '{}' created by {}", name, creatorId);
        return toChatRoomDto(chat, creatorId);
    }

    @Transactional
    public void markRead(UUID chatId, UUID userId) {
        assertMember(chatId, userId);
        memberRepository.markRead(chatId, userId, Instant.now());
        messageRepository.markAllReadInChat(chatId, userId);
    }

    @Transactional
    public void leaveGroup(UUID chatId, UUID userId) {
        ChatRoom chat = findChatOrThrow(chatId);
        if (chat.getType() != ChatType.group) throw new BadRequestException("Cannot leave a direct chat");
        memberRepository.deleteByChatRoomIdAndUserId(chatId, userId);
    }

    @Transactional
    public void touchLastActivity(UUID chatId) {
        chatRoomRepository.findById(chatId).ifPresent(c -> {
            c.setLastActivity(Instant.now());
            chatRoomRepository.save(c);
        });
    }

    public ChatRoom findChatOrThrow(UUID chatId) {
        return chatRoomRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat not found: " + chatId));
    }

    public void assertMember(UUID chatId, UUID userId) {
        if (chatRoomRepository.countMembership(chatId, userId) == 0)
            throw new ForbiddenException("Not a member of this chat");
    }

    // ── Private mappers ───────────────────────────────────────────────────────

    private ChatRoomDto toChatRoomDto(ChatRoom chat, UUID currentUserId) {
        List<ChatMemberDto> memberDtos = chat.getMembers().stream()
                .map(m -> ChatMemberDto.builder()
                        .user(toUserDto(m.getUser()))
                        .role(m.getRole().name())
                        .joinedAt(m.getJoinedAt())
                        .build())
                .collect(Collectors.toList());

        MessageDto lastMsg = messageRepository
                .findLastByChatRoomId(chat.getId())
                .map(messageMapper::toDto)
                .orElse(null);

        long unread = messageRepository.countUnread(chat.getId(), currentUserId);

        return ChatRoomDto.builder()
                .id(chat.getId())
                .type(chat.getType().name())
                .name(chat.getName())
                .avatarUrl(chat.getAvatarUrl())
                .members(memberDtos)
                .lastMessage(lastMsg)
                .lastActivity(chat.getLastActivity())
                .unreadCount((int) unread)
                .createdAt(chat.getCreatedAt())
                .build();
    }

    private UserDto toUserDto(User user) {
        return UserDto.builder()
                .id(user.getId()).name(user.getName())
                .username(user.getUsername()).email(user.getEmail())
                .avatarUrl(user.getAvatarUrl()).status(user.getStatus())
                .lastSeen(user.getLastSeen()).bio(user.getBio())
                .build();
    }
}
