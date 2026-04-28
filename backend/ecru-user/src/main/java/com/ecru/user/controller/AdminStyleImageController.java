package com.ecru.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecru.common.exception.BusinessException;
import com.ecru.common.result.ErrorCode;
import com.ecru.common.result.Result;
import com.ecru.common.service.storage.ImageStorageService;
import com.ecru.common.util.UserContext;
import com.ecru.user.dto.AdminStyleImageUpsertRequest;
import com.ecru.user.dto.response.StyleImageVO;
import com.ecru.user.dto.response.StyleTagVO;
import com.ecru.user.entity.StyleImage;
import com.ecru.user.entity.StyleImageTag;
import com.ecru.user.mapper.StyleImageMapper;
import com.ecru.user.mapper.StyleImageTagMapper;
import com.ecru.user.service.StyleImageService;
import com.ecru.user.service.StyleTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/style-images")
@RequiredArgsConstructor
@Tag(name = "后台风格图片管理", description = "管理用户偏好学习页使用的风格图片")
public class AdminStyleImageController {

    private static final String DEFAULT_SOURCE = "手工标注";

    private final StyleImageMapper styleImageMapper;
    private final StyleImageTagMapper styleImageTagMapper;
    private final StyleImageService styleImageService;
    private final StyleTagService styleTagService;
    private final ImageStorageService imageStorageService;

    @GetMapping
    @Operation(summary = "分页查询风格图片")
    public Result<Page<StyleImageVO>> listStyleImages(
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
        Page<StyleImageVO> response = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        response.setPages(result.getPages());
        response.setRecords(result.getRecords().stream()
                .map(item -> styleImageService.getStyleImageById(item.getId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        return Result.success(response);
    }

    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    @Operation(summary = "新增风格图片")
    public Result<StyleImageVO> createStyleImage(@Valid @RequestBody AdminStyleImageUpsertRequest request) {
        requireAdmin();

        StyleImage styleImage = new StyleImage();
        fillStyleImage(styleImage, request);
        styleImageMapper.insert(styleImage);
        syncStyleTags(styleImage.getId(), request.getStyleTagIds());
        return Result.success("风格图片创建成功", styleImageService.getStyleImageById(styleImage.getId()));
    }

    @PutMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    @Operation(summary = "更新风格图片")
    public Result<StyleImageVO> updateStyleImage(@PathVariable Long id,
                                                 @Valid @RequestBody AdminStyleImageUpsertRequest request) {
        requireAdmin();

        StyleImage styleImage = styleImageMapper.selectById(id);
        if (styleImage == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "风格图片不存在");
        }

        fillStyleImage(styleImage, request);
        styleImageMapper.updateById(styleImage);
        syncStyleTags(styleImage.getId(), request.getStyleTagIds());
        return Result.success("风格图片更新成功", styleImageService.getStyleImageById(styleImage.getId()));
    }

    @DeleteMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    @Operation(summary = "删除风格图片")
    public Result<Void> deleteStyleImage(@PathVariable Long id) {
        requireAdmin();

        StyleImage styleImage = styleImageMapper.selectById(id);
        if (styleImage == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "风格图片不存在");
        }

        styleImageTagMapper.deleteByImageId(id);
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
        String imageUrl = normalize(request.getImageUrl());
        if (!StringUtils.hasText(imageUrl)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "图片地址不能为空");
        }

        List<StyleTagVO> selectedTags = resolveSelectedTags(request.getStyleTagIds());
        String styleCategorySummary = buildStyleCategorySummary(selectedTags, request.getStyleCategory());
        if (!StringUtils.hasText(styleCategorySummary)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请至少选择一个风格分类");
        }

        styleImage.setImageUrl(imageUrl);
        styleImage.setTitle(defaultIfBlank(request.getTitle(), styleCategorySummary));
        styleImage.setSource(defaultIfBlank(request.getSource(), DEFAULT_SOURCE));
        styleImage.setSourceUrl(normalize(request.getSourceUrl()));
        styleImage.setPrice(request.getPrice());
        styleImage.setStyleCategory(styleCategorySummary);
        styleImage.setIsActive(request.getIsActive() == null || request.getIsActive());
    }

    private List<StyleTagVO> resolveSelectedTags(List<Long> styleTagIds) {
        if (styleTagIds == null || styleTagIds.isEmpty()) {
            return Collections.emptyList();
        }

        return styleTagIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(styleTagService::getTagById)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private String buildStyleCategorySummary(List<StyleTagVO> selectedTags, String fallback) {
        if (!selectedTags.isEmpty()) {
            return selectedTags.stream()
                    .map(StyleTagVO::getName)
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .collect(Collectors.collectingAndThen(
                            Collectors.toCollection(LinkedHashSet::new),
                            values -> String.join(" / ", values)));
        }
        return normalize(fallback);
    }

    private void syncStyleTags(Long imageId, List<Long> styleTagIds) {
        styleImageTagMapper.deleteByImageId(imageId);
        if (styleTagIds == null || styleTagIds.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<StyleImageTag> relations = resolveSelectedTags(styleTagIds).stream()
                .map(StyleTagVO::getId)
                .filter(Objects::nonNull)
                .map(styleTagId -> {
                    StyleImageTag relation = new StyleImageTag();
                    relation.setImageId(imageId);
                    relation.setStyleTagId(styleTagId);
                    relation.setConfidence(BigDecimal.ONE);
                    relation.setCreatedAt(now);
                    return relation;
                })
                .collect(Collectors.toList());

        if (!relations.isEmpty()) {
            styleImageTagMapper.batchInsert(relations);
        }
    }

    private String defaultIfBlank(String value, String fallback) {
        String normalized = normalize(value);
        return StringUtils.hasText(normalized) ? normalized : fallback;
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
