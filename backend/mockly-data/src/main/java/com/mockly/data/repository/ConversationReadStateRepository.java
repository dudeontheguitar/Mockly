package com.mockly.data.repository;

import com.mockly.data.entity.ConversationReadState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationReadStateRepository extends JpaRepository<ConversationReadState, UUID> {
    Optional<ConversationReadState> findByConversationIdAndUserId(UUID conversationId, UUID userId);
}
