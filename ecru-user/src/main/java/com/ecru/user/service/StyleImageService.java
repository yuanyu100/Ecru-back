package com.ecru.user.service;

import com.ecru.user.entity.StyleImage;
import com.ecru.user.dto.request.StyleImageQueryRequest;
import com.ecru.user.dto.response.StyleImageVO;
import java.util.List;

/**
 * 风格图片服务接口
 */
public interface StyleImageService {
    
    /**
     * 获取风格图片列表
     */
    List<StyleImageVO> getStyleImages(StyleImageQueryRequest request);
    
    /**
     * 根据ID获取风格图片
     */
    StyleImageVO getStyleImageById(Long id);
    
    /**
     * 随机获取风格图片
     */
    List<StyleImageVO> getRandomStyleImages(Integer count);
    
    /**
     * 根据风格大类获取图片
     */
    List<StyleImageVO> getStyleImagesByCategory(String category, Integer count);
    
    /**
     * 检查图片是否存在
     */
    boolean existsById(Long id);
}
