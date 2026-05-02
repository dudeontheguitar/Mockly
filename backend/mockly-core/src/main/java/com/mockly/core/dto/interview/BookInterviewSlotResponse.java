package com.mockly.core.dto.interview;

import com.mockly.data.enums.InterviewSlotStatus;

import java.util.UUID;

public record BookInterviewSlotResponse(
        UUID slotId,
        UUID sessionId,
        InterviewSlotStatus status
) {}
