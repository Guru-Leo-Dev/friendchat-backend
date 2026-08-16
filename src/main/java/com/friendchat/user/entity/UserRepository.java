package com.friendchat.user.entity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByGoogleId(String googleId);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    // MySQL uses LIKE — no pg_trgm extension needed
    @Query("""
        SELECT u FROM User u
        WHERE u.isActive = true
          AND u.id <> :currentUserId
          AND (
               LOWER(u.name)     LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
          )
        ORDER BY u.name ASC
        """)
    Page<User> searchUsers(@Param("query") String query,
                           @Param("currentUserId") UUID currentUserId,
                           Pageable pageable);

    @Modifying
    @Query("UPDATE User u SET u.status = :status, u.lastSeen = :lastSeen WHERE u.id = :userId")
    void updateStatus(@Param("userId") UUID userId,
                      @Param("status") UserStatus status,
                      @Param("lastSeen") Instant lastSeen);

    @Query("SELECT u FROM User u WHERE u.id IN :ids")
    List<User> findAllByIds(@Param("ids") List<UUID> ids);
}
