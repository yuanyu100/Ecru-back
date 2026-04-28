package com.ecru.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecru.user.vo.UserLoginLogVO;

public interface UserLoginLogService {

    void recordLogin(Long userId,
                     Integer loginType,
                     String loginIp,
                     String loginDevice,
                     String loginLocation,
                     Integer loginStatus,
                     String failReason);

    Page<UserLoginLogVO> getAdminLogs(Long userId, Integer loginStatus, long page, long size);
}
