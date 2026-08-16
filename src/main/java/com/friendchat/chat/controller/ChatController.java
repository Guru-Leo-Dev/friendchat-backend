package com.friendchat.chat.controller;

import com.friendchat.chat.dto.ChatRoomDto;
import com.friendchat.chat.service.ChatService;
import com.friendchat.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping
    public ResponseEntity<List<ChatRoomDto>> getChats(@AuthenticationPrincipal CurrentUser p) {
        return ResponseEntity.ok(chatService.getChatsForUser(p.getId()));
    }

    @GetMapping("/{chatId}")
    public ResponseEntity<ChatRoomDto> getChat(
            @AuthenticationPrincipal CurrentUser p, @PathVariable UUID chatId) {
        return ResponseEntity.ok(chatService.getChat(chatId, p.getId()));
    }

    @PostMapping("/direct")
    public ResponseEntity<ChatRoomDto> createDirectChat(
            @AuthenticationPrincipal CurrentUser p, @Valid @RequestBody DirectChatRequest req) {
        return ResponseEntity.ok(chatService.createDirectChat(p.getId(), req.getUserId()));
    }

    @PostMapping("/group")
    public ResponseEntity<ChatRoomDto> createGroupChat(
            @AuthenticationPrincipal CurrentUser p, @Valid @RequestBody GroupChatRequest req) {
        return ResponseEntity.ok(chatService.createGroupChat(p.getId(), req.getName(), req.getMemberIds()));
    }

    @PostMapping("/{chatId}/read")
    public ResponseEntity<Void> markRead(
            @AuthenticationPrincipal CurrentUser p, @PathVariable UUID chatId) {
        chatService.markRead(chatId, p.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{chatId}/leave")
    public ResponseEntity<Void> leaveGroup(
            @AuthenticationPrincipal CurrentUser p, @PathVariable UUID chatId) {
        chatService.leaveGroup(chatId, p.getId());
        return ResponseEntity.noContent().build();
    }

    @Data static class DirectChatRequest { @NotNull UUID userId; }
    @Data static class GroupChatRequest  { @NotBlank String name; @NotNull List<UUID> memberIds; }
}
