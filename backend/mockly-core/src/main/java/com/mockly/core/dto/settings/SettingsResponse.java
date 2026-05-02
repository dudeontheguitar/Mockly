package com.mockly.core.dto.settings;

import com.mockly.data.enums.ThemePreference;

import java.util.UUID;

public record SettingsResponse(
        UUID userId,
        String language,
        ThemePreference theme,
        Boolean notificationsEnabled,
        Boolean emailNotificationsEnabled,
        Boolean pushNotificationsEnabled,
        Boolean interviewRemindersEnabled,
        Boolean messageNotificationsEnabled,
        String timezone
) {}
