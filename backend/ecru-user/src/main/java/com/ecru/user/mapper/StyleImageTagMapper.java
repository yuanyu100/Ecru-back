package com.ecru.user.mapper;

import com.ecru.user.entity.StyleImageTag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 风格图片标签关联表Mapper接口
 */
@Mapper
public interface StyleImageTagMapper extends BaseMapper<StyleImageTag> {
    
    /**
     * 根据图片ID获取标签
     */
    List<StyleImageTag> selectByImageId(@Param("imageId") Long imageId);
    
    /**
     * 根据标签ID获取图片
     */
    List<StyleImageTag> selectByStyleTagId(@Param("styleTagId") Long styleTagId);
    
    /**
     * 批量插入图片标签关联
     */
    int batchInsert(@Param("tags") List<StyleImageTag> tags);
}
