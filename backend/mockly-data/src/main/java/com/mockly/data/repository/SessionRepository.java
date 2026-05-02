package com.mockly.data.repository;

import com.mockly.data.entity.Session;
import com.mockly.data.enums.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {

    


    Optional<Session> findByIdAndCreatedBy(UUID id, UUID createdBy);

    


    List<Session> findByCreatedByOrderByCreatedAtDesc(UUID userId);

    


    List<Session> findByStatus(SessionStatus status);

    



    Optional<Session> findFirstByCreatedByAndStatusInOrderByCreatedAtDesc(
        UUID userId, 
        List<SessionStatus> statuses
    );

    


    @Query("SELECT s FROM Session s JOIN s.participants p WHERE p.userId = :userId ORDER BY s.createdAt DESC")
    List<Session> findByParticipantUserId(@Param("userId") UUID userId);

    


    @Query("SELECT s FROM Session s JOIN s.participants p WHERE p.userId = :userId AND s.status = :status ORDER BY s.createdAt DESC")
    List<Session> findByParticipantUserIdAndStatus(
        @Param("userId") UUID userId, 
        @Param("status") SessionStatus status
    );

    


    @Query(
            value = """
                    SELECT DISTINCT s FROM Session s
                    LEFT JOIN s.participants p
                    WHERE s.createdBy = :userId OR p.userId = :userId
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT s) FROM Session s
                    LEFT JOIN s.participants p
                    WHERE s.createdBy = :userId OR p.userId = :userId
                    """
    )
    Page<Session> findVisibleToUser(@Param("userId") UUID userId, Pageable pageable);

    


    @Query(
            value = """
                    SELECT DISTINCT s FROM Session s
                    LEFT JOIN s.participants p
                    WHERE (s.createdBy = :userId OR p.userId = :userId)
                      AND s.status = :status
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT s) FROM Session s
                    LEFT JOIN s.participants p
                    WHERE (s.createdBy = :userId OR p.userId = :userId)
                      AND s.status = :status
                    """
    )
    Page<Session> findVisibleToUserAndStatus(
            @Param("userId") UUID userId,
            @Param("status") SessionStatus status,
            Pageable pageable
    );

    


    @Query(
            value = """
                    SELECT DISTINCT s FROM Session s
                    LEFT JOIN s.participants p
                    WHERE (s.createdBy = :userId OR p.userId = :userId)
                      AND s.status IN :statuses
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT s) FROM Session s
                    LEFT JOIN s.participants p
                    WHERE (s.createdBy = :userId OR p.userId = :userId)
                      AND s.status IN :statuses
                    """
    )
    Page<Session> findVisibleToUserAndStatusIn(
            @Param("userId") UUID userId,
            @Param("statuses") List<SessionStatus> statuses,
            Pageable pageable
    );

    


    boolean existsByCreatedByAndStatusIn(UUID userId, List<SessionStatus> statuses);
}

