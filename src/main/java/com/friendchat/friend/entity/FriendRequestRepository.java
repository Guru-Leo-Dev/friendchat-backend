package com.friendchat.friend.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, UUID> {

    List<FriendRequest> findByReceiverIdAndStatus(UUID receiverId, FriendRequestStatus status);

    List<FriendRequest> findBySenderIdAndStatus(UUID senderId, FriendRequestStatus status);

    // MySQL does not support LIMIT inside subqueries in all contexts;
    // use Spring Pageable approach instead
    @Query("""
        SELECT fr FROM FriendRequest fr
        WHERE (fr.sender.id = :a AND fr.receiver.id = :b)
           OR (fr.sender.id = :b AND fr.receiver.id = :a)
        ORDER BY fr.createdAt DESC
        """)
    List<FriendRequest> findBetweenList(@Param("a") UUID a, @Param("b") UUID b);

    default Optional<FriendRequest> findBetween(UUID a, UUID b) {
        List<FriendRequest> results = findBetweenList(a, b);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    boolean existsBySenderIdAndReceiverId(UUID senderId, UUID receiverId);
}
