package com.friendchat.friend.controller;

import com.friendchat.friend.dto.*;
import com.friendchat.friend.service.FriendService;
import com.friendchat.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @GetMapping
    public ResponseEntity<List<FriendshipDto>> getFriends(@AuthenticationPrincipal CurrentUser p) {
        return ResponseEntity.ok(friendService.getFriends(p.getId()));
    }

    @GetMapping("/requests")
    public ResponseEntity<List<FriendRequestDto>> getRequests(@AuthenticationPrincipal CurrentUser p) {
        return ResponseEntity.ok(friendService.getRequests(p.getId()));
    }

    @PostMapping("/request")
    public ResponseEntity<FriendRequestDto> sendRequest(
            @AuthenticationPrincipal CurrentUser p, @RequestBody Map<String, UUID> body) {
        return ResponseEntity.ok(friendService.sendRequest(p.getId(), body.get("userId")));
    }

    @PostMapping("/accept/{requestId}")
    public ResponseEntity<Void> accept(
            @AuthenticationPrincipal CurrentUser p, @PathVariable UUID requestId) {
        friendService.accept(requestId, p.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reject/{requestId}")
    public ResponseEntity<Void> reject(
            @AuthenticationPrincipal CurrentUser p, @PathVariable UUID requestId) {
        friendService.reject(requestId, p.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeFriend(
            @AuthenticationPrincipal CurrentUser p, @PathVariable UUID userId) {
        friendService.removeFriend(p.getId(), userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/block/{userId}")
    public ResponseEntity<Void> blockUser(
            @AuthenticationPrincipal CurrentUser p, @PathVariable UUID userId) {
        friendService.blockUser(p.getId(), userId);
        return ResponseEntity.noContent().build();
    }
}
