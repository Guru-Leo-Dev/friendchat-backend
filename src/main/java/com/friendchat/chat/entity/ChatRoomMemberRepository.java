package com.friendchat.chat.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, UUID> {

    Optional<ChatRoomMember> findByChatRoomIdAndUserId(UUID chatRoomId, UUID userId);

    @Modifying
    @Query("""
        UPDATE ChatRoomMember m SET m.lastReadAt = :now
        WHERE m.chatRoom.id = :chatRoomId AND m.user.id = :userId
        """)
    void markRead(@Param("chatRoomId") UUID chatRoomId,
                  @Param("userId") UUID userId,
                  @Param("now") Instant now);

    void deleteByChatRoomIdAndUserId(UUID chatRoomId, UUID userId);
}
