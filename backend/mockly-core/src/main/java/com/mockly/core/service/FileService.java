package com.mockly.core.service;

import com.mockly.core.dto.file.AvatarUploadRequest;
import com.mockly.core.dto.file.AvatarUploadResponse;
import com.mockly.core.dto.file.CompleteAvatarUploadResponse;
import com.mockly.core.exception.BadRequestException;
import com.mockly.core.exception.ForbiddenException;
import com.mockly.core.exception.ResourceNotFoundException;
import com.mockly.data.entity.FileUpload;
import com.mockly.data.entity.Profile;
import com.mockly.data.enums.FileUploadPurpose;
import com.mockly.data.enums.FileUploadStatus;
import com.mockly.data.repository.FileUploadRepository;
import com.mockly.data.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private static final long MAX_AVATAR_SIZE_BYTES = 5 * 1024 * 1024;
    private static final List<String> ALLOWED_AVATAR_TYPES = List.of("image/jpeg", "image/png", "image/webp");
    private static final List<String> ALLOWED_AVATAR_EXTENSIONS = List.of(".jpg", ".jpeg", ".png", ".webp");

    private final FileUploadRepository fileUploadRepository;
    private final ProfileRepository profileRepository;
    private final MinIOService minIOService;

    @Transactional
    public AvatarUploadResponse requestAvatarUpload(UUID userId, AvatarUploadRequest request) {
        validateAvatar(request);

        UUID fileId = UUID.randomUUID();
        String objectName = "users/%s/avatar/%s/%s".formatted(
                userId,
                fileId,
                sanitizeFileName(request.fileName())
        );

        FileUpload upload = FileUpload.builder()
                .id(fileId)
                .userId(userId)
                .purpose(FileUploadPurpose.AVATAR)
                .objectName(objectName)
                .fileName(request.fileName())
                .fileSizeBytes(request.fileSizeBytes())
                .contentType(request.contentType())
                .status(FileUploadStatus.PENDING)
                .build();

        fileUploadRepository.save(upload);

        int expirySeconds = 3600;
        return new AvatarUploadResponse(
                fileId,
                minIOService.generatePresignedUploadUrl(objectName, expirySeconds),
                objectName,
                expirySeconds
        );
    }

    @Transactional
    public CompleteAvatarUploadResponse completeAvatarUpload(UUID userId, UUID fileId) {
        FileUpload upload = fileUploadRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File upload not found: " + fileId));
        if (!upload.getUserId().equals(userId)) {
            throw new ForbiddenException("You do not have access to this upload");
        }
        if (upload.getPurpose() != FileUploadPurpose.AVATAR) {
            throw new BadRequestException("File upload is not an avatar upload");
        }
        if (!minIOService.objectExists(upload.getObjectName())) {
            throw new BadRequestException("File was not uploaded to storage");
        }

        upload.setStatus(FileUploadStatus.COMPLETED);
        upload.setCompletedAt(OffsetDateTime.now());
        fileUploadRepository.save(upload);

        String avatarUrl = minIOService.generatePresignedDownloadUrl(upload.getObjectName(), 604800);
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + userId));
        profile.setAvatarUrl(avatarUrl);
        profileRepository.save(profile);

        return new CompleteAvatarUploadResponse(avatarUrl);
    }

    private void validateAvatar(AvatarUploadRequest request) {
        if (request.fileSizeBytes() > MAX_AVATAR_SIZE_BYTES) {
            throw new BadRequestException("Avatar size exceeds maximum allowed size of 5 MB");
        }

        String fileName = request.fileName().toLowerCase();
        boolean extensionAllowed = ALLOWED_AVATAR_EXTENSIONS.stream().anyMatch(fileName::endsWith);
        if (!extensionAllowed) {
            throw new BadRequestException("Avatar file type is not allowed");
        }

        if (!ALLOWED_AVATAR_TYPES.contains(request.contentType().toLowerCase())) {
            throw new BadRequestException("Avatar content type is not allowed");
        }
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
