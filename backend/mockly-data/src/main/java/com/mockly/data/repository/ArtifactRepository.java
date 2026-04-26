package com.mockly.data.repository;

import com.mockly.data.entity.Artifact;
import com.mockly.data.enums.ArtifactType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArtifactRepository extends JpaRepository<Artifact, UUID> {

    /**
     * Find all artifacts for a session.
     */
    List<Artifact> findBySessionId(UUID sessionId);

    /**
     * Find all artifacts of a specific type for a session.
     */
    List<Artifact> findBySessionIdAndType(UUID sessionId, ArtifactType type);

    /**
     * Find a specific artifact by session ID and type.
     * Returns the first match if multiple exist.
     */
    Optional<Artifact> findFirstBySessionIdAndType(UUID sessionId, ArtifactType type);

    /**
     * Count artifacts for a session.
     */
    long countBySessionId(UUID sessionId);

    /**
     * Check if an artifact of a specific type exists for a session.
     */
    boolean existsBySessionIdAndType(UUID sessionId, ArtifactType type);

    /**
     * Check if a specific storage object is already registered for a session.
     */
    boolean existsBySessionIdAndStorageUrl(UUID sessionId, String storageUrl);

    /**
     * Find artifact by session and storage object path.
     */
    Optional<Artifact> findBySessionIdAndStorageUrl(UUID sessionId, String storageUrl);

    /**
     * Insert egress artifact only when it does not already exist.
     * Returns number of inserted rows (0 or 1).
     */
    @Modifying
    @Query(value = """
            INSERT INTO artifacts (session_id, type, storage_url, size_bytes, duration_sec)
            VALUES (:sessionId, :type, :storageUrl, :sizeBytes, :durationSec)
            ON CONFLICT (session_id, storage_url) DO NOTHING
            """, nativeQuery = true)
    int insertEgressArtifactIfAbsent(
            @Param("sessionId") UUID sessionId,
            @Param("type") String type,
            @Param("storageUrl") String storageUrl,
            @Param("sizeBytes") Long sizeBytes,
            @Param("durationSec") Integer durationSec
    );
}

