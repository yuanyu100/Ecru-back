package com.ecru.user.service;

import com.ecru.user.dto.request.StylePreferenceFeedbackRequest;
import com.ecru.user.dto.response.UserStyleProfileVO;
import java.util.List;

/**
 * 用户风格偏好服务接口
 */
public interface UserStylePreferenceService {
    
    /**
     * 提交风格偏好反馈
     */
    void submitFeedback(Long userId, StylePreferenceFeedbackRequest request);
    
    /**
     * 获取用户风格画像
     */
    List<UserStyleProfileVO> getUserStyleProfile(Long userId);
    
    /**
     * 获取用户Top N偏好风格
     */
    List<UserStyleProfileVO> getTopPreferences(Long userId, Integer limit);
    
    /**
     * 获取用户偏好学习进度
     */
    Integer getLearningProgress(Long userId);
    
    /**
     * 重置用户风格画像
     */
    void resetUserStyleProfile(Long userId);
}
