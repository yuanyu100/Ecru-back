package com.ecru.user.service.impl;

import com.ecru.user.entity.UserStylePreferenceLog;
import com.ecru.user.entity.UserStyleProfile;
import com.ecru.user.entity.StyleImageTag;
import com.ecru.user.dto.request.StylePreferenceFeedbackRequest;
import com.ecru.user.dto.response.UserStyleProfileVO;
import com.ecru.user.dto.response.StyleTagVO;
import com.ecru.user.mapper.UserStylePreferenceLogMapper;
import com.ecru.user.mapper.UserStyleProfileMapper;
import com.ecru.user.mapper.StyleImageTagMapper;
import com.ecru.user.service.UserStylePreferenceService;
import com.ecru.user.service.StyleTagService;
import com.ecru.user.service.StyleImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import java.math.BigDecimal;

/**
 * 用户风格偏好服务实现类
 */
@Service
public class UserStylePreferenceServiceImpl implements UserStylePreferenceService {
    
    @Autowired
    private UserStylePreferenceLogMapper preferenceLogMapper;
    
    @Autowired
    private UserStyleProfileMapper styleProfileMapper;
    
    @Autowired
    private StyleImageTagMapper styleImageTagMapper;
    
    @Autowired
    private StyleTagService styleTagService;
    
    @Autowired
    private StyleImageService styleImageService;
    
    private static final Integer LEARNING_THRESHOLD = 50; // 学习阈值
    
    @Override
    @Transactional
    public void submitFeedback(Long userId, StylePreferenceFeedbackRequest request) {
        // 验证图片是否存在
        if (!styleImageService.existsById(request.getImageId())) {
            throw new IllegalArgumentException("图片不存在");
        }
        
        // 检查是否已经标记过
        UserStylePreferenceLog existingLog = preferenceLogMapper.selectByUserIdAndImageId(
            userId, request.getImageId()
        );
        
        if (existingLog != null) {
            // 已经标记过，更新标记
            existingLog.setPreferenceType(request.getPreferenceType());
            preferenceLogMapper.updateById(existingLog);
        } else {
            // 新标记
            UserStylePreferenceLog log = new UserStylePreferenceLog();
            log.setUserId(userId);
            log.setImageId(request.getImageId());
            log.setPreferenceType(request.getPreferenceType());
            preferenceLogMapper.insert(log);
        }
        
        // 更新用户风格画像
        updateUserStyleProfile(userId, request.getImageId(), request.getPreferenceType());
    }
    
    @Override
    public List<UserStyleProfileVO> getUserStyleProfile(Long userId) {
        List<UserStyleProfile> profiles = styleProfileMapper.selectByUserId(userId);
        return profiles.stream().map(this::convertToVO).collect(Collectors.toList());
    }
    
    @Override
    public List<UserStyleProfileVO> getTopPreferences(Long userId, Integer limit) {
        List<UserStyleProfile> profiles = styleProfileMapper.selectTopPreferences(userId, limit);
        return profiles.stream().map(this::convertToVO).collect(Collectors.toList());
    }
    
    @Override
    public Integer getLearningProgress(Long userId) {
        Integer totalLogs = preferenceLogMapper.countByUserId(userId);
        Integer progress = (totalLogs * 100) / LEARNING_THRESHOLD;
        return Math.min(progress, 100);
    }
    
    @Override
    @Transactional
    public void resetUserStyleProfile(Long userId) {
        // 删除用户风格画像
        List<UserStyleProfile> profiles = styleProfileMapper.selectByUserId(userId);
        for (UserStyleProfile profile : profiles) {
            styleProfileMapper.deleteById(profile.getId());
        }
        
        // 删除用户偏好标记
        List<UserStylePreferenceLog> logs = preferenceLogMapper.selectByUserId(userId, Integer.MAX_VALUE);
        for (UserStylePreferenceLog log : logs) {
            preferenceLogMapper.deleteById(log.getId());
        }
    }
    
    /**
     * 更新用户风格画像
     */
    private void updateUserStyleProfile(Long userId, Long imageId, Integer preferenceType) {
        // 获取图片的标签
        List<StyleImageTag> imageTags = styleImageTagMapper.selectByImageId(imageId);
        
        for (StyleImageTag imageTag : imageTags) {
            Long styleTagId = imageTag.getStyleTagId();
            
            // 计算偏好分数变化
            BigDecimal scoreChange = calculateScoreChange(preferenceType, imageTag.getConfidence());
            
            // 查找现有风格画像记录
            UserStyleProfile profile = styleProfileMapper.selectByUserIdAndStyleTagId(userId, styleTagId);
            
            if (profile != null) {
                // 更新现有记录
                BigDecimal newScore = profile.getPreferenceScore().add(scoreChange);
                // 限制分数范围在 -1.0 到 1.0 之间
                newScore = new BigDecimal(Math.max(-1.0, Math.min(1.0, newScore.doubleValue())));
                
                int newInteractionCount = profile.getInteractionCount() + 1;
                styleProfileMapper.updatePreferenceScore(
                    userId, styleTagId, newScore, newInteractionCount
                );
            } else {
                // 创建新记录
                UserStyleProfile newProfile = new UserStyleProfile();
                newProfile.setUserId(userId);
                newProfile.setStyleTagId(styleTagId);
                newProfile.setPreferenceScore(scoreChange);
                newProfile.setInteractionCount(1);
                styleProfileMapper.insert(newProfile);
            }
        }
    }
    
    /**
     * 计算偏好分数变化
     */
    private BigDecimal calculateScoreChange(Integer preferenceType, BigDecimal confidence) {
        switch (preferenceType) {
            case 1: // like
                return new BigDecimal(0.1).multiply(confidence);
            case 2: // dislike
                return new BigDecimal(-0.1).multiply(confidence);
            case 0: // skip
            default:
                return BigDecimal.ZERO;
        }
    }
    
    /**
     * 转换实体类为VO
     */
    private UserStyleProfileVO convertToVO(UserStyleProfile profile) {
        UserStyleProfileVO vo = new UserStyleProfileVO();
        StyleTagVO styleTagVO = styleTagService.getTagById(profile.getStyleTagId());
        vo.setStyleTag(styleTagVO);
        vo.setPreferenceScore(profile.getPreferenceScore());
        vo.setInteractionCount(profile.getInteractionCount());
        return vo;
    }
}
