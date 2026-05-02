package com.mockly.api.controller;

import com.mockly.core.dto.file.AvatarUploadRequest;
import com.mockly.core.dto.file.AvatarUploadResponse;
import com.mockly.core.dto.file.CompleteAvatarUploadRequest;
import com.mockly.core.dto.file.CompleteAvatarUploadResponse;
import com.mockly.core.service.FileService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/files/avatar")
@RequiredArgsConstructor
@Tag(name = "Files", description = "File upload endpoints")
@SecurityRequirement(name = "bearerAuth")
public class FileController {

    private final FileService fileService;

    @PostMapping("/request-upload")
    public ResponseEntity<AvatarUploadResponse> requestAvatarUpload(
            Authentication authentication,
            @Valid @RequestBody AvatarUploadRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(fileService.requestAvatarUpload(userId, request));
    }

    @PostMapping("/complete")
    public ResponseEntity<CompleteAvatarUploadResponse> completeAvatarUpload(
            Authentication authentication,
            @Valid @RequestBody CompleteAvatarUploadRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(fileService.completeAvatarUpload(userId, request.fileId()));
    }
}
