package com.ecru.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecru.user.entity.UserSettings;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserSettingsMapper extends BaseMapper<UserSettings> {

    List<UserSettings> selectByUserId(@Param("userId") Long userId);
}
