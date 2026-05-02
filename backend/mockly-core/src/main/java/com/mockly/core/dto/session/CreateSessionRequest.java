package com.mockly.core.dto.session;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;




public record CreateSessionRequest(
        


        @NotNull(message = "Interviewer ID is required")
        UUID interviewerId,

        



        OffsetDateTime scheduledAt
) {}

