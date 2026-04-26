package com.ecru.user.controller;

import com.ecru.common.exception.BusinessException;
import com.ecru.common.result.ErrorCode;
import com.ecru.common.result.Result;
import com.ecru.common.util.UserContext;
import com.ecru.user.dto.HomePromptPdfPreviewDTO;
import com.ecru.user.dto.HomePromptSettingsDTO;
import com.ecru.user.dto.UpdatePasswordDTO;
import com.ecru.user.dto.UpdateUserDTO;
import com.ecru.user.service.HomePromptService;
import com.ecru.user.service.UserService;
import com.ecru.user.service.UserSettingsService;
import com.ecru.user.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "用户管理", description = "用户信息管理相关接口")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserSettingsService userSettingsService;
    private final HomePromptService homePromptService;

    @Operation(summary = "获取当前登录用户信息", description = "获取当前登录用户的详细信息")
    @GetMapping("/me")
    public Result<UserVO> getCurrentUser() {
        Long userId = currentUserId();
        return Result.success(userService.getCurrentUser(userId));
    }

    @Operation(summary = "根据ID获取用户信息", description = "根据用户ID获取用户详细信息")
    @GetMapping("/{userId}")
    public Result<UserVO> getUserById(
            @Parameter(description = "用户ID", required = true, example = "1")
            @PathVariable Long userId) {
        return Result.success(userService.getCurrentUser(userId));
    }

    @Operation(summary = "更新用户信息", description = "更新当前登录用户的基本信息")
    @PutMapping("/me")
    public Result<UserVO> updateUser(@Valid @RequestBody UpdateUserDTO request) {
        Long userId = currentUserId();
        return Result.success("更新成功", userService.updateUser(userId, request));
    }

    @Operation(summary = "修改密码", description = "修改当前登录用户的密码")
    @PutMapping("/me/password")
    public Result<String> updatePassword(@Valid @RequestBody UpdatePasswordDTO request) {
        Long userId = currentUserId();
        userService.updatePassword(userId, request);
        return Result.success("密码修改成功", null);
    }

    @Operation(summary = "更新头像", description = "更新当前登录用户的头像")
    @PutMapping("/me/avatar")
    public Result<UserVO> updateAvatar(
            @Parameter(description = "头像URL", required = true)
            @RequestParam String avatarUrl) {
        Long userId = currentUserId();
        return Result.success("头像更新成功", userService.updateAvatar(userId, avatarUrl));
    }

    @Operation(summary = "获取用户设置", description = "获取当前登录用户的偏好设置")
    @GetMapping("/settings")
    public Result<Map<String, String>> getUserSettings() {
        Long userId = currentUserId();
        return Result.success(userSettingsService.getUserSettings(userId));
    }

    @Operation(summary = "更新用户设置", description = "更新当前登录用户的偏好设置")
    @PutMapping("/settings")
    public Result<Map<String, String>> updateUserSettings(@RequestBody Map<String, String> settings) {
        Long userId = currentUserId();
        userSettingsService.updateUserSettings(userId, settings);
        return Result.success("设置更新成功", userSettingsService.getUserSettings(userId));
    }

    @Operation(summary = "获取首页提示语设置", description = "获取首页提示语条目、选中项和展示节奏")
    @GetMapping("/settings/home-prompts")
    public Result<HomePromptSettingsDTO> getHomePromptSettings() {
        Long userId = currentUserId();
        return Result.success(homePromptService.getHomePromptSettings(userId));
    }

    @Operation(summary = "更新首页提示语设置", description = "保存首页提示语条目、选中项和展示节奏")
    @PutMapping("/settings/home-prompts")
    public Result<HomePromptSettingsDTO> updateHomePromptSettings(@RequestBody HomePromptSettingsDTO request) {
        Long userId = currentUserId();
        return Result.success("首页提示语已更新", homePromptService.updateHomePromptSettings(userId, request));
    }

    @Operation(summary = "PDF 解析首页提示语预览", description = "上传 PDF，返回 AI 解析出的短提示语预览")
    @PostMapping("/settings/home-prompts/pdf-preview")
    public Result<HomePromptPdfPreviewDTO> previewHomePromptsFromPdf(
            @RequestParam("file") MultipartFile file) {
        Long userId = currentUserId();
        return Result.success(homePromptService.previewHomePromptsFromPdf(userId, file));
    }

    private Long currentUserId() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }
}
