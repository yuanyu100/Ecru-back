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
import com.ecru.user.service.UserLoginLogService;
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
    private final UserLoginLogService userLoginLogService;

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
        user.setRole("USER");
        user.setStatus(1);

        userMapper.insert(user);
        log.info("User register success: {}", request.getUsername());
        return user;
    }

    @Override
    public LoginVO login(LoginDTO request) {
        User user = userMapper.selectByUsername(request.getUsername());
        String loginIp = getClientIp();
        String loginDevice = getDeviceInfo();
        String loginLocation = null;

        if (user == null) {
            userLoginLogService.recordLogin(null, 1, loginIp, loginDevice, loginLocation, 0, "用户名不存在");
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (user.getStatus() == 0) {
            userLoginLogService.recordLogin(user.getId(), 1, loginIp, loginDevice, loginLocation, 0, "用户已被禁用");
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            userLoginLogService.recordLogin(user.getId(), 1, loginIp, loginDevice, loginLocation, 0, "密码错误");
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(loginIp);
        userMapper.updateById(user);

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), user.getRole());

        userLoginLogService.recordLogin(user.getId(), 1, loginIp, loginDevice, loginLocation, 1, null);
        log.info("User login success: {}", request.getUsername());

        return LoginVO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(7200L)
                .tokenType("Bearer")
                .user(LoginVO.UserInfo.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .role(user.getRole())
                        .avatarUrl(user.getAvatarUrl())
                        .build())
                .build();
    }

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
            log.warn("Get client ip failed", e);
        }
        return "unknown";
    }

    private String getDeviceInfo() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String userAgent = request.getHeader("User-Agent");
                return userAgent != null ? userAgent.substring(0, Math.min(userAgent.length(), 200)) : "unknown";
            }
        } catch (Exception e) {
            log.warn("Get device info failed", e);
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

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userMapper.existsByEmail(request.getEmail())) {
                throw new BusinessException(ErrorCode.EMAIL_EXISTS);
            }
            user.setEmail(request.getEmail());
        }

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
        log.info("User update success: {}", user.getUsername());
        return UserConverter.INSTANCE.toUserVO(user);
    }

    @Override
    @Transactional
    public void updatePassword(Long userId, UpdatePasswordDTO request) {
        User user = getUserById(userId);
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "旧密码不正确");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
        log.info("User password updated: {}", user.getUsername());
    }

    @Override
    @Transactional
    public UserVO updateAvatar(Long userId, String avatarUrl) {
        User user = getUserById(userId);
        user.setAvatarUrl(avatarUrl);
        userMapper.updateById(user);

        log.info("User avatar updated: {}", user.getUsername());
        return UserConverter.INSTANCE.toUserVO(user);
    }
}
