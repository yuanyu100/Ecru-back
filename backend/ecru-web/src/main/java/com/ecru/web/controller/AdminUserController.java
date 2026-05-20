package com.ecru.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecru.clothing.entity.Clothing;
import com.ecru.clothing.mapper.ClothingMapper;
import com.ecru.common.exception.BusinessException;
import com.ecru.common.result.ErrorCode;
import com.ecru.common.result.Result;
import com.ecru.common.util.UserContext;
import com.ecru.outfit.entity.UserStyleArchive;
import com.ecru.outfit.mapper.UserStyleArchiveMapper;
import com.ecru.user.converter.UserConverter;
import com.ecru.user.entity.User;
import com.ecru.user.mapper.UserMapper;
import com.ecru.user.vo.UserVO;
import com.ecru.web.vo.AdminUserDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Tag(name = "管理员用户详情", description = "管理员查看用户资料、风格档案和衣物统计")
public class AdminUserController {

    private final UserMapper userMapper;
    private final UserStyleArchiveMapper userStyleArchiveMapper;
    private final ClothingMapper clothingMapper;

    @GetMapping("/{userId}")
    @Operation(summary = "获取用户详情")
    public Result<AdminUserDetailVO> getUserDetail(@PathVariable Long userId) {
        requireAdmin();

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        UserVO userVO = UserConverter.INSTANCE.toUserVO(user);
        UserStyleArchive styleProfile = userStyleArchiveMapper.selectByUserId(userId);
        Long clothingCount = clothingMapper.selectCount(
                new LambdaQueryWrapper<Clothing>()
                        .eq(Clothing::getUserId, userId)
                        .eq(Clothing::getIsDeleted, false)
        );

        return Result.success(AdminUserDetailVO.builder()
                .user(userVO)
                .styleProfile(styleProfile)
                .clothingCount(clothingCount)
                .build());
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
