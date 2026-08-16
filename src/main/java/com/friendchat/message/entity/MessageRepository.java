package com.friendchat.message.entity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("""
        SELECT m FROM Message m
        WHERE m.chatRoom.id = :chatRoomId
          AND m.deletedAt IS NULL
        ORDER BY m.createdAt DESC
        """)
    Page<Message> findByChatRoomId(@Param("chatRoomId") UUID chatRoomId, Pageable pageable);

    // MySQL-compatible: use Spring Data method instead of LIMIT in JPQL
    @Query("""
        SELECT m FROM Message m
        WHERE m.chatRoom.id = :chatRoomId
        ORDER BY m.createdAt DESC
        """)
    Page<Message> findLastByChatRoomIdPage(@Param("chatRoomId") UUID chatRoomId, Pageable pageable);

    default Optional<Message> findLastByChatRoomId(UUID chatRoomId) {
        org.springframework.data.domain.PageRequest page =
            org.springframework.data.domain.PageRequest.of(0, 1);
        Page<Message> result = findLastByChatRoomIdPage(chatRoomId, page);
        return result.getContent().isEmpty()
                ? Optional.empty()
                : Optional.of(result.getContent().get(0));
    }

    @Modifying
    @Query("""
        UPDATE Message m SET m.status = 'read'
        WHERE m.chatRoom.id = :chatRoomId
          AND m.sender.id <> :userId
          AND m.status <> 'read'
          AND m.deletedAt IS NULL
        """)
    int markAllReadInChat(@Param("chatRoomId") UUID chatRoomId,
                          @Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM Message m WHERE m.expiresAt < :now AND m.deletedAt IS NULL")
    int deleteExpired(@Param("now") Instant now);

    @Query("""
        SELECT COUNT(m) FROM Message m
        WHERE m.chatRoom.id = :chatRoomId
          AND m.sender.id <> :userId
          AND m.status <> 'read'
          AND m.deletedAt IS NULL
        """)
    long countUnread(@Param("chatRoomId") UUID chatRoomId,
                     @Param("userId") UUID userId);
}
