package com.friendchat.user.controller;

import com.friendchat.common.dto.PageResponse;
import com.friendchat.security.CurrentUser;
import com.friendchat.user.dto.*;
import com.friendchat.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserDto> getMe(@AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(userService.getMe(principal.getId()));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserDto> updateProfile(
            @AuthenticationPrincipal CurrentUser principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(principal.getId(), request));
    }

    @PostMapping("/profile/avatar")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @AuthenticationPrincipal CurrentUser principal,
            @RequestParam("avatar") MultipartFile file) {
        String url = userService.uploadAvatar(principal.getId(), file);
        return ResponseEntity.ok(Map.of("avatarUrl", url));
    }

    @GetMapping("/users/search")
    public ResponseEntity<PageResponse<UserDto>> searchUsers(
            @AuthenticationPrincipal CurrentUser principal,
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("name"));
        return ResponseEntity.ok(PageResponse.of(userService.searchUsers(query, principal.getId(), pageable)));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserProfileDto> getUserProfile(
            @AuthenticationPrincipal CurrentUser principal,
            @PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getUserProfile(userId, principal.getId()));
    }
}
