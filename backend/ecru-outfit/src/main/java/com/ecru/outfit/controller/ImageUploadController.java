package com.ecru.outfit.controller;

import com.ecru.common.result.Result;
import com.ecru.common.service.storage.ImageStorageService;
import com.ecru.common.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/images")
@Tag(name = "图片上传", description = "图片上传相关接口")
public class ImageUploadController {

    @Autowired
    private ImageStorageService imageStorageService;

    /**
     * 上传图片
     * @param file 图片文件
     * @return 图片访问URL
     */
    @PostMapping("/upload")
    @Operation(summary = "上传图片", description = "上传衣物图片到 MinIO")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            Long userId = UserContext.getCurrentUserId();
            String imageUrl = imageStorageService.uploadImage(file, userId);
            return Result.success(imageUrl);
        } catch (Exception e) {
            log.error("上传图片失败: {}", e.getMessage());
            return Result.error(500, "上传图片失败");
        }
    }

    /**
     * 上传图片（无需登录）
     * @param file 图片文件
     * @return 图片访问URL
     */
    @PostMapping("/upload/public")
    @Operation(summary = "上传图片（无需登录）", description = "上传图片到 MinIO，无需登录")
    public Result<String> uploadImagePublic(@RequestParam("file") MultipartFile file) {
        try {
            String imageUrl = imageStorageService.uploadImage(file);
            return Result.success(imageUrl);
        } catch (Exception e) {
            log.error("上传图片失败: {}", e.getMessage());
            return Result.error(500, "上传图片失败");
        }
    }

}