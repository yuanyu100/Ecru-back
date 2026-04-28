package com.ecru.user.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecru.user.entity.UserLoginLog;
import com.ecru.user.mapper.UserLoginLogMapper;
import com.ecru.user.service.UserLoginLogService;
import com.ecru.user.vo.UserLoginLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserLoginLogServiceImpl implements UserLoginLogService {

    private final UserLoginLogMapper userLoginLogMapper;

    @Override
    public void recordLogin(Long userId,
                            Integer loginType,
                            String loginIp,
                            String loginDevice,
                            String loginLocation,
                            Integer loginStatus,
                            String failReason) {
        try {
            UserLoginLog logRecord = new UserLoginLog();
            logRecord.setUserId(userId);
            logRecord.setLoginType(loginType);
            logRecord.setLoginIp(loginIp);
            logRecord.setLoginDevice(loginDevice);
            logRecord.setLoginLocation(loginLocation);
            logRecord.setLoginStatus(loginStatus);
            logRecord.setFailReason(failReason);
            logRecord.setCreatedAt(LocalDateTime.now());
            userLoginLogMapper.insert(logRecord);
        } catch (Exception ex) {
            log.warn("Record login log failed", ex);
        }
    }

    @Override
    public Page<UserLoginLogVO> getAdminLogs(Long userId, Integer loginStatus, long page, long size) {
        long current = page > 0 ? page : 1L;
        long pageSize = size > 0 ? size : 20L;
        long offset = (current - 1) * pageSize;
        List<UserLoginLogVO> records = userLoginLogMapper.selectAdminLogs(userId, loginStatus, offset, pageSize);
        long total = userLoginLogMapper.countAdminLogs(userId, loginStatus);
        Page<UserLoginLogVO> result = new Page<>(current, pageSize, total);
        result.setRecords(records);
        return result;
    }
}
