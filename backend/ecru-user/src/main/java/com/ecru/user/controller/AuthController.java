package com.ecru.user.controller;

import com.ecru.common.result.Result;
import com.ecru.common.util.JwtUtil;
import com.ecru.common.util.UserContext;
import com.ecru.user.converter.UserConverter;
import com.ecru.user.dto.LoginDTO;
import com.ecru.user.dto.RefreshTokenDTO;
import com.ecru.user.dto.RegisterDTO;
import com.ecru.user.entity.User;
import com.ecru.user.service.UserService;
import com.ecru.user.vo.LoginVO;
import com.ecru.user.vo.RefreshTokenVO;
import com.ecru.user.vo.RegisterVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "认证管理", description = "用户注册、登录等接口")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "用户注册", description = "新用户注册账号")
    @PostMapping("/register")
    public Result<RegisterVO> register(@Valid @RequestBody RegisterDTO request) {
        User user = userService.register(request);
        RegisterVO vo = UserConverter.INSTANCE.toRegisterVO(user);
        return Result.success("注册成功", vo);
    }

    @Operation(summary = "用户登录", description = "用户登录获取JWT Token")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO request) {
        LoginVO vo = userService.login(request);
        return Result.success("登录成功", vo);
    }

    @Operation(summary = "刷新Token", description = "使用Refresh Token获取新的Access Token")
    @PostMapping("/refresh")
    public Result<RefreshTokenVO> refreshToken(@Valid @RequestBody RefreshTokenDTO request) {
        Map<String, Object> result = jwtUtil.refreshAccessToken(request.getRefreshToken());
        RefreshTokenVO vo = RefreshTokenVO.builder()
                .accessToken((String) result.get("accessToken"))
                .refreshToken((String) result.get("refreshToken"))
                .expiresIn((Long) result.get("expiresIn"))
                .tokenType((String) result.get("tokenType"))
                .build();
        return Result.success("刷新成功", vo);
    }

    @Operation(summary = "用户登出", description = "用户登出，使当前Token失效")
    @PostMapping("/logout")
    public Result<Void> logout() {
        String token = UserContext.getCurrentToken();
        if (token != null) {
            jwtUtil.blacklistToken(token);
        }
        return Result.success("登出成功", null);
    }
}
