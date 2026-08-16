package com.friendchat.notification.service;

import com.friendchat.common.dto.PageResponse;
import com.friendchat.common.exception.ResourceNotFoundException;
import com.friendchat.notification.dto.NotificationDto;
import com.friendchat.notification.entity.Notification;
import com.friendchat.notification.entity.NotificationRepository;
import com.friendchat.user.entity.User;
import com.friendchat.websocket.event.WsEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final WsEventPublisher       wsPublisher;

    @Transactional(readOnly = true)
    public PageResponse<NotificationDto> getNotifications(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> result = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.of(result.map(this::toDto));
    }

    @Transactional
    public void markRead(UUID notificationId, UUID userId) {
        int updated = notificationRepository.markRead(notificationId, userId);
        if (updated == 0) throw new ResourceNotFoundException("Notification not found");
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.markAllRead(userId);
    }

    @Transactional
    public void createFriendRequestNotification(User receiver, User sender) {
        Notification n = save(receiver,
                "friend_request",
                "New friend request",
                sender.getName() + " sent you a friend request",
                sender.getAvatarUrl(),
                "/friends");
        wsPublisher.publishNotification(receiver.getId(), toDto(n));
    }

    @Transactional
    public void createFriendAcceptedNotification(User requester, User acceptor) {
        Notification n = save(requester,
                "friend_accepted",
                "Friend request accepted",
                acceptor.getName() + " accepted your friend request 🎉",
                acceptor.getAvatarUrl(),
                "/friends");
        wsPublisher.publishNotification(requester.getId(), toDto(n));
    }

    @Transactional
    public void createMessageNotification(User recipient, User sender,
                                          String chatRoomId, String preview) {
        Notification n = save(recipient,
                "message",
                "New message from " + sender.getName(),
                preview,
                sender.getAvatarUrl(),
                "/chat/" + chatRoomId);
        wsPublisher.publishNotification(recipient.getId(), toDto(n));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Notification save(User user, String type, String title,
                               String body, String imageUrl, String actionUrl) {
        return notificationRepository.save(Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .body(body)
                .imageUrl(imageUrl)
                .actionUrl(actionUrl)
                .build());
    }

    private NotificationDto toDto(Notification n) {
        return NotificationDto.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .body(n.getBody())
                .imageUrl(n.getImageUrl())
                .read(Boolean.TRUE.equals(n.getReadStatus()))  // mapped from readStatus
                .actionUrl(n.getActionUrl())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
