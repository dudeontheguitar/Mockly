package com.mockly.core.dto.interview;

import java.util.List;

public record InterviewSlotListResponse(
        List<InterviewSlotResponse> items
) {}
