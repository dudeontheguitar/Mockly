package com.mockly.data.repository;

import com.mockly.data.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    @Query("""
            SELECT c FROM Conversation c
            JOIN ConversationParticipant p1 ON p1.conversationId = c.id
            JOIN ConversationParticipant p2 ON p2.conversationId = c.id
            WHERE c.type = com.mockly.data.enums.ConversationType.DIRECT
              AND p1.userId = :firstUserId
              AND p2.userId = :secondUserId
            """)
    Optional<Conversation> findDirectConversation(
            @Param("firstUserId") UUID firstUserId,
            @Param("secondUserId") UUID secondUserId
    );

    @Query(
            value = """
                    SELECT DISTINCT c FROM Conversation c
                    JOIN ConversationParticipant p ON p.conversationId = c.id
                    WHERE p.userId = :userId
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT c) FROM Conversation c
                    JOIN ConversationParticipant p ON p.conversationId = c.id
                    WHERE p.userId = :userId
                    """
    )
    Page<Conversation> findByParticipant(@Param("userId") UUID userId, Pageable pageable);
}
