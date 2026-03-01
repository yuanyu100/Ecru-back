package com.ecru.user.service.impl;

import com.ecru.common.exception.BusinessException;
import com.ecru.common.result.ErrorCode;
import com.ecru.common.util.JwtUtil;
import com.ecru.user.converter.UserConverter;
import com.ecru.user.dto.LoginDTO;
import com.ecru.user.dto.RegisterDTO;
import com.ecru.user.dto.UpdatePasswordDTO;
import com.ecru.user.dto.UpdateUserDTO;
import com.ecru.user.entity.User;
import com.ecru.user.mapper.UserMapper;

import com.ecru.user.service.UserService;
import com.ecru.user.vo.LoginVO;
import com.ecru.user.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User register(RegisterDTO request) {
        if (userMapper.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        if (request.getEmail() != null && userMapper.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }

        if (request.getPhone() != null && userMapper.existsByPhone(request.getPhone())) {
            throw new BusinessException(ErrorCode.PHONE_EXISTS);
        }

        User user = UserConverter.INSTANCE.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(1);

        userMapper.insert(user);
        log.info("用户注册成功: {}", request.getUsername());

        return user;
    }

    @Override
    public LoginVO login(LoginDTO request) {
        log.info("Login request received: {}", request);
        User user = userMapper.selectByUsername(request.getUsername());
        log.info("User found: {}", user);
        
        // 获取请求IP和设备信息
        String loginIp = getClientIp();
        String loginDevice = getDeviceInfo();
        log.info("Login IP: {}, Device: {}", loginIp, loginDevice);
        
        if (user == null) {
            // 记录登录失败日志
            // userLoginLogService.recordLoginLog(0L, 1, loginIp, loginDevice, 0, "用户名不存在");
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (user.getStatus() == 0) {
            // 记录登录失败日志
            // userLoginLogService.recordLoginLog(user.getId(), 1, loginIp, loginDevice, 0, "用户已被禁用");
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // 记录登录失败日志
            // userLoginLogService.recordLoginLog(user.getId(), 1, loginIp, loginDevice, 0, "密码错误");
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(loginIp);
        userMapper.updateById(user);

        // 生成Token
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        // 记录登录成功日志
        // userLoginLogService.recordLoginLog(user.getId(), 1, loginIp, loginDevice, 1, null);

        log.info("用户登录成功: {}", request.getUsername());

        return LoginVO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(7200L)
                .tokenType("Bearer")
                .user(LoginVO.UserInfo.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .avatarUrl(user.getAvatarUrl())
                        .build())
                .build();
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("Proxy-Client-IP");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("WL-Proxy-Client-IP");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }
                return ip;
            }
        } catch (Exception e) {
            log.warn("获取客户端IP失败", e);
        }
        return "unknown";
    }

    /**
     * 获取设备信息
     */
    private String getDeviceInfo() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String userAgent = request.getHeader("User-Agent");
                return userAgent != null ? userAgent.substring(0, Math.min(userAgent.length(), 200)) : "unknown";
            }
        } catch (Exception e) {
            log.warn("获取设备信息失败", e);
        }
        return "unknown";
    }

    @Override
    public User getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    @Override
    public UserVO getCurrentUser(Long userId) {
        User user = getUserById(userId);
        return UserConverter.INSTANCE.toUserVO(user);
    }

    @Override
    @Transactional
    public UserVO updateUser(Long userId, UpdateUserDTO request) {
        User user = getUserById(userId);

        // 检查邮箱是否被其他用户使用
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userMapper.existsByEmail(request.getEmail())) {
                throw new BusinessException(ErrorCode.EMAIL_EXISTS);
            }
            user.setEmail(request.getEmail());
        }

        // 检查手机号是否被其他用户使用
        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            if (userMapper.existsByPhone(request.getPhone())) {
                throw new BusinessException(ErrorCode.PHONE_EXISTS);
            }
            user.setPhone(request.getPhone());
        }

        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getBirthday() != null) {
            user.setBirthday(request.getBirthday());
        }

        userMapper.updateById(user);
        log.info("用户更新成功: {}", user.getUsername());

        return UserConverter.INSTANCE.toUserVO(user);
    }

    @Override
    @Transactional
    public void updatePassword(Long userId, UpdatePasswordDTO request) {
        User user = getUserById(userId);

        // 验证旧密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "旧密码不正确");
        }

        // 更新新密码
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);

        log.info("用户修改密码成功: {}", user.getUsername());
    }

    @Override
    @Transactional
    public UserVO updateAvatar(Long userId, String avatarUrl) {
        User user = getUserById(userId);
        user.setAvatarUrl(avatarUrl);
        userMapper.updateById(user);

        log.info("用户更新头像成功: {}", user.getUsername());
        return UserConverter.INSTANCE.toUserVO(user);
    }
}
