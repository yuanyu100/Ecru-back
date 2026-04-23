package com.ecru.common.service.storage;

import com.ecru.common.config.MinioConfig;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.util.UUID;

/**
 * 图片存储服务
 */
@Slf4j
@Service
public class ImageStorageService {

    @Autowired
    private MinioConfig minioConfig;

    private MinioClient minioClient;

    /**
     * 初始化 MinIO 客户端
     */
    @PostConstruct
    public void init() {
        try {
            // 初始化 MinIO 客户端
            minioClient = MinioClient.builder()
                    .endpoint(minioConfig.getEndpoint())
                    .credentials(minioConfig.getAccessKey(), minioConfig.getSecretKey())
                    .build();

            // 检查桶是否存在，不存在则创建
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .build());
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(minioConfig.getBucketName())
                        .build());
                log.info("MinIO 桶创建成功: {}", minioConfig.getBucketName());
            }
            log.info("MinIO 客户端初始化成功");
        } catch (Exception e) {
            log.error("MinIO 客户端初始化失败: {}", e.getMessage());
        }
    }

    /**
     * 上传图片
     * @param file 图片文件
     * @param userId 用户ID
     * @return 图片访问URL
     */
    public String uploadImage(MultipartFile file, Long userId) {
        try {
            // 生成唯一文件名
            String fileName = userId + "/" + UUID.randomUUID().toString() +
                    file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));

            // 上传文件
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(fileName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            // 返回访问URL
            String url = minioConfig.getDomain() + "/" + minioConfig.getBucketName() + "/" + fileName;
            log.info("上传图片成功，访问URL: {}", url);
            return url;
        } catch (Exception e) {
            log.error("上传图片失败: {}", e.getMessage());
            throw new RuntimeException("上传图片失败", e);
        }
    }

    /**
     * 上传图片（无用户ID）
     * @param file 图片文件
     * @return 图片访问URL
     */
    public String uploadImage(MultipartFile file) {
        return uploadImage(file, 0L);
    }

}