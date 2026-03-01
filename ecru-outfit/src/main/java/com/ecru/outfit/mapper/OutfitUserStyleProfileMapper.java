package com.ecru.outfit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecru.outfit.entity.UserStyleProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户风格档案Mapper
 */
@Mapper
public interface OutfitUserStyleProfileMapper extends BaseMapper<UserStyleProfile> {

    /**
     * 根据用户ID查询风格档案
     * @param userId 用户ID
     * @return 风格档案
     */
    UserStyleProfile selectByUserId(@Param("userId") Long userId);

    /**
     * 根据气质类型查询用户档案
     * @param temperamentType 气质类型
     * @param limit 限制数量
     * @return 档案列表
     */
    java.util.List<UserStyleProfile> selectByTemperamentType(@Param("temperamentType") String temperamentType, @Param("limit") Integer limit);

}
