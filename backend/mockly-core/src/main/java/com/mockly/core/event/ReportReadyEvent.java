package com.mockly.core.event;

import com.mockly.core.dto.report.ReportResponse;

import java.util.UUID;

/**
 * Event published when report processing is completed successfully.
 */
public record ReportReadyEvent(
        UUID sessionId,
        ReportResponse report
) {}
