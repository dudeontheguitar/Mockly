package com.mockly.core.service;

import com.mockly.core.dto.artifact.CompleteUploadRequest;
import com.mockly.core.dto.artifact.RequestUploadRequest;
import com.mockly.core.dto.artifact.RequestUploadResponse;
import com.mockly.core.dto.session.ArtifactResponse;
import com.mockly.core.event.ArtifactUploadCompletedEvent;
import com.mockly.core.exception.BadRequestException;
import com.mockly.core.exception.ResourceNotFoundException;
import com.mockly.data.entity.Artifact;
import com.mockly.data.repository.ArtifactRepository;
import com.mockly.data.repository.SessionParticipantRepository;
import com.mockly.data.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;





@Service
@RequiredArgsConstructor
@Slf4j
public class ArtifactService {

    private static final long MAX_FILE_SIZE_BYTES = 500 * 1024 * 1024; 
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "audio/mpeg", "audio/mp3", "audio/wav", "audio/wave", "audio/x-wav",
            "audio/webm", "audio/ogg", "audio/mp4", "audio/x-m4a",
            "application/octet-stream" 
    );
    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            ".mp3", ".wav", ".webm", ".ogg", ".m4a", ".mp4", ".bin", ".raw"
    );

    private final ArtifactRepository artifactRepository;
    private final SessionRepository sessionRepository;
    private final SessionParticipantRepository participantRepository;
    private final MinIOService minIOService;
    private final ApplicationEventPublisher eventPublisher;

    








    @Transactional
    public RequestUploadResponse requestUpload(UUID sessionId, UUID userId, RequestUploadRequest request) {
        log.info("Requesting upload URL for session: {}, type: {}, fileName: {}", 
                sessionId, request.type(), request.fileName());

        
        var session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));
        requireSessionAccess(session.getCreatedBy(), sessionId, userId);

        
        if (request.fileSizeBytes() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException(
                    String.format("File size exceeds maximum allowed size of %d MB", MAX_FILE_SIZE_BYTES / (1024 * 1024))
            );
        }

        
        validateFileType(request.fileName(), request.contentType());

        
        UUID artifactId = UUID.randomUUID();
        String objectName = String.format("sessions/%s/artifacts/%s/%s", 
                sessionId, artifactId, sanitizeFileName(request.fileName()));

        
        Artifact artifact = Artifact.builder()
                .id(artifactId)
                .sessionId(sessionId)
                .type(request.type())
                .storageUrl(objectName)
                .sizeBytes(request.fileSizeBytes())
                .build();

        artifact = artifactRepository.save(artifact);
        log.info("Created artifact record: {}", artifactId);

        
        int expirySeconds = 3600;
        String uploadUrl = minIOService.generatePresignedUploadUrl(objectName, expirySeconds);

        return new RequestUploadResponse(
                artifactId,
                uploadUrl,
                objectName,
                expirySeconds
        );
    }

    









    @Transactional
    public ArtifactResponse completeUpload(UUID sessionId, UUID artifactId, UUID userId, CompleteUploadRequest request) {
        log.info("Completing upload for artifact: {} in session: {}", artifactId, sessionId);

        
        var session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));
        requireSessionAccess(session.getCreatedBy(), sessionId, userId);

        
        Artifact artifact = artifactRepository.findById(artifactId)
                .orElseThrow(() -> new ResourceNotFoundException("Artifact not found: " + artifactId));

        
        if (!artifact.getSessionId().equals(sessionId)) {
            throw new BadRequestException("Artifact does not belong to this session");
        }

        
        String objectName = minIOService.normalizeObjectName(artifact.getStorageUrl());
        if (!minIOService.objectExists(objectName)) {
            throw new BadRequestException("File was not uploaded to storage. Please upload the file first.");
        }

        
        var metadata = minIOService.getObjectMetadata(objectName);
        long actualSize = metadata.size();

        
        artifact.setSizeBytes(actualSize);
        artifact.setDurationSec(request.durationSec());
        artifact.setStorageUrl(objectName);

        artifact = artifactRepository.save(artifact);
        log.info("Completed upload for artifact: {}", artifactId);

        eventPublisher.publishEvent(new ArtifactUploadCompletedEvent(
                sessionId,
                artifact.getId(),
                artifact.getType(),
                userId
        ));

        return toResponse(artifact);
    }

    


    @Transactional(readOnly = true)
    public ArtifactResponse getArtifact(UUID sessionId, UUID artifactId, UUID userId) {
        Artifact artifact = artifactRepository.findById(artifactId)
                .orElseThrow(() -> new ResourceNotFoundException("Artifact not found: " + artifactId));

        if (!artifact.getSessionId().equals(sessionId)) {
            throw new BadRequestException("Artifact does not belong to this session");
        }
        var session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));
        requireSessionAccess(session.getCreatedBy(), sessionId, userId);

        return toResponse(artifact);
    }

    


    @Transactional(readOnly = true)
    public List<ArtifactResponse> listArtifacts(UUID sessionId, UUID userId) {
        
        var session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));
        requireSessionAccess(session.getCreatedBy(), sessionId, userId);

        List<Artifact> artifacts = artifactRepository.findBySessionId(sessionId);
        return artifacts.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    


    private void validateFileType(String fileName, String contentType) {
        
        String lowerFileName = fileName.toLowerCase();
        boolean hasValidExtension = ALLOWED_EXTENSIONS.stream()
                .anyMatch(lowerFileName::endsWith);

        if (!hasValidExtension) {
            throw new BadRequestException(
                    String.format("File type not allowed. Allowed extensions: %s", ALLOWED_EXTENSIONS)
            );
        }

        
        if (contentType != null && !contentType.isBlank()) {
            String lowerContentType = contentType.toLowerCase();
            boolean hasValidContentType = ALLOWED_CONTENT_TYPES.stream()
                    .anyMatch(lowerContentType::startsWith);

            if (!hasValidContentType) {
                log.warn("Content type {} not in allowed list, but extension is valid. Proceeding.", contentType);
            }
        }
    }

    


    private String sanitizeFileName(String fileName) {
        
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void requireSessionAccess(UUID createdBy, UUID sessionId, UUID userId) {
        if (!createdBy.equals(userId) && !participantRepository.existsBySessionIdAndUserId(sessionId, userId)) {
            throw new BadRequestException("You don't have access to this session");
        }
    }

    


    private ArtifactResponse toResponse(Artifact artifact) {
        return new ArtifactResponse(
                artifact.getId(),
                artifact.getSessionId(),
                artifact.getType(),
                artifact.getStorageUrl(),
                artifact.getDurationSec(),
                artifact.getSizeBytes(),
                artifact.getCreatedAt()
        );
    }
}

