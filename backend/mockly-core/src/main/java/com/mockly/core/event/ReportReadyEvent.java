package com.mockly.core.event;

import com.mockly.core.dto.report.ReportResponse;

import java.util.UUID;




public record ReportReadyEvent(
        UUID sessionId,
        ReportResponse report
) {}
