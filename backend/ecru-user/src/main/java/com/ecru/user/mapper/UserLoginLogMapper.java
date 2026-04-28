package com.ecru.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecru.user.entity.UserLoginLog;
import com.ecru.user.vo.UserLoginLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserLoginLogMapper extends BaseMapper<UserLoginLog> {

    List<UserLoginLogVO> selectAdminLogs(@Param("userId") Long userId,
                                         @Param("loginStatus") Integer loginStatus,
                                         @Param("offset") long offset,
                                         @Param("size") long size);

    long countAdminLogs(@Param("userId") Long userId,
                        @Param("loginStatus") Integer loginStatus);
}
