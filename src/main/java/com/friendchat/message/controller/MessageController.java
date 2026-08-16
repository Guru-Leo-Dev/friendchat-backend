package com.friendchat.message.controller;

import com.friendchat.common.dto.PageResponse;
import com.friendchat.message.dto.MessageDto;
import com.friendchat.message.service.MessageService;
import com.friendchat.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/{chatId}")
    public ResponseEntity<PageResponse<MessageDto>> getMessages(
            @AuthenticationPrincipal CurrentUser p,
            @PathVariable UUID chatId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(messageService.getMessages(chatId, p.getId(), page, Math.min(size, 100)));
    }

    @PostMapping
    public ResponseEntity<MessageDto> send(
            @AuthenticationPrincipal CurrentUser p,
            @Valid @RequestBody SendMessageRequest req) {
        return ResponseEntity.ok(
                messageService.send(req.getChatRoomId(), p.getId(), req.getContent(), req.getReplyToId()));
    }

    @PostMapping("/file")
    public ResponseEntity<MessageDto> sendFile(
            @AuthenticationPrincipal CurrentUser p,
            @RequestParam("chatRoomId") UUID chatRoomId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(messageService.sendFile(chatRoomId, p.getId(), file));
    }

    @PostMapping("/{messageId}/react")
    public ResponseEntity<MessageDto> react(
            @AuthenticationPrincipal CurrentUser p,
            @PathVariable UUID messageId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(messageService.react(messageId, p.getId(), body.get("emoji")));
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal CurrentUser p,
            @PathVariable UUID messageId) {
        messageService.deleteMessage(messageId, p.getId());
        return ResponseEntity.noContent().build();
    }

    @Data
    static class SendMessageRequest {
        @NotNull  UUID   chatRoomId;
        @NotBlank String content;
        UUID replyToId;
    }
}
