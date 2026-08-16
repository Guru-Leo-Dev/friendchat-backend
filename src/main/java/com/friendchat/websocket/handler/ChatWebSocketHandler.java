package com.friendchat.websocket.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.friendchat.message.service.MessageService;
import com.friendchat.user.service.UserService;
import com.friendchat.websocket.event.WsEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper     objectMapper;
    private final MessageService   messageService;
    private final UserService      userService;
    private final WsEventPublisher wsPublisher;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        UUID userId = extractUserId(session);
        if (userId == null) {
            closeQuietly(session);
            return;
        }
        wsPublisher.addSession(session.getId(), session, userId);
        userService.setOnline(userId);
        wsPublisher.broadcastUserStatus(userId, "online", null);
        log.info("[WS] Connected: user={} session={}", userId, session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        UUID userId = extractUserId(session);
        if (userId != null) {
            wsPublisher.removeSession(session.getId(), userId);
            // Only go offline if no other sessions remain for this user
            if (!wsPublisher.hasOtherSessions(userId)) {
                userService.setOffline(userId);
                wsPublisher.broadcastUserStatus(userId, "offline",
                        java.time.Instant.now().toString());
            }
        }
        log.info("[WS] Disconnected: session={} reason={}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("[WS] Transport error on session {}: {}", session.getId(), exception.getMessage());
    }

    // ── Inbound messages ──────────────────────────────────────────────────────

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) {
        UUID userId = extractUserId(session);
        if (userId == null) return;

        try {
            JsonNode root  = objectMapper.readTree(textMessage.getPayload());
            String   event = root.path("event").asText();
            JsonNode data  = root.path("data");

            switch (event) {
                case "message:send"  -> handleMessageSend(userId, data);
                case "typing:start"  -> wsPublisher.publishTyping(
                        uuid(data, "chatRoomId"), userId, true, session.getId());
                case "typing:stop"   -> wsPublisher.publishTyping(
                        uuid(data, "chatRoomId"), userId, false, session.getId());
                case "message:read"  -> wsPublisher.publishReadStatus(
                        uuid(data, "chatRoomId"), userId);
                case "chat:join"     -> wsPublisher.joinRoom(session.getId(), uuid(data, "chatRoomId"));
                case "chat:leave"    -> wsPublisher.leaveRoom(session.getId(), uuid(data, "chatRoomId"));
                default              -> log.debug("[WS] Unknown event: {}", event);
            }
        } catch (Exception e) {
            log.error("[WS] Error handling message from {}: {}", userId, e.getMessage(), e);
        }
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    private void handleMessageSend(UUID senderId, JsonNode data) {
        UUID   chatRoomId = uuid(data, "chatRoomId");
        String content    = data.path("content").asText();
        UUID   replyToId  = data.has("replyToId") && !data.path("replyToId").isNull()
                ? uuid(data, "replyToId") : null;
        messageService.send(chatRoomId, senderId, content, replyToId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private UUID extractUserId(WebSocketSession session) {
        Object uid = session.getAttributes().get("userId");
        return uid instanceof UUID ? (UUID) uid : null;
    }

    private UUID uuid(JsonNode node, String field) {
        return UUID.fromString(node.path(field).asText());
    }

    private void closeQuietly(WebSocketSession session) {
        try { session.close(CloseStatus.NOT_ACCEPTABLE); } catch (Exception ignored) {}
    }
}
