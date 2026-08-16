package com.friendchat.friend.service;

import com.friendchat.chat.service.ChatService;
import com.friendchat.common.exception.*;
import com.friendchat.friend.dto.FriendRequestDto;
import com.friendchat.friend.dto.FriendshipDto;
import com.friendchat.friend.entity.*;
import com.friendchat.notification.service.NotificationService;
import com.friendchat.user.dto.UserMapper;
import com.friendchat.user.service.UserService;
import com.friendchat.websocket.event.WsEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
public class FriendService {

    private final FriendRequestRepository  friendRequestRepository;
    private final FriendshipRepository     friendshipRepository;
    private final UserService              userService;
    private final UserMapper               userMapper;
    private final NotificationService      notificationService;
    private final WsEventPublisher         wsPublisher;
    private final ChatService              chatService;

    public FriendService(
            FriendRequestRepository friendRequestRepository,
            FriendshipRepository friendshipRepository,
            UserService userService,
            UserMapper userMapper,
            NotificationService notificationService,
            WsEventPublisher wsPublisher,
            // @Lazy breaks: FriendService -> ChatService -> (FriendService via accept())
            @Lazy ChatService chatService) {
        this.friendRequestRepository = friendRequestRepository;
        this.friendshipRepository    = friendshipRepository;
        this.userService             = userService;
        this.userMapper              = userMapper;
        this.notificationService     = notificationService;
        this.wsPublisher             = wsPublisher;
        this.chatService             = chatService;
    }

    @Transactional(readOnly = true)
    public List<FriendshipDto> getFriends(UUID userId) {
        return friendshipRepository.findAllByUserId(userId).stream()
                .map(f -> FriendshipDto.builder()
                        .id(f.getId())
                        .friend(userMapper.toDto(f.getFriend(userId)))
                        .since(f.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendRequestDto> getRequests(UUID userId) {
        List<FriendRequest> all = new ArrayList<>(
                friendRequestRepository.findByReceiverIdAndStatus(userId, FriendRequestStatus.pending));
        all.addAll(friendRequestRepository.findBySenderIdAndStatus(userId, FriendRequestStatus.pending));
        return all.stream().map(this::toDto).toList();
    }

    @Transactional
    public FriendRequestDto sendRequest(UUID senderId, UUID receiverId) {
        if (senderId.equals(receiverId))
            throw new BadRequestException("Cannot send a friend request to yourself");

        var sender   = userService.findByIdOrThrow(senderId);
        var receiver = userService.findByIdOrThrow(receiverId);

        if (friendshipRepository.areFriends(senderId, receiverId))
            throw new ConflictException("Already friends");
        if (friendRequestRepository.existsBySenderIdAndReceiverId(senderId, receiverId))
            throw new ConflictException("Friend request already sent");

        var request = friendRequestRepository.save(
                FriendRequest.builder().sender(sender).receiver(receiver).build());

        wsPublisher.publishFriendRequest(receiverId, toDto(request));
        notificationService.createFriendRequestNotification(receiver, sender);
        log.info("Friend request sent: {} -> {}", senderId, receiverId);
        return toDto(request);
    }

    @Transactional
    public void accept(UUID requestId, UUID currentUserId) {
        FriendRequest request = findRequestOrThrow(requestId);
        validateReceiver(request, currentUserId);

        request.setStatus(FriendRequestStatus.accepted);
        friendRequestRepository.save(request);

        // canonical ordering: smaller UUID first
        UUID a = request.getSender().getId();
        UUID b = request.getReceiver().getId();
        if (a.compareTo(b) > 0) { UUID tmp = a; a = b; b = tmp; }

        boolean aIsSender = request.getSender().getId().equals(a);
        friendshipRepository.save(Friendship.builder()
                .userA(aIsSender ? request.getSender() : request.getReceiver())
                .userB(aIsSender ? request.getReceiver() : request.getSender())
                .build());

        chatService.findOrCreateDirectChat(
                request.getSender().getId(), request.getReceiver().getId());
        notificationService.createFriendAcceptedNotification(
                request.getSender(), request.getReceiver());
        log.info("Friend request {} accepted", requestId);
    }

    @Transactional
    public void reject(UUID requestId, UUID currentUserId) {
        FriendRequest request = findRequestOrThrow(requestId);
        validateReceiver(request, currentUserId);
        request.setStatus(FriendRequestStatus.rejected);
        friendRequestRepository.save(request);
    }

    @Transactional
    public void removeFriend(UUID userId, UUID friendId) {
        if (!friendshipRepository.areFriends(userId, friendId))
            throw new ResourceNotFoundException("Friendship not found");
        friendshipRepository.deleteFriendship(userId, friendId);
        log.info("Friendship removed between {} and {}", userId, friendId);
    }

    @Transactional
    public void blockUser(UUID blockerId, UUID blockedId) {
        userService.findByIdOrThrow(blockedId);
        if (friendshipRepository.areFriends(blockerId, blockedId))
            friendshipRepository.deleteFriendship(blockerId, blockedId);
        log.info("User {} blocked {}", blockerId, blockedId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private FriendRequest findRequestOrThrow(UUID id) {
        return friendRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request not found"));
    }

    private void validateReceiver(FriendRequest request, UUID currentUserId) {
        if (!request.getReceiver().getId().equals(currentUserId))
            throw new ForbiddenException("Not authorized to act on this request");
        if (request.getStatus() != FriendRequestStatus.pending)
            throw new ConflictException("Request already " + request.getStatus());
    }

    private FriendRequestDto toDto(FriendRequest req) {
        return FriendRequestDto.builder()
                .id(req.getId())
                .sender(userMapper.toDto(req.getSender()))
                .receiver(userMapper.toDto(req.getReceiver()))
                .status(req.getStatus().name())
                .createdAt(req.getCreatedAt())
                .build();
    }
}
