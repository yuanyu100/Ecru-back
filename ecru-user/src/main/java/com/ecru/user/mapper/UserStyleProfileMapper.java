package com.ecru.user.mapper;

import com.ecru.user.entity.UserStyleProfile;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.math.BigDecimal;

/**
 * 用户风格画像表Mapper接口
 */
@Mapper
public interface UserStyleProfileMapper extends BaseMapper<UserStyleProfile> {
    
    /**
     * 根据用户ID和标签ID查询偏好
     */
    UserStyleProfile selectByUserIdAndStyleTagId(@Param("userId") Long userId, @Param("styleTagId") Long styleTagId);
    
    /**
     * 根据用户ID获取风格画像
     */
    List<UserStyleProfile> selectByUserId(@Param("userId") Long userId);
    
    /**
     * 根据用户ID获取Top N偏好标签
     */
    List<UserStyleProfile> selectTopPreferences(@Param("userId") Long userId, @Param("limit") Integer limit);
    
    /**
     * 更新偏好分数
     */
    int updatePreferenceScore(@Param("userId") Long userId, 
                             @Param("styleTagId") Long styleTagId, 
                             @Param("preferenceScore") BigDecimal preferenceScore, 
                             @Param("interactionCount") Integer interactionCount);
}
