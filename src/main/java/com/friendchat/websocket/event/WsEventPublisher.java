package com.friendchat.websocket.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.friendchat.friend.dto.FriendRequestDto;
import com.friendchat.message.dto.MessageDto;
import com.friendchat.message.dto.ReactionGroupDto;
import com.friendchat.notification.dto.NotificationDto;
import com.friendchat.user.dto.UserDto;
import com.friendchat.user.dto.UserMapper;
import com.friendchat.user.entity.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class WsEventPublisher {

    private final ObjectMapper   objectMapper;
    private final UserRepository userRepository;
    private final UserMapper     userMapper;

    // Session registry — populated by ChatWebSocketHandler
    private final Map<String, WebSocketSession> sessions     = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>>        userSessions = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>>        roomSessions = new ConcurrentHashMap<>();

    // ── Session management ────────────────────────────────────────────────────

    public void addSession(String sessionId, WebSocketSession session, UUID userId) {
        sessions.put(sessionId, session);
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
    }

    public void removeSession(String sessionId, UUID userId) {
        sessions.remove(sessionId);
        Set<String> userSess = userSessions.get(userId);
        if (userSess != null) userSess.remove(sessionId);
        roomSessions.values().forEach(s -> s.remove(sessionId));
    }

    public void joinRoom(String sessionId, UUID chatRoomId) {
        roomSessions.computeIfAbsent(chatRoomId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
    }

    public void leaveRoom(String sessionId, UUID chatRoomId) {
        roomSessions.getOrDefault(chatRoomId, Set.of()).remove(sessionId);
    }

    public boolean hasOtherSessions(UUID userId) {
        Set<String> s = userSessions.get(userId);
        return s != null && !s.isEmpty();
    }

    // ── Message events ────────────────────────────────────────────────────────

    public void publishNewMessage(UUID chatRoomId, MessageDto message, UUID senderId) {
        broadcastToRoom(chatRoomId, "message:receive", Map.of("message", message));
    }

    public void publishMessageDeleted(UUID chatRoomId, UUID messageId) {
        broadcastToRoom(chatRoomId, "message:deleted", Map.of("messageId", messageId));
    }

    public void publishReaction(UUID chatRoomId, UUID messageId, List<ReactionGroupDto> reactions) {
        broadcastToRoom(chatRoomId, "message:reaction",
                Map.of("messageId", messageId, "reactions", reactions));
    }

    public void publishMessageStatus(UUID chatRoomId, UUID messageId, String status) {
        broadcastToRoom(chatRoomId, "message:status",
                Map.of("messageId", messageId, "chatRoomId", chatRoomId, "status", status));
    }

    public void publishReadStatus(UUID chatRoomId, UUID userId) {
        broadcastToRoom(chatRoomId, "message:read",
                Map.of("chatRoomId", chatRoomId, "userId", userId));
    }

    // ── Typing ────────────────────────────────────────────────────────────────

    public void publishTyping(UUID chatRoomId, UUID senderId, boolean isTyping, String senderSessionId) {
        userRepository.findById(senderId).ifPresent(user -> {
            UserDto userDto = userMapper.toDto(user);
            String  event   = isTyping ? "typing:start" : "typing:stop";
            Map<String, Object> data = Map.of(
                    "chatRoomId", chatRoomId, "user", userDto, "isTyping", isTyping);
            broadcastToRoomExcept(chatRoomId, event, data, senderSessionId);
        });
    }

    // ── Presence ──────────────────────────────────────────────────────────────

    public void broadcastUserStatus(UUID userId, String status, String lastSeen) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("status", status);
        if (lastSeen != null) data.put("lastSeen", lastSeen);
        broadcastToAll("online".equals(status) ? "user:online" : "user:offline", data);
    }

    // ── Friend & Notifications ────────────────────────────────────────────────

    public void publishFriendRequest(UUID receiverId, FriendRequestDto request) {
        sendToUser(receiverId, "friend:request", Map.of("request", request));
    }

    public void publishNotification(UUID userId, NotificationDto notification) {
        sendToUser(userId, "notification:new", Map.of("notification", notification));
    }

    // ── Internal broadcast helpers ────────────────────────────────────────────

    private void broadcastToRoom(UUID chatRoomId, String event, Map<String, Object> data) {
        roomSessions.getOrDefault(chatRoomId, Set.of())
                .forEach(sid -> sendToSession(sid, event, data));
    }

    private void broadcastToRoomExcept(UUID chatRoomId, String event,
                                        Map<String, Object> data, String excludeSessionId) {
        roomSessions.getOrDefault(chatRoomId, Set.of()).stream()
                .filter(sid -> !sid.equals(excludeSessionId))
                .forEach(sid -> sendToSession(sid, event, data));
    }

    private void sendToUser(UUID userId, String event, Map<String, Object> data) {
        userSessions.getOrDefault(userId, Set.of())
                .forEach(sid -> sendToSession(sid, event, data));
    }

    private void broadcastToAll(String event, Map<String, Object> data) {
        sessions.keySet().forEach(sid -> sendToSession(sid, event, data));
    }

    private void sendToSession(String sessionId, String event, Object data) {
        WebSocketSession session = sessions.get(sessionId);
        if (session == null || !session.isOpen()) {
            sessions.remove(sessionId);
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(Map.of("event", event, "data", data));
            synchronized (session) {
                session.sendMessage(new TextMessage(payload));
            }
        } catch (Exception e) {
            log.warn("[WS] Failed to send '{}' to session {}: {}", event, sessionId, e.getMessage());
            sessions.remove(sessionId);
        }
    }
}
