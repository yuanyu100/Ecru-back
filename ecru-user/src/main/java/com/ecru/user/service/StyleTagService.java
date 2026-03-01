package com.ecru.user.service;

import com.ecru.user.entity.StyleTag;
import com.ecru.user.dto.response.StyleTagVO;
import java.util.List;

/**
 * 风格标签服务接口
 */
public interface StyleTagService {
    
    /**
     * 获取所有风格标签
     */
    List<StyleTagVO> getAllTags();
    
    /**
     * 根据风格大类获取标签
     */
    List<StyleTagVO> getTagsByCategory(String category);
    
    /**
     * 获取预设标签
     */
    List<StyleTagVO> getPresetTags();
    
    /**
     * 根据ID获取标签
     */
    StyleTagVO getTagById(Long id);
    
    /**
     * 增加标签使用次数
     */
    void incrementUsageCount(Long id);
    
    /**
     * 获取风格标签分类列表
     */
    List<String> getTagCategories();
}
