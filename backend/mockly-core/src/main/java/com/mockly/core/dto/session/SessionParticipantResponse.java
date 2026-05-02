package com.mockly.core.dto.session;

import com.mockly.data.enums.ParticipantRole;

import java.time.OffsetDateTime;
import java.util.UUID;




public record SessionParticipantResponse(
        UUID id,
        UUID userId,
        String userDisplayName,
        String userEmail,
        String userAvatarUrl,
        ParticipantRole roleInSession,
        OffsetDateTime joinedAt,
        OffsetDateTime leftAt
) {}

