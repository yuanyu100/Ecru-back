package com.ecru.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecru.common.exception.BusinessException;
import com.ecru.common.result.ErrorCode;
import com.ecru.common.result.Result;
import com.ecru.common.service.storage.ImageStorageService;
import com.ecru.common.util.UserContext;
import com.ecru.user.dto.AdminStyleImageUpsertRequest;
import com.ecru.user.entity.StyleImage;
import com.ecru.user.mapper.StyleImageMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/style-images")
@RequiredArgsConstructor
@Tag(name = "后台风格图片管理", description = "管理用户偏好学习页使用的风格图片")
public class AdminStyleImageController {

    private final StyleImageMapper styleImageMapper;
    private final ImageStorageService imageStorageService;

    @GetMapping
    @Operation(summary = "分页查询风格图片")
    public Result<Page<StyleImage>> listStyleImages(
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "20") Long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String styleCategory,
            @RequestParam(required = false) Boolean active) {
        requireAdmin();

        QueryWrapper<StyleImage> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query.like("title", keyword)
                    .or()
                    .like("source", keyword)
                    .or()
                    .like("style_category", keyword));
        }
        if (StringUtils.hasText(styleCategory)) {
            wrapper.eq("style_category", styleCategory.trim());
        }
        if (active != null) {
            wrapper.eq("is_active", active);
        }
        wrapper.orderByDesc("updated_at").orderByDesc("id");

        Page<StyleImage> result = styleImageMapper.selectPage(new Page<>(page, size), wrapper);
        return Result.success(result);
    }

    @PostMapping
    @Operation(summary = "新增风格图片")
    public Result<StyleImage> createStyleImage(@Valid @RequestBody AdminStyleImageUpsertRequest request) {
        requireAdmin();

        StyleImage styleImage = new StyleImage();
        fillStyleImage(styleImage, request);
        styleImageMapper.insert(styleImage);
        return Result.success("风格图片创建成功", styleImage);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新风格图片")
    public Result<StyleImage> updateStyleImage(@PathVariable Long id,
                                               @Valid @RequestBody AdminStyleImageUpsertRequest request) {
        requireAdmin();

        StyleImage styleImage = styleImageMapper.selectById(id);
        if (styleImage == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "风格图片不存在");
        }

        fillStyleImage(styleImage, request);
        styleImageMapper.updateById(styleImage);
        return Result.success("风格图片更新成功", styleImage);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除风格图片")
    public Result<Void> deleteStyleImage(@PathVariable Long id) {
        requireAdmin();

        StyleImage styleImage = styleImageMapper.selectById(id);
        if (styleImage == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "风格图片不存在");
        }

        styleImageMapper.deleteById(id);
        return Result.success("风格图片删除成功", null);
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @Operation(summary = "上传风格图片")
    public Result<String> uploadStyleImage(@RequestParam("file") MultipartFile file) {
        requireAdmin();
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "上传文件不能为空");
        }

        String imageUrl = imageStorageService.uploadImage(file, UserContext.getCurrentUserId());
        return Result.success("图片上传成功", imageUrl);
    }

    private void fillStyleImage(StyleImage styleImage, AdminStyleImageUpsertRequest request) {
        styleImage.setImageUrl(request.getImageUrl().trim());
        styleImage.setTitle(normalize(request.getTitle()));
        styleImage.setSource(normalize(request.getSource()));
        styleImage.setSourceUrl(normalize(request.getSourceUrl()));
        styleImage.setPrice(request.getPrice());
        styleImage.setStyleCategory(request.getStyleCategory().trim());
        styleImage.setIsActive(request.getIsActive() == null || request.getIsActive());
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void requireAdmin() {
        if (!UserContext.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (!UserContext.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
