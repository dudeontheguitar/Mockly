package com.mockly.api.controller;

import com.mockly.core.dto.settings.SettingsResponse;
import com.mockly.core.dto.settings.UpdateSettingsRequest;
import com.mockly.core.service.SettingsService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/settings/me")
@RequiredArgsConstructor
@Tag(name = "Settings", description = "Current user settings")
@SecurityRequirement(name = "bearerAuth")
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping
    public ResponseEntity<SettingsResponse> getSettings(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(settingsService.getSettings(userId));
    }

    @PatchMapping
    public ResponseEntity<SettingsResponse> updateSettings(
            Authentication authentication,
            @Valid @RequestBody UpdateSettingsRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(settingsService.updateSettings(userId, request));
    }
}
