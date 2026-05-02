package com.mockly.core.service;

import com.mockly.core.event.ArtifactUploadCompletedEvent;
import com.mockly.data.enums.ArtifactType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;




@Component
@RequiredArgsConstructor
@Slf4j
public class ArtifactUploadEventListener {

    private final ReportService reportService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleArtifactUploaded(ArtifactUploadCompletedEvent event) {
        if (!isAudioArtifact(event.artifactType())) {
            return;
        }

        try {
            reportService.triggerReportGenerationForArtifact(
                    event.sessionId(),
                    event.uploadedBy(),
                    event.artifactId()
            );
            log.info("Triggered report generation from artifact upload event: session={}, artifact={}",
                    event.sessionId(), event.artifactId());
        } catch (Exception e) {
            log.error("Failed to trigger report generation for uploaded artifact: session={}, artifact={}",
                    event.sessionId(), event.artifactId(), e);
        }
    }

    private boolean isAudioArtifact(ArtifactType type) {
        return type == ArtifactType.AUDIO_MIXED
                || type == ArtifactType.AUDIO_LEFT
                || type == ArtifactType.AUDIO_RIGHT;
    }
}
