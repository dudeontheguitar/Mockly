package com.mockly.core.service;

import com.mockly.core.dto.ml.MLProcessRequest;
import com.mockly.core.dto.report.ReportResponse;
import com.mockly.core.event.ReportReadyEvent;
import com.mockly.core.exception.ResourceNotFoundException;
import com.mockly.data.entity.Artifact;
import com.mockly.data.entity.Report;
import com.mockly.data.entity.Transcript;
import com.mockly.data.repository.ArtifactRepository;
import com.mockly.data.repository.ReportRepository;
import com.mockly.data.repository.TranscriptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Performs heavy ML processing for reports in background threads.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportProcessingService {

    private final ReportRepository reportRepository;
    private final ArtifactRepository artifactRepository;
    private final TranscriptRepository transcriptRepository;
    private final MLServiceClient mlServiceClient;
    private final MinIOService minIOService;
    private final ApplicationEventPublisher eventPublisher;

    @Async("reportProcessingExecutor")
    @Transactional
    public CompletableFuture<Void> processReportAsync(UUID sessionId, UUID artifactId) {
        log.info("Starting async report processing for session: {}, artifact: {}", sessionId, artifactId);

        Report report = reportRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + sessionId));

        try {
            report.setStatus(Report.ReportStatus.PROCESSING);
            report = reportRepository.save(report);

            Artifact artifact = artifactRepository.findById(artifactId)
                    .orElseThrow(() -> new ResourceNotFoundException("Artifact not found: " + artifactId));

            String objectName = minIOService.normalizeObjectName(artifact.getStorageUrl());
            String artifactUrl = minIOService.generatePresignedDownloadUrl(objectName, 3600);

            MLProcessRequest mlRequest = new MLProcessRequest(
                    sessionId,
                    artifactId,
                    artifactUrl,
                    artifact.getType().name()
            );

            var mlResponse = mlServiceClient.processArtifact(mlRequest);

            if (mlResponse.transcript() != null && !mlResponse.transcript().isEmpty()) {
                Transcript transcript = Transcript.builder()
                        .sessionId(sessionId)
                        .source(Transcript.TranscriptSource.MIXED)
                        .text(mlResponse.transcript())
                        .build();
                transcriptRepository.save(transcript);
                log.info("Saved transcript for session: {}", sessionId);
            }

            report.setMetrics(mlResponse.metrics());
            report.setSummary(mlResponse.summary());
            report.setRecommendations(mlResponse.recommendations());
            report.setStatus(Report.ReportStatus.READY);
            report.setErrorMessage(null);
            report = reportRepository.save(report);

            log.info("Report processing completed successfully for session: {}", sessionId);
            eventPublisher.publishEvent(new ReportReadyEvent(sessionId, toResponse(report)));
        } catch (Exception e) {
            log.error("Report processing failed for session: {}", sessionId, e);
            report.setStatus(Report.ReportStatus.FAILED);
            report.setErrorMessage(e.getMessage());
            reportRepository.save(report);
        }

        return CompletableFuture.completedFuture(null);
    }

    private ReportResponse toResponse(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getSessionId(),
                report.getMetrics(),
                report.getSummary(),
                report.getRecommendations(),
                report.getStatus(),
                report.getErrorMessage(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }
}
