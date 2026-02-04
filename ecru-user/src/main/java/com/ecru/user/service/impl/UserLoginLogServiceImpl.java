package com.ecru.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecru.user.entity.UserLoginLog;
import com.ecru.user.mapper.UserLoginLogMapper;
import com.ecru.user.service.UserLoginLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserLoginLogServiceImpl extends ServiceImpl<UserLoginLogMapper, UserLoginLog> 
        implements UserLoginLogService {

    @Override
    public void recordLoginLog(Long userId, Integer loginType, String loginIp, 
                              String loginDevice, Integer loginStatus, String failReason) {
        UserLoginLog loginLog = new UserLoginLog();
        loginLog.setUserId(userId);
        loginLog.setLoginType(loginType);
        loginLog.setLoginIp(loginIp);
        loginLog.setLoginDevice(loginDevice);
        loginLog.setLoginStatus(loginStatus);
        loginLog.setFailReason(failReason);
        
        baseMapper.insert(loginLog);
    }
}
