package com.friendchat.auth.controller.dto;

import com.friendchat.user.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AuthResponse {
    private final UserDto   user;
    private final TokenPair tokens;
}
