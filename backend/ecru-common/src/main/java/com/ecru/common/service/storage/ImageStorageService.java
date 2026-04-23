package com.ecru.common.service.storage;

import com.ecru.common.config.MinioConfig;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Part;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
public class ImageStorageService {

    private final MinioConfig minioConfig;
    private volatile MinioClient minioClient;

    public ImageStorageService(MinioConfig minioConfig) {
        this.minioConfig = minioConfig;
    }

    @PostConstruct
    public void init() {
        try {
            ensureClient();
            ensureBucket();
            log.info("MinIO client initialized");
        } catch (Exception e) {
            log.warn("MinIO is not ready during startup: {}", e.getMessage());
        }
    }

    public String uploadImage(MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传图片失败：文件为空");
        }

        try {
            return uploadStream(file.getInputStream(), file.getOriginalFilename(), file.getContentType(), file.getSize(), userId);
        } catch (Exception e) {
            log.error("Upload image failed", e);
            throw new RuntimeException("上传图片失败", e);
        }
    }

    public String uploadImage(Part file, Long userId) {
        if (file == null || file.getSize() <= 0) {
            throw new RuntimeException("上传图片失败：文件为空");
        }

        try {
            return uploadStream(file.getInputStream(), file.getSubmittedFileName(), file.getContentType(), file.getSize(), userId);
        } catch (Exception e) {
            log.error("Upload image failed", e);
            throw new RuntimeException("上传图片失败", e);
        }
    }

    public String uploadImage(MultipartFile file) {
        return uploadImage(file, 0L);
    }

    public String uploadBase64Image(String base64Data, String filename, String contentType, Long userId) {
        if (base64Data == null || base64Data.isBlank()) {
            throw new RuntimeException("上传图片失败：缺少图片内容");
        }

        try {
            String rawData = base64Data.contains(",")
                    ? base64Data.substring(base64Data.indexOf(',') + 1)
                    : base64Data;
            byte[] bytes = Base64.getDecoder().decode(rawData);
            return uploadStream(new java.io.ByteArrayInputStream(bytes), filename, contentType, bytes.length, userId);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("上传图片失败：Base64 内容无效", e);
        } catch (Exception e) {
            log.error("Upload base64 image failed", e);
            throw new RuntimeException("上传图片失败", e);
        }
    }

    private synchronized void ensureClient() {
        if (minioClient != null) {
            return;
        }

        minioClient = MinioClient.builder()
                .endpoint(minioConfig.getEndpoint())
                .credentials(minioConfig.getAccessKey(), minioConfig.getSecretKey())
                .build();
    }

    private void ensureBucket() throws Exception {
        boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(minioConfig.getBucketName())
                .build());

        if (!bucketExists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .build());
            log.info("Created MinIO bucket: {}", minioConfig.getBucketName());
        }
    }

    private String uploadStream(InputStream inputStream,
                                String originalFilename,
                                String contentType,
                                long size,
                                Long userId) throws Exception {
        ensureClient();
        ensureBucket();

        String safeFilename = originalFilename == null ? "image.jpg" : originalFilename;
        String extension = safeFilename.contains(".")
                ? safeFilename.substring(safeFilename.lastIndexOf("."))
                : ".jpg";
        String fileName = userId + "/" + UUID.randomUUID() + extension;

        minioClient.putObject(PutObjectArgs.builder()
                .bucket(minioConfig.getBucketName())
                .object(fileName)
                .stream(inputStream, size, -1)
                .contentType(contentType == null ? "application/octet-stream" : contentType)
                .build());

        String url = minioConfig.getDomain() + "/" + minioConfig.getBucketName() + "/" + fileName;
        log.info("Uploaded image to MinIO: {}", url);
        return url;
    }
}
