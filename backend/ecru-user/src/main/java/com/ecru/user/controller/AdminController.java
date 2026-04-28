package com.ecru.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecru.common.exception.BusinessException;
import com.ecru.common.result.ErrorCode;
import com.ecru.common.result.Result;
import com.ecru.common.util.UserContext;
import com.ecru.user.converter.UserConverter;
import com.ecru.user.dto.UpdateUserStatusDTO;
import com.ecru.user.dto.UserQueryRequest;
import com.ecru.user.entity.User;
import com.ecru.user.mapper.UserMapper;
import com.ecru.user.service.UserLoginLogService;
import com.ecru.user.vo.UserLoginLogVO;
import com.ecru.user.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Tag(name = "管理员接口", description = "管理员用户与登录日志管理接口")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserMapper userMapper;
    private final UserLoginLogService userLoginLogService;

    @Operation(summary = "获取用户列表", description = "获取所有用户列表（管理员权限）")
    @GetMapping("/users")
    public Result<Page<UserVO>> getUserList(UserQueryRequest request) {
        checkAdminPermission();

        long current = request.getPage() != null && request.getPage() > 0 ? request.getPage() : 1L;
        long size = request.getSize() != null && request.getSize() > 0 ? request.getSize() : 10L;
        long offset = (current - 1) * size;

        long total = userMapper.countAdminUsers(request.getKeyword(), request.getStatus());
        List<UserVO> records = userMapper.selectAdminUsers(request.getKeyword(), request.getStatus(), offset, size)
                .stream()
                .map(UserConverter.INSTANCE::toUserVO)
                .toList();

        Page<UserVO> voPage = new Page<>(current, size, total);
        voPage.setRecords(records);
        return Result.success(voPage);
    }

    @Operation(summary = "更新用户状态", description = "启用或禁用用户账号")
    @PutMapping("/users/{userId}/status")
    public Result<Void> updateUserStatus(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusDTO request) {

        checkAdminPermission();

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        user.setStatus(request.getStatus());
        userMapper.updateById(user);
        String statusText = request.getStatus() == 1 ? "启用" : "禁用";
        return Result.success("用户" + statusText + "成功", null);
    }

    @Operation(summary = "获取登录日志", description = "分页查询登录成功与失败日志")
    @GetMapping("/login-logs")
    public Result<Page<UserLoginLogVO>> getLoginLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer loginStatus,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "20") Long size) {

        checkAdminPermission();
        return Result.success(userLoginLogService.getAdminLogs(userId, loginStatus, page, size));
    }

    private void checkAdminPermission() {
        if (!UserContext.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        if (!UserContext.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
