package com.mockly.core.dto.interview;

import com.mockly.core.dto.user.UserSummaryResponse;
import com.mockly.data.enums.InterviewSlotStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InterviewSlotResponse(
        UUID id,
        String title,
        String company,
        String location,
        String description,
        OffsetDateTime scheduledAt,
        Integer durationMinutes,
        InterviewSlotStatus status,
        UserSummaryResponse interviewer,
        UserSummaryResponse bookedBy,
        UUID sessionId
) {}
