package com.ecru.outfit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecru.outfit.entity.UserStyleArchive;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserStyleArchiveMapper extends BaseMapper<UserStyleArchive> {

    UserStyleArchive selectByUserId(@Param("userId") Long userId);
}
