package com.mockly.api.controller;

import com.mockly.core.dto.report.ReportResponse;
import com.mockly.core.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/sessions/{sessionId}/report")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Report generation and retrieval endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/trigger")
    @Operation(
            summary = "Trigger report generation manually",
            description = "Starts async report generation for a session. Requires an uploaded audio artifact. " +
                    "Use this endpoint for manual retries."
    )
    public ResponseEntity<ReportResponse> triggerReport(
            Authentication authentication,
            @PathVariable UUID sessionId) {
        UUID userId = UUID.fromString(authentication.getName());
        ReportResponse response = reportService.triggerReportGeneration(sessionId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(
            summary = "Get report",
            description = "Returns report status and data for a session. Status can be PENDING, PROCESSING, READY, or FAILED. " +
                    "If no report exists yet but audio is already uploaded, processing starts automatically."
    )
    public ResponseEntity<ReportResponse> getReport(
            Authentication authentication,
            @PathVariable UUID sessionId) {
        UUID userId = UUID.fromString(authentication.getName());
        ReportResponse response = reportService.getReport(sessionId, userId);
        return ResponseEntity.ok(response);
    }
}

