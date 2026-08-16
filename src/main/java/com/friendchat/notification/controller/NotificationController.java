package com.friendchat.notification.controller;

import com.friendchat.common.dto.PageResponse;
import com.friendchat.notification.dto.NotificationDto;
import com.friendchat.notification.service.NotificationService;
import com.friendchat.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<PageResponse<NotificationDto>> getAll(
            @AuthenticationPrincipal CurrentUser p,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                notificationService.getNotifications(p.getId(), page, Math.min(size, 50)));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(
            @AuthenticationPrincipal CurrentUser p, @PathVariable UUID id) {
        notificationService.markRead(id, p.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal CurrentUser p) {
        notificationService.markAllRead(p.getId());
        return ResponseEntity.noContent().build();
    }
}
