package com.mockly.data.repository;

import com.mockly.data.entity.SessionParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionParticipantRepository extends JpaRepository<SessionParticipant, UUID> {

    


    Optional<SessionParticipant> findBySessionIdAndUserId(UUID sessionId, UUID userId);

    


    List<SessionParticipant> findBySessionId(UUID sessionId);

    


    List<SessionParticipant> findByUserId(UUID userId);

    


    boolean existsBySessionIdAndUserId(UUID sessionId, UUID userId);

    


    long countBySessionId(UUID sessionId);

    


    List<SessionParticipant> findBySessionIdAndLeftAtIsNull(UUID sessionId);

    


    long countBySessionIdAndJoinedAtIsNotNullAndLeftAtIsNull(UUID sessionId);

    


    long countBySessionIdAndJoinedAtIsNotNull(UUID sessionId);
}

