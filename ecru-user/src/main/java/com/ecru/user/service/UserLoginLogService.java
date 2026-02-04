package com.ecru.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ecru.user.entity.UserLoginLog;

public interface UserLoginLogService extends IService<UserLoginLog> {

    void recordLoginLog(Long userId, Integer loginType, String loginIp, 
                       String loginDevice, Integer loginStatus, String failReason);
}
