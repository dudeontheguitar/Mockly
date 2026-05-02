package com.mockly.core.dto.settings;

import com.mockly.data.enums.ThemePreference;
import jakarta.validation.constraints.Size;

public record UpdateSettingsRequest(
        @Size(max = 10, message = "Language must not exceed 10 characters")
        String language,
        ThemePreference theme,
        Boolean notificationsEnabled,
        Boolean emailNotificationsEnabled,
        Boolean pushNotificationsEnabled,
        Boolean interviewRemindersEnabled,
        Boolean messageNotificationsEnabled,
        @Size(max = 100, message = "Timezone must not exceed 100 characters")
        String timezone
) {}
