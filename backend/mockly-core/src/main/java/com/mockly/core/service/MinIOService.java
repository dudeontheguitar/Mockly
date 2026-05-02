package com.mockly.core.service;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;




@Service
@RequiredArgsConstructor
@Slf4j
public class MinIOService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name:mockly-artifacts}")
    private String bucketName;

    



    public void initializeBucket() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());

            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
                log.info("Created MinIO bucket: {}", bucketName);
            } else {
                log.info("MinIO bucket already exists: {}", bucketName);
            }
        } catch (ErrorResponseException | InsufficientDataException | InternalException |
                 InvalidKeyException | InvalidResponseException | IOException |
                 NoSuchAlgorithmException | ServerException | XmlParserException e) {
            log.error("Failed to initialize MinIO bucket: {}", bucketName, e);
            throw new RuntimeException("Failed to initialize MinIO bucket", e);
        }
    }

    






    public String generatePresignedUploadUrl(String objectName, int expiryTime) {
        try {
            String normalizedObjectName = normalizeObjectName(objectName);
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucketName)
                            .object(normalizedObjectName)
                            .expiry(expiryTime, TimeUnit.SECONDS)
                            .build()
            );
        } catch (ErrorResponseException | InsufficientDataException | InternalException |
                 InvalidKeyException | InvalidResponseException | IOException |
                 NoSuchAlgorithmException | ServerException | XmlParserException e) {
            log.error("Failed to generate pre-signed upload URL for object: {}", objectName, e);
            throw new RuntimeException("Failed to generate pre-signed upload URL", e);
        }
    }

    






    public String generatePresignedDownloadUrl(String objectName, int expiryTime) {
        try {
            String normalizedObjectName = normalizeObjectName(objectName);
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(normalizedObjectName)
                            .expiry(expiryTime, TimeUnit.SECONDS)
                            .build()
            );
        } catch (ErrorResponseException | InsufficientDataException | InternalException |
                 InvalidKeyException | InvalidResponseException | IOException |
                 NoSuchAlgorithmException | ServerException | XmlParserException e) {
            log.error("Failed to generate pre-signed download URL for object: {}", objectName, e);
            throw new RuntimeException("Failed to generate pre-signed download URL", e);
        }
    }

    





    public boolean objectExists(String objectName) {
        try {
            String normalizedObjectName = normalizeObjectName(objectName);
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(normalizedObjectName)
                    .build());
            return true;
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                return false;
            }
            log.error("Error checking object existence: {}", objectName, e);
            throw new RuntimeException("Failed to check object existence", e);
        } catch (InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException |
                 ServerException | XmlParserException e) {
            log.error("Error checking object existence: {}", objectName, e);
            throw new RuntimeException("Failed to check object existence", e);
        }
    }

    





    public StatObjectResponse getObjectMetadata(String objectName) {
        try {
            String normalizedObjectName = normalizeObjectName(objectName);
            return minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(normalizedObjectName)
                    .build());
        } catch (ErrorResponseException | InsufficientDataException | InternalException |
                 InvalidKeyException | InvalidResponseException | IOException |
                 NoSuchAlgorithmException | ServerException | XmlParserException e) {
            log.error("Failed to get object metadata for: {}", objectName, e);
            throw new RuntimeException("Failed to get object metadata", e);
        }
    }

    public String getBucketName() {
        return bucketName;
    }

    






    public String normalizeObjectName(String objectReference) {
        if (objectReference == null || objectReference.isBlank()) {
            throw new IllegalArgumentException("Object reference cannot be blank");
        }

        String normalized = objectReference.trim();

        if (normalized.startsWith("s3://")) {
            String withoutScheme = normalized.substring("s3://".length());
            int slashIndex = withoutScheme.indexOf('/');
            if (slashIndex >= 0 && slashIndex + 1 < withoutScheme.length()) {
                normalized = withoutScheme.substring(slashIndex + 1);
            } else {
                normalized = withoutScheme;
            }
        }

        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            try {
                URI uri = new URI(normalized);
                normalized = uri.getPath();
            } catch (URISyntaxException e) {
                log.warn("Failed to parse object URL '{}', using raw value", objectReference);
            }
        }

        normalized = normalized.replaceFirst("^/+", "");

        String bucketPrefix = bucketName + "/";
        if (normalized.startsWith(bucketPrefix)) {
            normalized = normalized.substring(bucketPrefix.length());
        }

        return normalized;
    }
}

