package com.ecru.outfit.service;

import com.ecru.outfit.entity.OutfitAdviceRecord;
import com.ecru.outfit.entity.OutfitItem;
import com.ecru.outfit.entity.OutfitFeedback;
import com.ecru.outfit.entity.UserStyleProfile;
import com.ecru.outfit.mapper.OutfitAdviceRecordMapper;
import com.ecru.outfit.mapper.OutfitItemMapper;
import com.ecru.outfit.mapper.OutfitFeedbackMapper;
import com.ecru.outfit.mapper.OutfitUserStyleProfileMapper;
import com.ecru.outfit.service.agent.OutfitAdvisorAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;

/**
 * 搭配建议服务
 */
@Slf4j
@Service
public class OutfitAdviceService {

    @Autowired
    private OutfitAdvisorAgent outfitAdvisorAgent;

    @Autowired
    private OutfitAdviceRecordMapper outfitAdviceRecordMapper;

    @Autowired
    private OutfitItemMapper outfitItemMapper;

    @Autowired
    private OutfitFeedbackMapper outfitFeedbackMapper;

    @Autowired
    private OutfitUserStyleProfileMapper userStyleProfileMapper;

    /**
     * 获取搭配建议
     * @param userId 用户ID
     * @param imageStream 穿搭照片
     * @param description 文字描述
     * @param location 地理位置
     * @param occasion 场合
     * @return 搭配建议记录
     */
    @Transactional
    public OutfitAdviceRecord getOutfitAdvice(
            Long userId,
            InputStream imageStream,
            String description,
            String location,
            String occasion
    ) {
        try {
            // 调用Agent获取搭配建议
            var advice = outfitAdvisorAgent.adviseOutfit(
                    userId,
                    imageStream,
                    description,
                    location,
                    occasion
            );

            // 保存搭配建议记录
            OutfitAdviceRecord record = saveAdviceRecord(userId, advice, imageStream, description, location, occasion);

            return record;
        } catch (Exception e) {
            log.error("获取搭配建议失败: {}", e.getMessage());
            throw new RuntimeException("获取搭配建议失败", e);
        }
    }

    /**
     * 保存搭配建议记录
     * @param userId 用户ID
     * @param advice 搭配建议
     * @param imageStream 穿搭照片
     * @param description 文字描述
     * @param location 地理位置
     * @param occasion 场合
     * @return 搭配建议记录
     */
    private OutfitAdviceRecord saveAdviceRecord(
            Long userId,
            OutfitAdvisorAgent.OutfitAdvice advice,
            InputStream imageStream,
            String description,
            String location,
            String occasion
    ) {
        // 创建搭配建议记录
        OutfitAdviceRecord record = new OutfitAdviceRecord();
        record.setUserId(userId);
        record.setInputType(imageStream != null ? 1 : 2);
        record.setInputDescription(description);
        record.setLocation(location);
        record.setOccasion(occasion);
        record.setOutfitName(advice.getOutfitName());
        record.setOutfitDescription(advice.getOutfitDescription());
        record.setReasoning(advice.getReasoning());
        record.setFashionSuggestions(advice.getFashionSuggestions());

        // 保存记录
        outfitAdviceRecordMapper.insert(record);

        // 保存搭配单品
        saveOutfitItems(record.getId(), advice.getItems());

        return record;
    }

    /**
     * 保存搭配单品
     * @param outfitAdviceId 搭配建议ID
     * @param items 单品列表
     */
    private void saveOutfitItems(Long outfitAdviceId, List<OutfitAdvisorAgent.OutfitAdvice.OutfitItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        for (int i = 0; i < items.size(); i++) {
            var item = items.get(i);
            OutfitItem outfitItem = new OutfitItem();
            outfitItem.setOutfitAdviceId(outfitAdviceId);
            outfitItem.setClothingId(item.getClothingId());
            outfitItem.setItemName(item.getName());
            outfitItem.setItemCategory(item.getCategory());
            outfitItem.setItemColor(item.getColor());
            outfitItem.setItemImageUrl(item.getImageUrl());
            outfitItem.setIsRecommended(item.getIsRecommended() != null ? item.getIsRecommended() : false);
            outfitItem.setReason(item.getReason());
            outfitItem.setSortOrder(i);

            outfitItemMapper.insert(outfitItem);
        }
    }

    /**
     * 获取历史搭配记录
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 搭配记录列表
     */
    public List<OutfitAdviceRecord> getHistory(Long userId, Integer page, Integer size) {
        // 计算偏移量
        int offset = (page - 1) * size;
        List<OutfitAdviceRecord> records = outfitAdviceRecordMapper.selectByUserId(userId, offset, size);
        return records != null ? records : java.util.Collections.emptyList();
    }

    /**
     * 获取搭配详情
     * @param id 搭配记录ID
     * @return 搭配记录
     */
    public OutfitAdviceRecord getAdviceById(Long id) {
        return outfitAdviceRecordMapper.selectById(id);
    }

    /**
     * 删除搭配记录
     * @param id 搭配记录ID
     * @return 是否成功
     */
    @Transactional
    public boolean deleteAdvice(Long id) {
        // 删除搭配单品
        outfitItemMapper.deleteByOutfitAdviceId(id);
        // 删除搭配记录
        return outfitAdviceRecordMapper.deleteById(id) > 0;
    }

    /**
     * 收藏/取消收藏搭配
     * @param id 搭配记录ID
     * @param isFavorite 是否收藏
     * @return 是否成功
     */
    public boolean toggleFavorite(Long id, Boolean isFavorite) {
        OutfitAdviceRecord record = outfitAdviceRecordMapper.selectById(id);
        if (record == null) {
            return false;
        }
        record.setIsFavorite(isFavorite);
        return outfitAdviceRecordMapper.updateById(record) > 0;
    }

    /**
     * 提交搭配反馈
     * @param outfitAdviceId 搭配建议ID
     * @param userId 用户ID
     * @param feedback 反馈
     * @return 反馈记录
     */
    public OutfitFeedback submitFeedback(Long outfitAdviceId, Long userId, OutfitFeedback feedback) {
        feedback.setOutfitAdviceId(outfitAdviceId);
        feedback.setUserId(userId);
        outfitFeedbackMapper.insert(feedback);
        return feedback;
    }

    /**
     * 获取用户风格档案
     * @param userId 用户ID
     * @return 风格档案
     */
    public UserStyleProfile getStyleProfile(Long userId) {
        UserStyleProfile profile = userStyleProfileMapper.selectByUserId(userId);
        if (profile == null) {
            // 如果没有风格档案，返回一个空的对象
            profile = new UserStyleProfile();
            profile.setUserId(userId);
        }
        return profile;
    }

    /**
     * 更新用户风格档案
     * @param profile 风格档案
     * @return 是否成功
     */
    public boolean updateStyleProfile(UserStyleProfile profile) {
        return userStyleProfileMapper.updateById(profile) > 0;
    }

}
