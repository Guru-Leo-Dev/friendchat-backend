package com.friendchat.common.util;

import com.friendchat.common.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class S3Service {

    private final String bucket;
    private final String region;
    private final String accessKey;
    private final String secretKey;

    private static final long        MAX_AVATAR_BYTES = 5L  * 1024 * 1024;
    private static final long        MAX_FILE_BYTES   = 25L * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGES   =
            Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

    public S3Service(
            @Value("${app.aws.region:us-east-1}")     String region,
            @Value("${app.aws.s3-bucket:friendchat-media}") String bucket,
            @Value("${app.aws.access-key:}")          String accessKey,
            @Value("${app.aws.secret-key:}")          String secretKey) {
        this.region    = region;
        this.bucket    = bucket;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        // S3Client is created lazily on first use — not at startup
        // This prevents startup failure when AWS credentials are not configured
    }

    // ── Lazy S3 client — built only when actually needed ─────────────────────
    private S3Client buildClient() {
        AwsCredentialsProvider creds = (accessKey == null || accessKey.isBlank())
                ? DefaultCredentialsProvider.create()
                : StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey));
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(creds)
                .build();
    }

    public String uploadAvatar(UUID userId, MultipartFile file) {
        validateImage(file, MAX_AVATAR_BYTES);
        return upload("avatars/" + userId + "/" + UUID.randomUUID() + ext(file.getOriginalFilename()), file);
    }

    public String uploadMessageFile(UUID chatId, UUID senderId, MultipartFile file) {
        if (file.getSize() > MAX_FILE_BYTES) throw new BadRequestException("File exceeds 25 MB limit");
        return upload("messages/" + chatId + "/" + senderId + "/" + UUID.randomUUID() + ext(file.getOriginalFilename()), file);
    }

    private String upload(String key, MultipartFile file) {
        try {
            S3Client s3 = buildClient();
            s3.putObject(PutObjectRequest.builder()
                    .bucket(bucket).key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize()).build(),
                    RequestBody.fromBytes(file.getBytes()));
            String url = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
            log.info("Uploaded to S3: {}", url);
            return url;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file", e);
        } catch (S3Exception e) {
            log.error("S3 upload failed: {}", e.getMessage());
            throw new RuntimeException("File upload failed: " + e.getMessage(), e);
        }
    }

    private void validateImage(MultipartFile file, long maxBytes) {
        if (file.isEmpty()) throw new BadRequestException("File is empty");
        if (file.getSize() > maxBytes) throw new BadRequestException("Image exceeds size limit");
        String ct = file.getContentType();
        if (ct == null || !ALLOWED_IMAGES.contains(ct))
            throw new BadRequestException("Unsupported image type: " + ct);
    }

    private String ext(String name) {
        return (name != null && name.contains(".")) ? name.substring(name.lastIndexOf('.')) : "";
    }
}
