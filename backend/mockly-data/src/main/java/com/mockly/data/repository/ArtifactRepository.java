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

    


    List<Artifact> findBySessionId(UUID sessionId);

    


    List<Artifact> findBySessionIdAndType(UUID sessionId, ArtifactType type);

    



    Optional<Artifact> findFirstBySessionIdAndType(UUID sessionId, ArtifactType type);

    


    long countBySessionId(UUID sessionId);

    


    boolean existsBySessionIdAndType(UUID sessionId, ArtifactType type);

    


    boolean existsBySessionIdAndStorageUrl(UUID sessionId, String storageUrl);

    


    Optional<Artifact> findBySessionIdAndStorageUrl(UUID sessionId, String storageUrl);

    



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

