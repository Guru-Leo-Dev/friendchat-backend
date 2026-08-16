package com.friendchat.user.service;

import com.friendchat.common.exception.ConflictException;
import com.friendchat.common.exception.ResourceNotFoundException;
import com.friendchat.common.util.S3Service;
import com.friendchat.friend.entity.FriendRequestRepository;
import com.friendchat.friend.entity.FriendRequestStatus;
import com.friendchat.friend.entity.FriendshipRepository;
import com.friendchat.user.dto.UpdateProfileRequest;
import com.friendchat.user.dto.UserDto;
import com.friendchat.user.dto.UserMapper;
import com.friendchat.user.dto.UserProfileDto;
import com.friendchat.user.entity.User;
import com.friendchat.user.entity.UserRepository;
import com.friendchat.user.entity.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository          userRepository;
    private final FriendshipRepository    friendshipRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final UserMapper              userMapper;
    private final S3Service               s3Service;

    @Transactional(readOnly = true)
    public UserDto getMe(UUID userId) {
        return userMapper.toDto(findByIdOrThrow(userId));
    }

    @Transactional
    public UserDto updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findByIdOrThrow(userId);
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername()))
                throw new ConflictException("Username already taken");
        }
        userMapper.updateUserFromRequest(request, user);
        return userMapper.toDto(userRepository.save(user));
    }

    @Transactional
    public String uploadAvatar(UUID userId, MultipartFile file) {
        User user = findByIdOrThrow(userId);
        String url = s3Service.uploadAvatar(userId, file);
        user.setAvatarUrl(url);
        userRepository.save(user);
        return url;
    }

    @Transactional(readOnly = true)
    public Page<UserDto> searchUsers(String query, UUID currentUserId, Pageable pageable) {
        return userRepository.searchUsers(query, currentUserId, pageable).map(userMapper::toDto);
    }

    @Transactional(readOnly = true)
    public UserProfileDto getUserProfile(UUID targetUserId, UUID currentUserId) {
        User target      = findByIdOrThrow(targetUserId);
        int friendsCount = friendshipRepository.countByUserId(targetUserId);
        int mutualCount  = friendshipRepository.countMutualFriends(currentUserId, targetUserId);
        String status    = resolveFriendshipStatus(currentUserId, targetUserId);
        return UserProfileDto.builder()
                .id(target.getId()).name(target.getName()).username(target.getUsername())
                .email(target.getEmail()).avatarUrl(target.getAvatarUrl())
                .status(target.getStatus()).lastSeen(target.getLastSeen()).bio(target.getBio())
                .friendsCount(friendsCount).mutualFriendsCount(mutualCount)
                .friendshipStatus(status).build();
    }

    @Transactional
    public void setOnline(UUID userId) {
        userRepository.updateStatus(userId, UserStatus.online, null);
    }

    @Transactional
    public void setOffline(UUID userId) {
        userRepository.updateStatus(userId, UserStatus.offline, Instant.now());
    }

    public User findByIdOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private String resolveFriendshipStatus(UUID currentUserId, UUID targetUserId) {
        if (friendshipRepository.areFriends(currentUserId, targetUserId)) return "friends";
        var request = friendRequestRepository.findBetween(currentUserId, targetUserId);
        if (request.isEmpty()) return "none";
        var req = request.get();
        if (req.getStatus() != FriendRequestStatus.pending) return "none";
        return req.getSender().getId().equals(currentUserId) ? "pending_sent" : "pending_received";
    }
}
