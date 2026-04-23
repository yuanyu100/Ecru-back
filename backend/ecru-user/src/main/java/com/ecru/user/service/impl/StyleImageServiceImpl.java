package com.ecru.user.service.impl;

import com.ecru.user.entity.StyleImage;
import com.ecru.user.entity.StyleImageTag;
import com.ecru.user.dto.request.StyleImageQueryRequest;
import com.ecru.user.dto.response.StyleImageVO;
import com.ecru.user.dto.response.StyleTagVO;
import com.ecru.user.mapper.StyleImageMapper;
import com.ecru.user.mapper.StyleImageTagMapper;
import com.ecru.user.service.StyleImageService;
import com.ecru.user.service.StyleTagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 风格图片服务实现类
 */
@Service
public class StyleImageServiceImpl implements StyleImageService {
    
    @Autowired
    private StyleImageMapper styleImageMapper;
    
    @Autowired
    private StyleImageTagMapper styleImageTagMapper;
    
    @Autowired
    private StyleTagService styleTagService;
    
    @Override
    public List<StyleImageVO> getStyleImages(StyleImageQueryRequest request) {
        List<StyleImage> images;
        
        if (request.getStyleCategory() != null) {
            images = styleImageMapper.selectByStyleCategory(
                request.getStyleCategory(), 
                request.getLimit(), 
                request.getExcludeImageId()
            );
        } else {
            images = styleImageMapper.selectActiveImages(request.getLimit());
        }
        
        return images.stream().map(this::convertToVO).collect(Collectors.toList());
    }
    
    @Override
    public StyleImageVO getStyleImageById(Long id) {
        StyleImage image = styleImageMapper.selectById(id);
        return image != null ? convertToVO(image) : null;
    }
    
    @Override
    public List<StyleImageVO> getRandomStyleImages(Integer count) {
        List<StyleImage> images = styleImageMapper.selectActiveImages(count);
        return images.stream().map(this::convertToVO).collect(Collectors.toList());
    }
    
    @Override
    public List<StyleImageVO> getStyleImagesByCategory(String category, Integer count) {
        List<StyleImage> images = styleImageMapper.selectByStyleCategory(category, count, null);
        return images.stream().map(this::convertToVO).collect(Collectors.toList());
    }
    
    @Override
    public boolean existsById(Long id) {
        return styleImageMapper.selectById(id) != null;
    }
    
    /**
     * 转换实体类为VO
     */
    private StyleImageVO convertToVO(StyleImage image) {
        StyleImageVO vo = new StyleImageVO();
        vo.setId(image.getId());
        vo.setImageUrl(image.getImageUrl());
        vo.setTitle(image.getTitle());
        vo.setSource(image.getSource());
        vo.setSourceUrl(image.getSourceUrl());
        vo.setPrice(image.getPrice());
        vo.setStyleCategory(image.getStyleCategory());
        vo.setCreatedAt(image.getCreatedAt());
        
        // 获取图片的标签
        List<StyleImageTag> imageTags = styleImageTagMapper.selectByImageId(image.getId());
        List<StyleTagVO> tagVOs = imageTags.stream()
            .map(tag -> styleTagService.getTagById(tag.getStyleTagId()))
            .filter(tag -> tag != null)
            .collect(Collectors.toList());
        vo.setTags(tagVOs);
        
        return vo;
    }
}
