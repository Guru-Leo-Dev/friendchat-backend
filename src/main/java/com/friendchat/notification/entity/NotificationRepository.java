package com.friendchat.notification.entity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    // Use "read_status" field name to match the MySQL column (avoids reserved word)
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.readStatus = false")
    long countByUserIdAndReadFalse(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE Notification n SET n.readStatus = true WHERE n.id = :id AND n.user.id = :userId")
    int markRead(@Param("id") UUID id, @Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE Notification n SET n.readStatus = true WHERE n.user.id = :userId AND n.readStatus = false")
    int markAllRead(@Param("userId") UUID userId);
}
