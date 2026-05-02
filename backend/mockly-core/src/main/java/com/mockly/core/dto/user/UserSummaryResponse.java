package com.mockly.core.dto.user;

import com.mockly.data.entity.Profile;

import java.util.UUID;

public record UserSummaryResponse(
        UUID id,
        String displayName,
        String email,
        String avatarUrl,
        Profile.ProfileRole role,
        String level
) {}
