package com.ecru.user.controller;

import com.ecru.user.dto.request.StylePreferenceFeedbackRequest;
import com.ecru.user.dto.request.StyleImageQueryRequest;
import com.ecru.user.dto.response.StyleImageVO;
import com.ecru.user.dto.response.StyleTagVO;
import com.ecru.user.dto.response.UserStyleProfileVO;
import com.ecru.user.service.StyleTagService;
import com.ecru.user.service.StyleImageService;
import com.ecru.user.service.UserStylePreferenceService;
import com.ecru.common.result.Result;
import com.ecru.common.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 风格偏好模块Controller
 * 处理风格标签、风格图片和用户偏好相关的请求
 */
@RestController
@RequestMapping("/api/v1/style-preferences")
@Tag(name = "风格偏好模块", description = "风格标签、图片和用户偏好管理接口")
public class StylePreferenceController {
    
    @Autowired
    private StyleTagService styleTagService;
    
    @Autowired
    private StyleImageService styleImageService;
    
    @Autowired
    private UserStylePreferenceService userStylePreferenceService;
    
    // ==================== 风格标签相关接口 ====================
    
    @Operation(summary = "获取所有风格标签", description = "获取系统中所有的风格标签")
    @GetMapping("/tags")
    public Result<List<StyleTagVO>> getAllTags() {
        List<StyleTagVO> tags = styleTagService.getAllTags();
        return Result.success(tags);
    }
    
    @Operation(summary = "根据分类获取风格标签", description = "根据指定的风格大类获取标签")
    @GetMapping("/tags/category/{category}")
    public Result<List<StyleTagVO>> getTagsByCategory(@PathVariable String category) {
        List<StyleTagVO> tags = styleTagService.getTagsByCategory(category);
        return Result.success(tags);
    }
    
    @Operation(summary = "获取预设风格标签", description = "获取系统预设的风格标签")
    @GetMapping("/tags/preset")
    public Result<List<StyleTagVO>> getPresetTags() {
        List<StyleTagVO> tags = styleTagService.getPresetTags();
        return Result.success(tags);
    }
    
    @Operation(summary = "获取风格标签分类", description = "获取所有风格标签的分类列表")
    @GetMapping("/tags/categories")
    public Result<List<String>> getTagCategories() {
        List<String> categories = styleTagService.getTagCategories();
        return Result.success(categories);
    }
    
    // ==================== 风格图片相关接口 ====================
    
    @Operation(summary = "获取风格图片列表", description = "获取风格图片，支持按分类筛选")
    @PostMapping("/images")
    public Result<List<StyleImageVO>> getStyleImages(@Valid @RequestBody StyleImageQueryRequest request) {
        List<StyleImageVO> images = styleImageService.getStyleImages(request);
        return Result.success(images);
    }
    
    @Operation(summary = "获取随机风格图片", description = "随机获取指定数量的风格图片")
    @GetMapping("/images/random")
    public Result<List<StyleImageVO>> getRandomStyleImages(@RequestParam(defaultValue = "20") Integer count) {
        List<StyleImageVO> images = styleImageService.getRandomStyleImages(count);
        return Result.success(images);
    }
    
    @Operation(summary = "根据分类获取风格图片", description = "根据指定分类获取风格图片")
    @GetMapping("/images/category/{category}")
    public Result<List<StyleImageVO>> getStyleImagesByCategory(@PathVariable String category, 
                                                              @RequestParam(defaultValue = "20") Integer count) {
        List<StyleImageVO> images = styleImageService.getStyleImagesByCategory(category, count);
        return Result.success(images);
    }
    
    @Operation(summary = "获取风格图片详情", description = "根据ID获取风格图片详情")
    @GetMapping("/images/{id}")
    public Result<StyleImageVO> getStyleImageById(@PathVariable Long id) {
        StyleImageVO image = styleImageService.getStyleImageById(id);
        return Result.success(image);
    }
    
    // ==================== 用户偏好相关接口 ====================
    
    @Operation(summary = "提交风格偏好反馈", description = "用户对风格图片的偏好反馈（like/dislike/skip）")
    @PostMapping("/feedback")
    public Result<Void> submitFeedback(@Valid @RequestBody StylePreferenceFeedbackRequest request) {
        Long userId = UserContext.getCurrentUserId();
        userStylePreferenceService.submitFeedback(userId, request);
        return Result.success();
    }
    
    @Operation(summary = "获取用户风格画像", description = "获取用户的风格偏好画像")
    @GetMapping("/profile")
    public Result<List<UserStyleProfileVO>> getUserStyleProfile() {
        Long userId = UserContext.getCurrentUserId();
        List<UserStyleProfileVO> profile = userStylePreferenceService.getUserStyleProfile(userId);
        return Result.success(profile);
    }
    
    @Operation(summary = "获取用户Top偏好", description = "获取用户最偏好的前N个风格标签")
    @GetMapping("/profile/top")
    public Result<List<UserStyleProfileVO>> getTopPreferences(@RequestParam(defaultValue = "10") Integer limit) {
        Long userId = UserContext.getCurrentUserId();
        List<UserStyleProfileVO> topPreferences = userStylePreferenceService.getTopPreferences(userId, limit);
        return Result.success(topPreferences);
    }
    
    @Operation(summary = "获取学习进度", description = "获取用户风格偏好学习的进度")
    @GetMapping("/progress")
    public Result<Integer> getLearningProgress() {
        Long userId = UserContext.getCurrentUserId();
        Integer progress = userStylePreferenceService.getLearningProgress(userId);
        return Result.success(progress);
    }
    
    @Operation(summary = "重置风格画像", description = "重置用户的风格偏好画像")
    @PostMapping("/profile/reset")
    public Result<Void> resetUserStyleProfile() {
        Long userId = UserContext.getCurrentUserId();
        userStylePreferenceService.resetUserStyleProfile(userId);
        return Result.success();
    }
}
