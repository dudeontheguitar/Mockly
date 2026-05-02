package com.mockly.core.service;

import com.mockly.core.dto.settings.SettingsResponse;
import com.mockly.core.dto.settings.UpdateSettingsRequest;
import com.mockly.core.exception.ResourceNotFoundException;
import com.mockly.data.entity.User;
import com.mockly.data.entity.UserSettings;
import com.mockly.data.repository.UserRepository;
import com.mockly.data.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final UserRepository userRepository;
    private final UserSettingsRepository settingsRepository;

    @Transactional
    public SettingsResponse getSettings(UUID userId) {
        return toResponse(getOrCreate(userId));
    }

    @Transactional
    public SettingsResponse updateSettings(UUID userId, UpdateSettingsRequest request) {
        UserSettings settings = getOrCreate(userId);

        if (request.language() != null) {
            settings.setLanguage(request.language());
        }
        if (request.theme() != null) {
            settings.setTheme(request.theme());
        }
        if (request.notificationsEnabled() != null) {
            settings.setNotificationsEnabled(request.notificationsEnabled());
        }
        if (request.emailNotificationsEnabled() != null) {
            settings.setEmailNotificationsEnabled(request.emailNotificationsEnabled());
        }
        if (request.pushNotificationsEnabled() != null) {
            settings.setPushNotificationsEnabled(request.pushNotificationsEnabled());
        }
        if (request.interviewRemindersEnabled() != null) {
            settings.setInterviewRemindersEnabled(request.interviewRemindersEnabled());
        }
        if (request.messageNotificationsEnabled() != null) {
            settings.setMessageNotificationsEnabled(request.messageNotificationsEnabled());
        }
        if (request.timezone() != null) {
            settings.setTimezone(request.timezone());
        }

        return toResponse(settingsRepository.save(settings));
    }

    private UserSettings getOrCreate(UUID userId) {
        return settingsRepository.findById(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
                    return settingsRepository.save(UserSettings.builder()
                            .user(user)
                            .build());
                });
    }

    private SettingsResponse toResponse(UserSettings settings) {
        return new SettingsResponse(
                settings.getUserId(),
                settings.getLanguage(),
                settings.getTheme(),
                settings.getNotificationsEnabled(),
                settings.getEmailNotificationsEnabled(),
                settings.getPushNotificationsEnabled(),
                settings.getInterviewRemindersEnabled(),
                settings.getMessageNotificationsEnabled(),
                settings.getTimezone()
        );
    }
}
