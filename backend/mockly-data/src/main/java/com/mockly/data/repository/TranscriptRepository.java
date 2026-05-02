package com.mockly.data.repository;

import com.mockly.data.entity.Transcript;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TranscriptRepository extends JpaRepository<Transcript, UUID> {

    


    List<Transcript> findBySessionId(UUID sessionId);

    


    List<Transcript> findBySessionIdAndSource(UUID sessionId, Transcript.TranscriptSource source);
}

