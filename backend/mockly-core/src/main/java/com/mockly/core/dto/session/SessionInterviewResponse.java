package com.mockly.core.dto.session;

import java.util.UUID;

public record SessionInterviewResponse(
        UUID slotId,
        String title,
        String company,
        String location,
        String description,
        Integer durationMinutes
) {}
