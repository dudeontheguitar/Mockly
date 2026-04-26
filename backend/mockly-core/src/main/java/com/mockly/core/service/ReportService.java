package com.mockly.core.service;

import com.mockly.core.dto.report.ReportResponse;
import com.mockly.core.exception.BadRequestException;
import com.mockly.core.exception.ResourceNotFoundException;
import com.mockly.data.entity.Artifact;
import com.mockly.data.entity.Report;
import com.mockly.data.enums.ArtifactType;
import com.mockly.data.repository.ArtifactRepository;
import com.mockly.data.repository.ReportRepository;
import com.mockly.data.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing reports.
 * Handles report generation, ML processing, and status management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;
    private final SessionRepository sessionRepository;
    private final ArtifactRepository artifactRepository;
    private final ReportProcessingService reportProcessingService;

    /**
     * Trigger report generation for a session.
     * Creates a PENDING report and starts async processing.
     *
     * @param sessionId Session ID
     * @param userId User ID (for authorization)
     * @return Report response
     */
    public ReportResponse triggerReportGeneration(UUID sessionId, UUID userId) {
        log.info("Triggering report generation for session: {}", sessionId);

        // Validate session exists
        sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        Artifact artifact = findLatestAudioArtifact(sessionId)
                .orElseThrow(() -> new BadRequestException(
                        "No audio artifact found for session. Please upload an audio file first."));

        return triggerReportGenerationForArtifact(sessionId, userId, artifact.getId());
    }

    /**
     * Trigger report generation for a specific artifact.
     */
    public ReportResponse triggerReportGenerationForArtifact(UUID sessionId, UUID userId, UUID artifactId) {
        log.info("Triggering report generation for session: {}, artifact: {}", sessionId, artifactId);

        sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        Artifact artifact = artifactRepository.findById(artifactId)
                .orElseThrow(() -> new ResourceNotFoundException("Artifact not found: " + artifactId));

        if (!artifact.getSessionId().equals(sessionId)) {
            throw new BadRequestException("Artifact does not belong to this session");
        }
        if (!isAudioArtifact(artifact.getType())) {
            throw new BadRequestException("Only audio artifacts can be processed for report generation");
        }

        ReportPreparation preparation = prepareReportForProcessing(sessionId);
        Report report = preparation.report();
        if (preparation.shouldProcess()) {
            report.setStatus(Report.ReportStatus.PROCESSING);
            report.setErrorMessage(null);
            report = reportRepository.save(report);
            scheduleReportProcessingAfterCommit(sessionId, artifact.getId());
        }

        return toResponse(report);
    }

    /**
     * Get report for a session.
     *
     * @param sessionId Session ID
     * @param userId User ID (for authorization)
     * @return Report response
     */
    public ReportResponse getReport(UUID sessionId, UUID userId) {
        sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        Optional<Report> report = reportRepository.findBySessionId(sessionId);
        if (report.isPresent()) {
            return toResponse(report.get());
        }

        Optional<Artifact> latestAudioArtifact = findLatestAudioArtifact(sessionId);
        if (latestAudioArtifact.isPresent()) {
            return triggerReportGenerationForArtifact(sessionId, userId, latestAudioArtifact.get().getId());
        }

        throw new ResourceNotFoundException(
                "Report not found for session: " + sessionId + ". Upload audio first to start processing."
        );
    }

    /**
     * Convert entity to response DTO.
     */
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

    private ReportPreparation prepareReportForProcessing(UUID sessionId) {
        Optional<Report> existingReport = reportRepository.findBySessionId(sessionId);
        if (existingReport.isPresent()) {
            Report report = existingReport.get();
            if (report.getStatus() == Report.ReportStatus.PROCESSING || report.getStatus() == Report.ReportStatus.READY) {
                log.info("Report already exists and is {} for session: {}", report.getStatus(), sessionId);
                return new ReportPreparation(report, false);
            }

            report.setStatus(Report.ReportStatus.PENDING);
            report.setErrorMessage(null);
            report = reportRepository.save(report);
            return new ReportPreparation(report, true);
        }

        Report report = Report.builder()
                .sessionId(sessionId)
                .status(Report.ReportStatus.PENDING)
                .build();
        try {
            report = reportRepository.save(report);
            return new ReportPreparation(report, true);
        } catch (DataIntegrityViolationException e) {
            Report existing = reportRepository.findBySessionId(sessionId)
                    .orElseThrow(() -> e);
            if (existing.getStatus() == Report.ReportStatus.PROCESSING || existing.getStatus() == Report.ReportStatus.READY) {
                return new ReportPreparation(existing, false);
            }
            existing.setStatus(Report.ReportStatus.PENDING);
            existing.setErrorMessage(null);
            existing = reportRepository.save(existing);
            return new ReportPreparation(existing, true);
        }
    }

    private Optional<Artifact> findLatestAudioArtifact(UUID sessionId) {
        Comparator<Artifact> byCreatedAt = Comparator.comparing(Artifact::getCreatedAt);
        var artifacts = artifactRepository.findBySessionId(sessionId);

        Optional<Artifact> mixed = artifacts.stream()
                .filter(a -> a.getType() == ArtifactType.AUDIO_MIXED)
                .max(byCreatedAt);
        if (mixed.isPresent()) {
            return mixed;
        }

        return artifacts.stream()
                .filter(a -> a.getType() == ArtifactType.AUDIO_LEFT || a.getType() == ArtifactType.AUDIO_RIGHT)
                .max(byCreatedAt);
    }

    private boolean isAudioArtifact(ArtifactType type) {
        return type == ArtifactType.AUDIO_MIXED
                || type == ArtifactType.AUDIO_LEFT
                || type == ArtifactType.AUDIO_RIGHT;
    }

    private void scheduleReportProcessingAfterCommit(UUID sessionId, UUID artifactId) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    reportProcessingService.processReportAsync(sessionId, artifactId);
                }
            });
            return;
        }

        reportProcessingService.processReportAsync(sessionId, artifactId);
    }

    private record ReportPreparation(Report report, boolean shouldProcess) {}
}

