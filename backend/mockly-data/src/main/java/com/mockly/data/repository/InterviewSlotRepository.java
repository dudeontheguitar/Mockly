package com.mockly.data.repository;

import com.mockly.data.entity.InterviewSlot;
import com.mockly.data.enums.InterviewSlotStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterviewSlotRepository extends JpaRepository<InterviewSlot, UUID> {
    List<InterviewSlot> findByStatusOrderByScheduledAtAsc(InterviewSlotStatus status);
    List<InterviewSlot> findByInterviewerIdOrderByScheduledAtDesc(UUID interviewerId);
    Optional<InterviewSlot> findBySessionId(UUID sessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InterviewSlot> findWithLockById(UUID id);
}
