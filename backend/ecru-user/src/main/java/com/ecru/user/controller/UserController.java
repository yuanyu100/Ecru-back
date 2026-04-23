package com.ecru.user.controller;

import com.ecru.common.exception.BusinessException;
import com.ecru.common.result.ErrorCode;
import com.ecru.common.result.Result;
import com.ecru.common.util.UserContext;
import com.ecru.user.dto.UpdatePasswordDTO;
import com.ecru.user.dto.UpdateUserDTO;
import com.ecru.user.service.UserService;
import com.ecru.user.service.UserSettingsService;
import com.ecru.user.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "用户管理", description = "用户信息管理相关接口")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserSettingsService userSettingsService;

    @Operation(summary = "获取当前登录用户信息", description = "获取当前登录用户的详细信息")
    @GetMapping("/me")
    public Result<UserVO> getCurrentUser() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        UserVO userVO = userService.getCurrentUser(userId);
        return Result.success(userVO);
    }

    @Operation(summary = "根据ID获取用户信息", description = "根据用户ID获取用户详细信息")
    @GetMapping("/{userId}")
    public Result<UserVO> getUserById(
            @Parameter(description = "用户ID", required = true, example = "1")
            @PathVariable Long userId) {
        UserVO userVO = userService.getCurrentUser(userId);
        return Result.success(userVO);
    }

    @Operation(summary = "更新用户信息", description = "更新当前登录用户的基本信息")
    @PutMapping("/me")
    public Result<UserVO> updateUser(
            @Valid @RequestBody UpdateUserDTO request) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        UserVO userVO = userService.updateUser(userId, request);
        return Result.success("更新成功", userVO);
    }

    @Operation(summary = "修改密码", description = "修改当前登录用户的密码")
    @PutMapping("/me/password")
    public Result<String> updatePassword(
            @Valid @RequestBody UpdatePasswordDTO request) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        userService.updatePassword(userId, request);
        return Result.success("密码修改成功", null);
    }

    @Operation(summary = "更新头像", description = "更新当前登录用户的头像")
    @PutMapping("/me/avatar")
    public Result<UserVO> updateAvatar(
            @Parameter(description = "头像URL", required = true)
            @RequestParam String avatarUrl) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        UserVO userVO = userService.updateAvatar(userId, avatarUrl);
        return Result.success("头像更新成功", userVO);
    }

    @Operation(summary = "获取用户设置", description = "获取当前登录用户的偏好设置")
    @GetMapping("/settings")
    public Result<Map<String, String>> getUserSettings() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Map<String, String> settings = userSettingsService.getUserSettings(userId);
        return Result.success(settings);
    }

    @Operation(summary = "更新用户设置", description = "更新当前登录用户的偏好设置")
    @PutMapping("/settings")
    public Result<Map<String, String>> updateUserSettings(
            @RequestBody Map<String, String> settings) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        userSettingsService.updateUserSettings(userId, settings);
        return Result.success("设置更新成功", userSettingsService.getUserSettings(userId));
    }
}
