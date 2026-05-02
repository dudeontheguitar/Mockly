package com.mockly.core.dto.user;

import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateProfileRequest(
        @Size(max = 100, message = "Display name must not exceed 100 characters")
        String displayName,

        @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
        String avatarUrl,

        @Size(max = 50, message = "Level must not exceed 50 characters")
        String level,

        List<String> skills,

        @Size(max = 5000, message = "Bio must not exceed 5000 characters")
        String bio,

        @Size(max = 100, message = "Location must not exceed 100 characters")
        String location
) {}

