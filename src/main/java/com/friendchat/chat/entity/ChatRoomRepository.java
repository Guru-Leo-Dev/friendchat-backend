package com.friendchat.chat.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {

    @Query("""
        SELECT DISTINCT c FROM ChatRoom c
        JOIN c.members m
        WHERE m.user.id = :userId
        ORDER BY c.lastActivity DESC
        """)
    List<ChatRoom> findAllByMemberUserId(@Param("userId") UUID userId);

    @Query("""
        SELECT c FROM ChatRoom c
        JOIN c.members m1
        JOIN c.members m2
        WHERE m1.user.id = :userA AND m2.user.id = :userB AND c.type = 'direct'
        """)
    Optional<ChatRoom> findDirectChat(@Param("userA") UUID userA, @Param("userB") UUID userB);

    @Query("""
        SELECT COUNT(m) FROM ChatRoomMember m
        WHERE m.chatRoom.id = :chatRoomId AND m.user.id = :userId
        """)
    long countMembership(@Param("chatRoomId") UUID chatRoomId, @Param("userId") UUID userId);
}
