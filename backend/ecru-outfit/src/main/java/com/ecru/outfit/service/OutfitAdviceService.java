package com.ecru.outfit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecru.outfit.dto.response.OutfitAdviceDetailDTO;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    @Autowired
    @Qualifier("aiTextGeneratorService")
    private com.ecru.common.service.ai.AiTextGeneratorService textGeneratorService;

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
    public OutfitAdviceDetailDTO getAdviceById(Long id, Long userId) {
        OutfitAdviceRecord record = getOwnedAdviceRecord(id, userId);
        if (record == null) {
            return null;
        }

        OutfitAdviceDetailDTO detail = new OutfitAdviceDetailDTO();
        detail.setRecord(record);
        detail.setItems(outfitItemMapper.selectByOutfitAdviceId(id));
        detail.setFeedback(outfitFeedbackMapper.selectByOutfitAndUser(id, userId));
        return detail;
    }

    /**
     * 删除搭配记录
     * @param id 搭配记录ID
     * @return 是否成功
     */
    @Transactional
    public boolean deleteAdvice(Long id, Long userId) {
        if (getOwnedAdviceRecord(id, userId) == null) {
            return false;
        }
        outfitFeedbackMapper.delete(new LambdaQueryWrapper<OutfitFeedback>()
                .eq(OutfitFeedback::getOutfitAdviceId, id)
                .eq(OutfitFeedback::getUserId, userId));
        if (getOwnedAdviceRecord(id, userId) == null) {
            return false;
        }
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
    public boolean toggleFavorite(Long id, Long userId, Boolean isFavorite) {
        OutfitAdviceRecord record = getOwnedAdviceRecord(id, userId);
        if (record == null) {
            return false;
        }
        record.setIsFavorite(isFavorite);
        record.setUpdatedAt(LocalDateTime.now());
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
        if (getOwnedAdviceRecord(outfitAdviceId, userId) == null) {
            return null;
        }

        OutfitFeedback existing = outfitFeedbackMapper.selectByOutfitAndUser(outfitAdviceId, userId);
        LocalDateTime now = LocalDateTime.now();

        if (existing != null) {
            existing.setOverallRating(feedback.getOverallRating());
            existing.setStyleRating(feedback.getStyleRating());
            existing.setPracticalityRating(feedback.getPracticalityRating());
            existing.setWeatherRating(feedback.getWeatherRating());
            existing.setIsWorn(feedback.getIsWorn());
            existing.setWornAt(feedback.getWornAt());
            existing.setFeedbackText(feedback.getFeedbackText());
            existing.setUpdatedAt(now);
            outfitFeedbackMapper.updateById(existing);
            return existing;
        }

        feedback.setOutfitAdviceId(outfitAdviceId);
        feedback.setUserId(userId);
        feedback.setCreatedAt(now);
        feedback.setUpdatedAt(now);
        outfitFeedbackMapper.insert(feedback);
        return feedback;
    }

    /**
     * 获取用户风格档案
     * @param userId 用户ID
     * @return 风格档案
     */
    private OutfitAdviceRecord getOwnedAdviceRecord(Long id, Long userId) {
        OutfitAdviceRecord record = outfitAdviceRecordMapper.selectById(id);
        if (record == null || !Objects.equals(record.getUserId(), userId)) {
            return null;
        }
        return record;
    }

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
        UserStyleProfile existing = userStyleProfileMapper.selectByUserId(profile.getUserId());
        LocalDateTime now = LocalDateTime.now();

        if (existing == null) {
            profile.setId(null);
            profile.setCreatedAt(now);
            profile.setUpdatedAt(now);
            return userStyleProfileMapper.insert(profile) > 0;
        }

        profile.setId(existing.getId());
        profile.setCreatedAt(existing.getCreatedAt());
        profile.setUpdatedAt(now);
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
            
            // 5. 使用AI分析查询意图
            Map<String, Object> intentAnalysis = textGeneratorService.analyzeQueryIntent(userMessage);
            log.info("查询意图分析结果: {}", intentAnalysis);
            
            // 6. 将当前消息添加到历史
            Map<String, String> userMessageMap = new HashMap<>();
            userMessageMap.put("role", "user");
            userMessageMap.put("content", userMessage);
            chatHistory.add(userMessageMap);

            // 7. 生成衣物查询
            String query = generateClothingQuery(userMessage, weatherInfo, chatRequest.getOccasion(), intentAnalysis);

            // 8. 从意图分析中提取负面偏好
            List<String> negativePreferences = new ArrayList<>();
            if (intentAnalysis.containsKey("negativePreferences")) {
                Object negativePrefs = intentAnalysis.get("negativePreferences");
                if (negativePrefs instanceof List) {
                    for (Object pref : (List<?>) negativePrefs) {
                        if (pref instanceof String) {
                            negativePreferences.add((String) pref);
                        }
                    }
                }
            }
            log.info("负面偏好: {}", negativePreferences);

            // 9. 从衣柜中检索衣物
            List<Map<String, Object>> recommendedClothes = new ArrayList<>();
            if (query != null) {
                var results = ragService.searchClothes(userId, query, 8, negativePreferences);
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

            // 10. 构建上下文信息
            Map<String, Object> context = new HashMap<>();
            context.put("weather", weatherInfoStr);
            context.put("occasion", chatRequest.getOccasion());
            context.put("clothes", recommendedClothes);
            context.put("intent", intentAnalysis);
            context.put("negativePreferences", negativePreferences);

            // 11. 使用AI生成智能回复
            String response = textGeneratorService.generateResponse(userMessage, chatHistory, context);

            // 12. 将回复添加到历史
            Map<String, String> assistantMessageMap = new HashMap<>();
            assistantMessageMap.put("role", "assistant");
            assistantMessageMap.put("content", response);
            chatHistory.add(assistantMessageMap);

            // 13. 保存聊天历史
            saveChatHistory(userId, sessionId, chatHistory);

            // 14. 构建响应
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
                    try {
                        // 尝试解析聊天历史
                        Object parsed = com.alibaba.fastjson2.JSON.parse(cachedData);
                        if (parsed instanceof List) {
                            List<Map<String, String>> history = new ArrayList<>();
                            for (Object item : (List<?>) parsed) {
                                if (item instanceof Map) {
                                    Map<String, String> convertedItem = new HashMap<>();
                                    for (Map.Entry<?, ?> entry : ((Map<?, ?>) item).entrySet()) {
                                        if (entry.getKey() instanceof String) {
                                            convertedItem.put(
                                                (String) entry.getKey(),
                                                entry.getValue() != null ? entry.getValue().toString() : null
                                            );
                                        }
                                    }
                                    history.add(convertedItem);
                                }
                            }
                            return history;
                        }
                    } catch (Exception e) {
                        log.warn("解析聊天历史失败: {}", e.getMessage());
                        return new ArrayList<>();
                    }
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
     * @param intentAnalysis 意图分析结果
     * @return 查询文本
     */
    private String generateClothingQuery(String userMessage, McpWeatherService.WeatherInfo weatherInfo, String occasion, Map<String, Object> intentAnalysis) {
        StringBuilder query = new StringBuilder();
        
        // 分析用户消息
        userMessage = userMessage.toLowerCase();
        
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
            } else if (occasion.contains("约会")) {
                query.append("时尚 优雅 ");
            } else if (occasion.contains("聚会")) {
                query.append("时尚 派对 ");
            }
        }
        
        // 根据意图分析结果添加查询条件
        if (intentAnalysis != null) {
            // 添加风格
            if (intentAnalysis.containsKey("style")) {
                String style = (String) intentAnalysis.get("style");
                if (style != null) {
                    query.append(style).append(" ");
                }
            }
            
            // 添加季节
            if (intentAnalysis.containsKey("season")) {
                String season = (String) intentAnalysis.get("season");
                if (season != null) {
                    query.append(season).append(" ");
                }
            }
            
            // 添加天气需求
            if (intentAnalysis.containsKey("weather")) {
                String weatherNeed = (String) intentAnalysis.get("weather");
                if (weatherNeed != null) {
                    query.append(weatherNeed).append(" ");
                }
            }
            
            // 添加衣物类型
            if (intentAnalysis.containsKey("clothingType")) {
                String clothingType = (String) intentAnalysis.get("clothingType");
                if (clothingType != null) {
                    query.append(clothingType).append(" ");
                }
            }
            
            // 添加关键词
            if (intentAnalysis.containsKey("keywords")) {
                List<String> keywords = (List<String>) intentAnalysis.get("keywords");
                if (keywords != null && !keywords.isEmpty()) {
                    for (String keyword : keywords) {
                        query.append(keyword).append(" ");
                    }
                }
            }
        }
        
        // 提取用户提到的关键词
        if (userMessage.contains("衬衫")) query.append("衬衫 ");
        if (userMessage.contains("裤子")) query.append("裤子 ");
        if (userMessage.contains("裙子")) query.append("裙子 ");
        if (userMessage.contains("外套")) query.append("外套 ");
        if (userMessage.contains("鞋子")) query.append("鞋子 ");
        if (userMessage.contains("毛衣")) query.append("毛衣 ");
        if (userMessage.contains("T恤")) query.append("T恤 ");
        if (userMessage.contains("牛仔裤")) query.append("牛仔裤 ");
        
        // 添加风格关键词
        if (userMessage.contains("时尚") || userMessage.contains("潮流")) {
            query.append("时尚 潮流 ");
        } else if (userMessage.contains("简约") || userMessage.contains("休闲")) {
            query.append("简约 休闲 ");
        } else if (userMessage.contains("正式") || userMessage.contains("商务")) {
            query.append("正式 商务 ");
        }
        
        // 如果查询为空，使用用户消息作为查询
        if (query.length() == 0) {
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
