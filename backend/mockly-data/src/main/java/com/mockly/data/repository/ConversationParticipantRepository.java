package com.mockly.data.repository;

import com.mockly.data.entity.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, UUID> {
    boolean existsByConversationIdAndUserId(UUID conversationId, UUID userId);
    List<ConversationParticipant> findByConversationId(UUID conversationId);
    List<ConversationParticipant> findByUserId(UUID userId);
}
