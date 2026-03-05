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
import com.ecru.outfit.service.mcp.McpWeatherService;
import com.ecru.outfit.service.rag.RagService;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

    @Autowired
    private McpWeatherService weatherService;

    @Autowired
    private RagService ragService;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

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
        PageHelper.startPage(page, size);
        com.github.pagehelper.Page<OutfitAdviceRecord> pageResult = outfitAdviceRecordMapper.selectByUserId(userId);
        return pageResult.getResult() != null ? pageResult.getResult() : java.util.Collections.emptyList();
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

    /**
     * Agent聊天方法
     * @param userId 用户ID
     * @param chatRequest 聊天请求
     * @return 聊天响应
     */
    public ChatResponse chatWithAgent(Long userId, ChatRequest chatRequest) {
        try {
            // 1. 生成或使用会话ID
            String sessionId = chatRequest.getSessionId();
            if (sessionId == null || sessionId.trim().isEmpty()) {
                sessionId = generateSessionId(userId);
            }

            // 2. 获取聊天历史
            List<Map<String, String>> chatHistory = getChatHistory(userId, sessionId);

            // 3. 获取天气信息
            McpWeatherService.WeatherInfo weatherInfo = null;
            String weatherInfoStr = "";
            if (chatRequest.getLocation() != null) {
                weatherInfo = weatherService.getWeatherByLocation(chatRequest.getLocation());
                if (weatherInfo != null) {
                    weatherInfoStr = weatherInfo.getLocation() + "，" + weatherInfo.getTemperature() + "℃，" + weatherInfo.getWeatherCondition();
                }
            }

            // 4. 分析用户需求
            String userMessage = chatRequest.getMessage();
            
            // 5. 将当前消息添加到历史
            Map<String, String> userMessageMap = new HashMap<>();
            userMessageMap.put("role", "user");
            userMessageMap.put("content", userMessage);
            chatHistory.add(userMessageMap);

            // 6. 生成衣物查询
            String query = generateClothingQuery(userMessage, weatherInfo, chatRequest.getOccasion());

            // 7. 从衣柜中检索衣物
            List<Map<String, Object>> recommendedClothes = new ArrayList<>();
            if (query != null) {
                var results = ragService.searchClothes(userId, query, 8);
                for (var result : results) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", result.getClothingId());
                    item.put("name", result.getName());
                    item.put("category", result.getCategory());
                    item.put("color", result.getPrimaryColor());
                    item.put("imageUrl", result.getImageUrl());
                    item.put("similarity", result.getSimilarity());
                    recommendedClothes.add(item);
                }
            }

            // 8. 生成智能回复
            String response = generateResponse(userMessage, weatherInfo, recommendedClothes, chatRequest.getOccasion());

            // 9. 将回复添加到历史
            Map<String, String> assistantMessageMap = new HashMap<>();
            assistantMessageMap.put("role", "assistant");
            assistantMessageMap.put("content", response);
            chatHistory.add(assistantMessageMap);

            // 10. 保存聊天历史
            saveChatHistory(userId, sessionId, chatHistory);

            // 11. 构建响应
            ChatResponse chatResponse = new ChatResponse();
            chatResponse.setResponse(response);
            chatResponse.setRecommendedClothes(recommendedClothes);
            chatResponse.setWeatherInfo(weatherInfoStr);
            chatResponse.setSessionId(sessionId);

            return chatResponse;
        } catch (Exception e) {
            log.error("Agent聊天失败: {}", e.getMessage());
            throw new RuntimeException("Agent聊天失败", e);
        }
    }

    /**
     * 生成会话ID
     * @param userId 用户ID
     * @return 会话ID
     */
    private String generateSessionId(Long userId) {
        return "chat:session:" + userId + ":" + System.currentTimeMillis();
    }

    /**
     * 获取聊天历史
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @return 聊天历史
     */
    private List<Map<String, String>> getChatHistory(Long userId, String sessionId) {
        try {
            if (redisTemplate != null) {
                String key = "chat:history:" + sessionId;
                String cachedData = redisTemplate.opsForValue().get(key);
                if (cachedData != null) {
                    // 直接返回空列表，避免类型转换问题
                    // 后续可以优化为正确的类型转换
                    return new ArrayList<>();
                }
            }
        } catch (Exception e) {
            log.warn("获取聊天历史失败: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    /**
     * 保存聊天历史
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param chatHistory 聊天历史
     */
    private void saveChatHistory(Long userId, String sessionId, List<Map<String, String>> chatHistory) {
        try {
            if (redisTemplate != null) {
                String key = "chat:history:" + sessionId;
                redisTemplate.opsForValue().set(
                        key,
                        com.alibaba.fastjson2.JSON.toJSONString(chatHistory),
                        24, // 24小时过期
                        TimeUnit.HOURS
                );
            }
        } catch (Exception e) {
            log.warn("保存聊天历史失败: {}", e.getMessage());
        }
    }

    /**
     * 根据用户消息生成衣物查询
     * @param userMessage 用户消息
     * @param weatherInfo 天气信息
     * @param occasion 场合
     * @return 查询文本
     */
    private String generateClothingQuery(String userMessage, McpWeatherService.WeatherInfo weatherInfo, String occasion) {
        StringBuilder query = new StringBuilder();
        
        // 分析用户消息
        userMessage = userMessage.toLowerCase();
        
        if (userMessage.contains("推荐") || userMessage.contains("建议") || userMessage.contains("穿什么")) {
            // 根据天气添加查询条件
            if (weatherInfo != null) {
                double temperature = weatherInfo.getTemperature();
                if (temperature >= 30) {
                    query.append("夏季 清凉 透气 ");
                } else if (temperature >= 20) {
                    query.append("春秋 舒适 ");
                } else if (temperature >= 10) {
                    query.append("秋季 保暖 ");
                } else {
                    query.append("冬季 保暖 厚实 ");
                }
                
                // 根据天气状况添加查询条件
                String weatherCondition = weatherInfo.getWeatherCondition();
                if (weatherCondition.contains("雨")) {
                    query.append("防水 ");
                } else if (weatherCondition.contains("雪")) {
                    query.append("防寒 保暖 ");
                } else if (weatherCondition.contains("晴")) {
                    query.append("防晒 ");
                }
            }
            
            // 根据场合添加查询条件
            if (occasion != null) {
                occasion = occasion.toLowerCase();
                if (occasion.contains("正式") || occasion.contains("商务")) {
                    query.append("正式 商务 ");
                } else if (occasion.contains("休闲")) {
                    query.append("休闲 舒适 ");
                } else if (occasion.contains("运动")) {
                    query.append("运动 透气 ");
                }
            }
            
            // 提取用户提到的关键词
            if (userMessage.contains("衬衫")) query.append("衬衫 ");
            if (userMessage.contains("裤子")) query.append("裤子 ");
            if (userMessage.contains("裙子")) query.append("裙子 ");
            if (userMessage.contains("外套")) query.append("外套 ");
            if (userMessage.contains("鞋子")) query.append("鞋子 ");
            
            // 添加风格关键词
            if (userMessage.contains("时尚") || userMessage.contains("潮流")) {
                query.append("时尚 潮流 ");
            } else if (userMessage.contains("简约") || userMessage.contains("休闲")) {
                query.append("简约 休闲 ");
            }
        } else {
            // 直接使用用户消息作为查询
            query.append(userMessage);
        }
        
        return query.length() > 0 ? query.toString().trim() : null;
    }

    /**
     * 生成智能回复
     * @param userMessage 用户消息
     * @param weatherInfo 天气信息
     * @param recommendedClothes 推荐衣物
     * @param occasion 场合
     * @return 回复文本
     */
    private String generateResponse(String userMessage, McpWeatherService.WeatherInfo weatherInfo, List<Map<String, Object>> recommendedClothes, String occasion) {
        StringBuilder response = new StringBuilder();
        
        // 天气信息
        if (weatherInfo != null) {
            response.append("当前").append(weatherInfo.getLocation()).append("的天气是").append(weatherInfo.getTemperature()).append("℃，").append(weatherInfo.getWeatherCondition()).append("。");
        }
        
        // 场合信息
        if (occasion != null) {
            response.append("您今天的场合是").append(occasion).append("。");
        }
        
        // 推荐衣物
        if (!recommendedClothes.isEmpty()) {
            response.append("为您推荐以下衣物：");
            for (int i = 0; i < Math.min(3, recommendedClothes.size()); i++) {
                Map<String, Object> item = recommendedClothes.get(i);
                response.append((i + 1)).append(". ").append(item.get("name")).append("（").append(item.get("category")).append("，").append(item.get("color")).append("）");
                if (i < Math.min(3, recommendedClothes.size()) - 1) {
                    response.append("；");
                }
            }
        } else {
            response.append("未找到符合条件的衣物，建议您尝试其他关键词或添加更多衣物到衣柜。");
        }
        
        // 结束语
        response.append("希望这些推荐对您有帮助！如果您有其他问题，随时告诉我。");
        
        return response.toString();
    }

    /**
     * 聊天请求DTO
     */
    public static class ChatRequest {
        private String message;
        private String location;
        private String occasion;
        private String sessionId;

        // Getters and Setters
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getOccasion() { return occasion; }
        public void setOccasion(String occasion) { this.occasion = occasion; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    }

    /**
     * 聊天响应DTO
     */
    public static class ChatResponse {
        private String response;
        private List<Map<String, Object>> recommendedClothes;
        private String weatherInfo;
        private String sessionId;

        // Getters and Setters
        public String getResponse() { return response; }
        public void setResponse(String response) { this.response = response; }
        public List<Map<String, Object>> getRecommendedClothes() { return recommendedClothes; }
        public void setRecommendedClothes(List<Map<String, Object>> recommendedClothes) { this.recommendedClothes = recommendedClothes; }
        public String getWeatherInfo() { return weatherInfo; }
        public void setWeatherInfo(String weatherInfo) { this.weatherInfo = weatherInfo; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    }

}
