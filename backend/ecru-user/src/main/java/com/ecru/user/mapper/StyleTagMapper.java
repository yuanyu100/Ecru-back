package com.ecru.user.mapper;

import com.ecru.user.entity.StyleTag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 风格标签Mapper接口
 */
@Mapper
public interface StyleTagMapper extends BaseMapper<StyleTag> {
    
    /**
     * 根据风格大类获取标签
     */
    List<StyleTag> selectByCategory(@Param("category") String category);
    
    /**
     * 获取预设标签
     */
    List<StyleTag> selectPresetTags();
    
    /**
     * 增加标签使用次数
     */
    int incrementUsageCount(@Param("id") Long id);
}
