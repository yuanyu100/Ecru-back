package com.ecru.user.service;

import com.ecru.user.dto.LoginDTO;
import com.ecru.user.dto.RegisterDTO;
import com.ecru.user.dto.UpdatePasswordDTO;
import com.ecru.user.dto.UpdateUserDTO;
import com.ecru.user.entity.User;
import com.ecru.user.vo.LoginVO;
import com.ecru.user.vo.UserVO;

public interface UserService {

    /**
     * 用户注册
     */
    User register(RegisterDTO request);

    /**
     * 用户登录
     */
    LoginVO login(LoginDTO request);

    /**
     * 根据ID获取用户
     */
    User getUserById(Long userId);

    /**
     * 获取当前登录用户信息
     */
    UserVO getCurrentUser(Long userId);

    /**
     * 更新用户信息
     */
    UserVO updateUser(Long userId, UpdateUserDTO request);

    /**
     * 修改密码
     */
    void updatePassword(Long userId, UpdatePasswordDTO request);

    /**
     * 更新用户头像
     */
    UserVO updateAvatar(Long userId, String avatarUrl);
}
