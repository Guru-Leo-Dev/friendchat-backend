package com.friendchat.friend.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    @Query("""
        SELECT f FROM Friendship f
        WHERE f.userA.id = :userId OR f.userB.id = :userId
        """)
    List<Friendship> findAllByUserId(@Param("userId") UUID userId);

    @Query("""
        SELECT COUNT(f) > 0 FROM Friendship f
        WHERE (f.userA.id = :a AND f.userB.id = :b)
           OR (f.userA.id = :b AND f.userB.id = :a)
        """)
    boolean areFriends(@Param("a") UUID a, @Param("b") UUID b);

    @Modifying
    @Query("""
        DELETE FROM Friendship f
        WHERE (f.userA.id = :a AND f.userB.id = :b)
           OR (f.userA.id = :b AND f.userB.id = :a)
        """)
    void deleteFriendship(@Param("a") UUID a, @Param("b") UUID b);

    @Query("""
        SELECT COUNT(f) FROM Friendship f
        WHERE f.userA.id = :userId OR f.userB.id = :userId
        """)
    int countByUserId(@Param("userId") UUID userId);

    // MySQL-compatible mutual friends count using two separate exists checks
    @Query(value = """
        SELECT COUNT(*) FROM friendships f
        WHERE
          (f.user_id_a = :a OR f.user_id_b = :a)
          AND (
            f.user_id_a IN (
                SELECT user_id_b FROM friendships WHERE user_id_a = :b
                UNION
                SELECT user_id_a FROM friendships WHERE user_id_b = :b
            )
            OR f.user_id_b IN (
                SELECT user_id_b FROM friendships WHERE user_id_a = :b
                UNION
                SELECT user_id_a FROM friendships WHERE user_id_b = :b
            )
          )
        """, nativeQuery = true)
    int countMutualFriends(@Param("a") UUID a, @Param("b") UUID b);
}
