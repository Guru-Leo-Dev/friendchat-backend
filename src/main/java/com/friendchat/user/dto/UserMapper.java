package com.friendchat.user.dto;

import com.friendchat.user.entity.User;
import org.springframework.stereotype.Component;

/**
 * Manual mapper replacing MapStruct to avoid annotation-processor
 * configuration issues across different IDEs and build environments.
 */
@Component
public class UserMapper {

    public UserDto toDto(User user) {
        if (user == null) return null;
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus())
                .lastSeen(user.getLastSeen())
                .bio(user.getBio())
                .build();
    }

    public void updateUserFromRequest(UpdateProfileRequest request, User user) {
        if (request == null || user == null) return;
        if (request.getName()     != null) user.setName(request.getName());
        if (request.getUsername() != null) user.setUsername(request.getUsername());
        if (request.getBio()      != null) user.setBio(request.getBio());
    }
}
