package com.ecru.user.service.impl;

import com.ecru.user.entity.StyleTag;
import com.ecru.user.dto.response.StyleTagVO;
import com.ecru.user.mapper.StyleTagMapper;
import com.ecru.user.service.StyleTagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 风格标签服务实现类
 */
@Service
public class StyleTagServiceImpl implements StyleTagService {
    
    @Autowired
    private StyleTagMapper styleTagMapper;
    
    @Override
    public List<StyleTagVO> getAllTags() {
        List<StyleTag> tags = styleTagMapper.selectList(null);
        return tags.stream().map(this::convertToVO).collect(Collectors.toList());
    }
    
    @Override
    public List<StyleTagVO> getTagsByCategory(String category) {
        List<StyleTag> tags = styleTagMapper.selectByCategory(category);
        return tags.stream().map(this::convertToVO).collect(Collectors.toList());
    }
    
    @Override
    public List<StyleTagVO> getPresetTags() {
        List<StyleTag> tags = styleTagMapper.selectPresetTags();
        return tags.stream().map(this::convertToVO).collect(Collectors.toList());
    }
    
    @Override
    public StyleTagVO getTagById(Long id) {
        StyleTag tag = styleTagMapper.selectById(id);
        return tag != null ? convertToVO(tag) : null;
    }
    
    @Override
    public void incrementUsageCount(Long id) {
        styleTagMapper.incrementUsageCount(id);
    }
    
    @Override
    public List<String> getTagCategories() {
        // 这里可以从数据库查询所有唯一的分类
        // 为了简化，返回预设的分类列表
        return List.of("日系", "韩系", "欧美", "通勤", "运动", "复古");
    }
    
    /**
     * 转换实体类为VO
     */
    private StyleTagVO convertToVO(StyleTag tag) {
        StyleTagVO vo = new StyleTagVO();
        vo.setId(tag.getId());
        vo.setName(tag.getName());
        vo.setCategory(tag.getCategory());
        vo.setDescription(tag.getDescription());
        vo.setUsageCount(tag.getUsageCount());
        return vo;
    }
}
