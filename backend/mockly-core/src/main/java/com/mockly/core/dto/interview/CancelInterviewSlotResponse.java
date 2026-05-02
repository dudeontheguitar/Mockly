package com.mockly.core.dto.interview;

import com.mockly.data.enums.InterviewSlotStatus;

import java.util.UUID;

public record CancelInterviewSlotResponse(
        UUID slotId,
        InterviewSlotStatus status
) {}
