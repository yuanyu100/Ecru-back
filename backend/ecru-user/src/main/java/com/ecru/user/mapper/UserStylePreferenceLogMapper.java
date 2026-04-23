package com.ecru.user.mapper;

import com.ecru.user.entity.UserStylePreferenceLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 用户风格偏好标记表Mapper接口
 */
@Mapper
public interface UserStylePreferenceLogMapper extends BaseMapper<UserStylePreferenceLog> {
    
    /**
     * 根据用户ID和图片ID查询标记
     */
    UserStylePreferenceLog selectByUserIdAndImageId(@Param("userId") Long userId, @Param("imageId") Long imageId);
    
    /**
     * 根据用户ID获取标记历史
     */
    List<UserStylePreferenceLog> selectByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);
    
    /**
     * 统计用户标记数量
     */
    Integer countByUserId(@Param("userId") Long userId);
    
    /**
     * 统计用户对特定类型的标记数量
     */
    Integer countByUserIdAndType(@Param("userId") Long userId, @Param("preferenceType") Integer preferenceType);
}
