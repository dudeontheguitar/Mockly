package com.mockly.core.dto.chat;

import com.mockly.data.entity.Profile;

import java.util.UUID;

public record ConversationParticipantResponse(
        UUID id,
        String displayName,
        String email,
        String avatarUrl,
        Profile.ProfileRole role
) {}
