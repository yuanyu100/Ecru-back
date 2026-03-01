package com.ecru.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecru.common.exception.BusinessException;
import com.ecru.common.result.ErrorCode;
import com.ecru.common.result.Result;
import com.ecru.common.util.UserContext;
import com.ecru.user.dto.UpdateUserStatusDTO;
import com.ecru.user.dto.UserQueryRequest;
import com.ecru.user.entity.User;
import com.ecru.user.mapper.UserMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;



@Slf4j
@Tag(name = "管理员接口", description = "管理员用户管理相关接口")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserMapper userMapper;

    @Operation(summary = "获取用户列表", description = "获取所有用户列表（管理员权限）")
    @GetMapping("/users")
    public Result<Page<User>> getUserList(
            UserQueryRequest request) {

        Long currentUserId = UserContext.getCurrentUserId();
        checkAdminPermission(currentUserId);

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w.like(User::getUsername, request.getKeyword())
                             .or()
                             .like(User::getEmail, request.getKeyword()));
        }
        
        if (request.getStatus() != null) {
            wrapper.eq(User::getStatus, request.getStatus());
        }
        
        wrapper.orderByDesc(User::getCreatedAt);
        
        Page<User> userPage = userMapper.selectPage(new Page<>(request.getPage(), request.getSize()), wrapper);
        return Result.success(userPage);
    }

    @Operation(summary = "更新用户状态", description = "启用/禁用用户账号（管理员权限）")
    @PutMapping("/users/{userId}/status")
    public Result<Void> updateUserStatus(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusDTO request) {

        Long currentUserId = UserContext.getCurrentUserId();
        checkAdminPermission(currentUserId);

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        user.setStatus(request.getStatus());
        userMapper.updateById(user);
        
        String statusText = request.getStatus() == 1 ? "启用" : "禁用";
        return Result.success("用户" + statusText + "成功", null);
    }

    /**
     * 检查是否为管理员
     */
    private void checkAdminPermission(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        
        // 暂时简单判断：userId为1的是管理员
        // 实际应该查询用户角色表
        if (!userId.equals(1L)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
