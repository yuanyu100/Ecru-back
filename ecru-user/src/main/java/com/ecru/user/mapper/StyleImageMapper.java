package com.ecru.user.mapper;

import com.ecru.user.entity.StyleImage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 风格图片Mapper接口
 */
@Mapper
public interface StyleImageMapper extends BaseMapper<StyleImage> {
    
    /**
     * 根据风格大类获取图片
     */
    List<StyleImage> selectByStyleCategory(@Param("styleCategory") String styleCategory, 
                                          @Param("limit") Integer limit,
                                          @Param("excludeImageId") Long excludeImageId);
    
    /**
     * 获取活跃的风格图片
     */
    List<StyleImage> selectActiveImages(@Param("limit") Integer limit);
    
    /**
     * 根据ID获取图片及其标签
     */
    StyleImage selectByIdWithTags(@Param("id") Long id);
}
