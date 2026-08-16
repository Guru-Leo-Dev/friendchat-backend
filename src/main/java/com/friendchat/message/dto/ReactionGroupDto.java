package com.friendchat.message.dto;

import com.friendchat.user.dto.UserDto;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ReactionGroupDto {
    String        emoji;
    List<UserDto> users;
    int           count;
}
