package com.ecru.outfit.service;

import com.alibaba.fastjson2.JSON;
import com.ecru.common.service.ai.AiPromptSettingsService;
import com.ecru.common.service.ai.AiTextGeneratorService;
import com.ecru.outfit.dto.request.ChatRequestDTO;
import com.ecru.outfit.dto.request.CreateConversationRequestDTO;
import com.ecru.outfit.dto.response.ChatMessageVO;
import com.ecru.outfit.dto.response.ChatResponseDTO;
import com.ecru.outfit.dto.response.ConversationVO;
import com.ecru.outfit.entity.AiChatMessage;
import com.ecru.outfit.entity.AiConversation;
import com.ecru.outfit.mapper.AiChatMessageMapper;
import com.ecru.outfit.mapper.AiConversationMapper;
import com.ecru.outfit.service.mcp.McpWeatherService;
import com.ecru.outfit.service.rag.RagService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * AI对话服务
 */
@Slf4j
@Service
public class AiChatService {

    private static final Set<String> GENERIC_TITLES = new HashSet<>(Arrays.asList(
            "新对话", "新的对话", "对话", "聊天", "会话", "标题"
    ));

    @Autowired
    private AiConversationMapper conversationMapper;

    @Autowired
    private AiChatMessageMapper chatMessageMapper;

    @Autowired
    private McpWeatherService weatherService;

    @Autowired
    private RagService ragService;

    @Autowired
    private AiTextGeneratorService textGeneratorService;

    @Autowired
    private AiPromptSettingsService promptSettingsService;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private static final String CHAT_CONTEXT_PREFIX = "chat:context:";
    private static final long CONTEXT_EXPIRE_HOURS = 24;
    private static final Set<String> MATERIAL_KNOWLEDGE_TERMS = new HashSet<>(Arrays.asList(
            "羊毛", "羊绒", "羊毛衫", "羊绒衫", "棉", "纯棉", "亚麻", "真丝", "桑蚕丝", "丝绸",
            "牛仔", "牛仔布", "涤纶", "聚酯", "聚酯纤维", "粘胶", "粘纤", "viscose", "rayon",
            "linen", "silk", "wool", "cashmere", "cotton", "polyester", "denim"
    ));
    private static final Set<String> MATERIAL_KNOWLEDGE_QUESTIONS = new HashSet<>(Arrays.asList(
            "区别", "差别", "不同", "对比", "哪个好", "哪个更好", "怎么选", "是什么", "什么意思",
            "怎么养护", "怎么护理", "怎么保养", "怎么洗", "能洗吗", "洗护", "清洗", "护理",
            "特点", "优点", "缺点", "优缺点", "适合什么", "适不适合", "值不值得买", "能买吗"
    ));

    /**
     * 发送消息并获取AI回复
     * @param userId 用户ID
     * @param request 聊天请求
     * @return 聊天响应
     */
    @Transactional
    public ChatResponseDTO chat(Long userId, ChatRequestDTO request) {
        try {
            String userMessage = normalizeUserMessage(request.getMessage());
            // 1. 获取或创建会话
            AiConversation conversation = getOrCreateConversation(userId, request);
            boolean isNewConversation = conversation.getMessageCount() == 0;

            // 2. 获取天气信息
            String weatherInfo = getWeatherInfo(request.getLocation());

            // 3. 获取对话历史(用于上下文)
            List<Map<String, String>> chatHistory = getChatHistoryFromDb(conversation.getId());

            // 4. 保存用户消息
            saveUserMessage(conversation.getId(), userId, userMessage, weatherInfo, request.getOccasion());

            if (isSimpleGreetingMessage(userMessage)) {
                Map<String, Object> greetingIntent = new HashMap<>();
                greetingIntent.put("intent", "greeting");

                Map<String, Object> context = buildContext(
                        weatherInfo,
                        request.getOccasion(),
                        Collections.emptyList(),
                        greetingIntent,
                        Collections.emptyList(),
                        false
                );
                String aiResponse = buildGreetingResponse();
                Long aiMessageId = saveAiMessage(conversation.getId(), userId, aiResponse, Collections.emptyList(), context);

                conversation.setMessageCount(updateConversationMessageCount(conversation.getId()));
                updateConversationTitleIfNeeded(conversation, isNewConversation, userMessage, aiResponse, userId);
                cacheChatContext(conversation.getSessionId(), chatHistory, userMessage, aiResponse);

                return buildChatResponse(conversation, aiResponse, Collections.emptyList(), weatherInfo, aiMessageId, isNewConversation);
            }

            // 5. 分析用户意图
            if (isIdentityQuestionMessage(userMessage)) {
                Map<String, Object> identityIntent = new HashMap<>();
                identityIntent.put("intent", "identity");

                Map<String, Object> context = buildContext(
                        weatherInfo,
                        request.getOccasion(),
                        Collections.emptyList(),
                        identityIntent,
                        Collections.emptyList(),
                        false
                );
                String aiResponse = buildIdentityResponse();
                Long aiMessageId = saveAiMessage(conversation.getId(), userId, aiResponse, Collections.emptyList(), context);

                conversation.setMessageCount(updateConversationMessageCount(conversation.getId()));
                updateConversationTitleIfNeeded(conversation, isNewConversation, userMessage, aiResponse, userId);
                cacheChatContext(conversation.getSessionId(), chatHistory, userMessage, aiResponse);

                return buildChatResponse(conversation, aiResponse, Collections.emptyList(), weatherInfo, aiMessageId, isNewConversation);
            }

            Map<String, Object> intentAnalysis = textGeneratorService.analyzeQueryIntent(userMessage);
            log.info("查询意图分析结果: {}", intentAnalysis);

            // 6. 判断是否需要检索衣柜
            boolean needClothingSearch = isNeedClothingSearch(intentAnalysis, userMessage);
            log.info("是否需要检索衣柜: {}", needClothingSearch);

            // 7. 提取负面偏好
            List<String> negativePreferences = extractNegativePreferences(intentAnalysis);

            // 8. 检索推荐衣物(仅在需要时)
            List<Map<String, Object>> recommendedClothes = new ArrayList<>();
            if (needClothingSearch) {
                String query = generateClothingQuery(userMessage, weatherInfo, request.getOccasion(), intentAnalysis);
                recommendedClothes = searchClothes(userId, query, negativePreferences);
            }

            // 9. 构建上下文信息
            Map<String, Object> context = buildContext(weatherInfo, request.getOccasion(), recommendedClothes, intentAnalysis, negativePreferences, needClothingSearch);

            // 10. 生成AI回复
            String aiResponse = textGeneratorService.generateResponse(userMessage, chatHistory, context);

            // 11. 保存AI回复
            Long aiMessageId = saveAiMessage(conversation.getId(), userId, aiResponse, recommendedClothes, context);

            // 12. 更新会话消息计数
            conversation.setMessageCount(updateConversationMessageCount(conversation.getId()));

            // 13. 生成会话标题(如果是新会话)
            updateConversationTitleIfNeeded(conversation, isNewConversation, userMessage, aiResponse, userId);

            // 14. 缓存上下文到Redis
            cacheChatContext(conversation.getSessionId(), chatHistory, userMessage, aiResponse);

            // 14. 构建响应
            return buildChatResponse(conversation, aiResponse, recommendedClothes, weatherInfo, aiMessageId, isNewConversation);

        } catch (Exception e) {
            log.error("AI对话失败: {}", e.getMessage(), e);
            throw new RuntimeException("AI对话失败", e);
        }
    }

    /**
     * 获取或创建会话
     */
    private AiConversation getOrCreateConversation(Long userId, ChatRequestDTO request) {
        String sessionId = request.getSessionId();

        if (sessionId != null && !sessionId.trim().isEmpty()) {
            // 尝试获取现有会话
            AiConversation conversation = conversationMapper.selectBySessionId(sessionId);
            if (conversation != null && conversation.getUserId().equals(userId)) {
                return conversation;
            }
        }

        // 创建新会话
        return createNewConversation(userId, request);
    }

    /**
     * 创建新会话
     */
    private AiConversation createNewConversation(Long userId, ChatRequestDTO request) {
        AiConversation conversation = new AiConversation();
        conversation.setUserId(userId);
        conversation.setSessionId(generateSessionId(userId));
        conversation.setContext(request.getContext() != null ? request.getContext() : "general");
        conversation.setIsActive(true);
        conversation.setMessageCount(0);
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());

        // 构建metadata
        Map<String, Object> metadata = new HashMap<>();
        if (request.getLocation() != null) {
            metadata.put("location", request.getLocation());
        }
        if (request.getOccasion() != null) {
            metadata.put("occasion", request.getOccasion());
        }
        if (request.getMetadata() != null) {
            metadata.putAll(request.getMetadata());
        }
        conversation.setMetadata(JSON.toJSONString(metadata));

        conversationMapper.insert(conversation);
        return conversation;
    }

    /**
     * 生成会话ID
     */
    private String generateSessionId(Long userId) {
        return "chat:" + userId + ":" + System.currentTimeMillis() + ":" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 获取天气信息
     */
    private String getWeatherInfo(String location) {
        if (location == null || location.trim().isEmpty()) {
            return null;
        }
        try {
            McpWeatherService.WeatherInfo weatherInfo = weatherService.getWeatherByLocation(location);
            if (weatherInfo != null) {
                return weatherInfo.getLocation() + "，" + weatherInfo.getTemperature() + "℃，" + weatherInfo.getWeatherCondition();
            }
        } catch (Exception e) {
            log.warn("获取天气信息失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 从数据库获取对话历史
     */
    private List<Map<String, String>> getChatHistoryFromDb(Long conversationId) {
        List<AiChatMessage> messages = chatMessageMapper.selectRecentMessages(conversationId, 10);
        List<Map<String, String>> history = new ArrayList<>();

        // 按时间正序排列
        Collections.reverse(messages);

        for (AiChatMessage message : messages) {
            Map<String, String> msg = new HashMap<>();
            msg.put("role", message.getRole());
            msg.put("content", message.getContent());
            history.add(msg);
        }

        return history;
    }

    /**
     * 保存用户消息
     */
    private void saveUserMessage(Long conversationId, Long userId, String content, String weatherInfo, String occasion) {
        AiChatMessage message = new AiChatMessage();
        message.setConversationId(conversationId);
        message.setUserId(userId);
        message.setRole("user");
        message.setContent(content);
        message.setMessageType("text");
        message.setCreatedAt(LocalDateTime.now());

        // 保存上下文快照
        Map<String, Object> contextSnapshot = new HashMap<>();
        if (weatherInfo != null) {
            contextSnapshot.put("weather", weatherInfo);
        }
        if (occasion != null) {
            contextSnapshot.put("occasion", occasion);
        }
        if (!contextSnapshot.isEmpty()) {
            message.setContextSnapshot(JSON.toJSONString(contextSnapshot));
        }

        chatMessageMapper.insert(message);
    }

    private String normalizeUserMessage(String message) {
        return message == null ? "" : message.trim();
    }

    private boolean isSimpleGreetingMessage(String userMessage) {
        String normalized = normalizeUserMessage(userMessage).toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return false;
        }

        normalized = normalized.replaceAll("[\\p{Punct}\\s\\u3000-\\u303F\\uFF00-\\uFF65]+", "");

        Set<String> greetingMessages = new HashSet<>(Arrays.asList(
                "\u4f60\u597d",
                "\u60a8\u597d",
                "\u54c8\u55bd",
                "\u55e8",
                "\u5728\u5417",
                "\u5728\u561b",
                "\u6709\u4eba\u5417",
                "hello",
                "hi",
                "hey",
                "goodmorning",
                "goodafternoon",
                "goodevening"
        ));

        return greetingMessages.contains(normalized);
    }

    private boolean isIdentityQuestionMessage(String userMessage) {
        String normalized = normalizeUserMessage(userMessage).toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return false;
        }

        normalized = normalized.replaceAll("[\\p{Punct}\\s\\u3000-\\u303F\\uFF00-\\uFF65]+", "");

        Set<String> identityQuestions = new HashSet<>(Arrays.asList(
                "\u4f60\u662f\u8c01",
                "\u4f60\u53eb\u4ec0\u4e48",
                "\u4f60\u662f\u505a\u4ec0\u4e48\u7684",
                "\u4f60\u662f\u4ec0\u4e48",
                "\u4ecb\u7ecd\u4e00\u4e0b\u4f60\u81ea\u5df1",
                "whoareyou",
                "whatareyou",
                "introduceyourself"
        ));

        return identityQuestions.contains(normalized);
    }

    private String buildGreetingResponse() {
        return promptSettingsService.getGreetingReply();
    }

    private String buildIdentityResponse() {
        return promptSettingsService.getIdentityReply();
    }

    /**
     * 保存AI回复
     */
    private Long saveAiMessage(Long conversationId, Long userId, String content,
                               List<Map<String, Object>> recommendations, Map<String, Object> context) {
        AiChatMessage message = new AiChatMessage();
        message.setConversationId(conversationId);
        message.setUserId(userId);
        message.setRole("assistant");
        message.setContent(content);
        message.setMessageType(recommendations != null && !recommendations.isEmpty() ? "recommendation" : "text");
        message.setCreatedAt(LocalDateTime.now());

        if (recommendations != null && !recommendations.isEmpty()) {
            message.setRecommendations(JSON.toJSONString(recommendations));
        }

        if (context != null && !context.isEmpty()) {
            message.setContextSnapshot(JSON.toJSONString(context));
        }

        chatMessageMapper.insert(message);
        return message.getId();
    }

    /**
     * 判断是否需要检索衣柜
     * 根据意图分析结果判断用户是否要求衣物推荐
     */
    private boolean isNeedClothingSearch(Map<String, Object> intentAnalysis, String userMessage) {
        if (intentAnalysis == null) {
            return false;
        }
        if (isMaterialKnowledgeQuestion(userMessage, intentAnalysis)) {
            log.info("消息 [{}] 识别为材质知识问答，不检索衣柜", userMessage);
            return false;
        }

        // 获取意图类型
        String intent = readIntentText(intentAnalysis.get("intent"));

        // 不需要检索衣柜的意图类型
        Set<String> noSearchIntents = new HashSet<>(Arrays.asList(
            "打招呼", "问候", "寒暄", "闲聊", "感谢", "告别", "再见",
            "greeting", "identity", "chat", "thanks", "goodbye", "farewell",
            "系统设置", "帮助", "说明", "介绍"
        ));

        if (intent != null && noSearchIntents.contains(intent.toLowerCase())) {
            log.info("意图 [{}] 不需要检索衣柜", intent);
            return false;
        }

        // 检查关键词
        String lowerMessage = userMessage.toLowerCase();
        Set<String> greetingKeywords = new HashSet<>(Arrays.asList(
            "你好", "您好", "嗨", "hello", "hi", "hey",
            "早上好", "下午好", "晚上好", "good morning", "good afternoon", "good evening",
            "谢谢", "感谢", "thank", "thanks",
            "再见", "拜拜", "bye", "goodbye",
            "你是谁", "你能做什么", "介绍一下", "help", "帮助"
        ));

        // 如果消息主要是打招呼，不需要检索
        for (String keyword : greetingKeywords) {
            if (lowerMessage.trim().equals(keyword) || lowerMessage.trim().startsWith(keyword + " ")) {
                if (lowerMessage.length() < keyword.length() + 10) {
                    log.info("消息 [{}] 主要是打招呼，不需要检索衣柜", userMessage);
                    return false;
                }
            }
        }

        // 检查是否有衣物相关关键词
        Set<String> clothingKeywords = new HashSet<>(Arrays.asList(
            "穿", "搭配", "衣服", "服装", "穿搭", "推荐", "建议",
            "衬衫", "裤子", "裙子", "外套", "鞋子", "毛衣", "t恤", "牛仔裤",
            "风格", "时尚", "颜色", "款式", "衣柜", "衣橱",
            "今天", "明天", "天气", "场合", "约会", "工作", "聚会",
            "冷", "热", "保暖", "凉爽", "正式", "休闲"
        ));

        for (String keyword : clothingKeywords) {
            if (lowerMessage.contains(keyword)) {
                log.info("消息包含衣物相关关键词 [{}]，需要检索衣柜", keyword);
                return true;
            }
        }

        // 默认情况下，如果意图不明确且有衣物类型，则检索
        if (intentAnalysis.containsKey("clothingType")) {
            String clothingType = readIntentText(intentAnalysis.get("clothingType"));
            if (clothingType != null && !clothingType.isEmpty()) {
                log.info("意图分析包含衣物类型 [{}]，需要检索衣柜", clothingType);
                return true;
            }
        }

        log.info("未检测到衣物相关需求，不需要检索衣柜");
        return false;
    }

    /**
     * 提取负面偏好
     */
    private boolean isMaterialKnowledgeQuestion(String userMessage, Map<String, Object> intentAnalysis) {
        String lowerMessage = userMessage == null ? "" : userMessage.toLowerCase();
        boolean hasMaterialTerm = MATERIAL_KNOWLEDGE_TERMS.stream().anyMatch(lowerMessage::contains);
        boolean hasKnowledgeQuestion = MATERIAL_KNOWLEDGE_QUESTIONS.stream().anyMatch(lowerMessage::contains);

        if (!hasMaterialTerm && intentAnalysis != null) {
            String clothingType = readIntentText(intentAnalysis.get("clothingType"));
            if (clothingType != null) {
                String normalizedType = clothingType.toLowerCase();
                hasMaterialTerm = MATERIAL_KNOWLEDGE_TERMS.stream().anyMatch(normalizedType::contains);
            }
        }

        String intent = intentAnalysis == null ? null : readIntentText(intentAnalysis.get("intent"));
        boolean knowledgeIntent = intent != null
                && (intent.toLowerCase().contains("knowledge")
                || intent.contains("知识")
                || intent.contains("材质")
                || intent.contains("面料"));

        return hasMaterialTerm && (hasKnowledgeQuestion || knowledgeIntent);
    }

    private List<String> extractNegativePreferences(Map<String, Object> intentAnalysis) {
        List<String> negativePreferences = new ArrayList<>();
        if (intentAnalysis != null && intentAnalysis.containsKey("negativePreferences")) {
            Object negativePrefs = intentAnalysis.get("negativePreferences");
            if (negativePrefs instanceof List) {
                for (Object pref : (List<?>) negativePrefs) {
                    if (pref instanceof String) {
                        negativePreferences.add((String) pref);
                    }
                }
            }
        }
        return negativePreferences;
    }

    /**
     * 生成衣物查询
     */
    private String generateClothingQuery(String userMessage, String weatherInfo, String occasion, Map<String, Object> intentAnalysis) {
        StringBuilder query = new StringBuilder();

        // 根据意图分析添加查询条件
        if (intentAnalysis != null) {
            if (intentAnalysis.containsKey("style")) {
                String style = readIntentText(intentAnalysis.get("style"));
                if (style != null) {
                    query.append(style).append(" ");
                }
            }
            if (intentAnalysis.containsKey("season")) {
                String season = readIntentText(intentAnalysis.get("season"));
                if (season != null) {
                    query.append(season).append(" ");
                }
            }
            if (intentAnalysis.containsKey("clothingType")) {
                String clothingType = readIntentText(intentAnalysis.get("clothingType"));
                if (clothingType != null) {
                    query.append(clothingType).append(" ");
                }
            }
            if (intentAnalysis.containsKey("keywords")) {
                @SuppressWarnings("unchecked")
                List<String> keywords = (List<String>) intentAnalysis.get("keywords");
                if (keywords != null) {
                    for (String keyword : keywords) {
                        query.append(keyword).append(" ");
                    }
                }
            }
        }

        // 提取用户提到的关键词
        String lowerMessage = userMessage.toLowerCase();
        if (lowerMessage.contains("衬衫")) query.append("衬衫 ");
        if (lowerMessage.contains("裤子")) query.append("裤子 ");
        if (lowerMessage.contains("裙子")) query.append("裙子 ");
        if (lowerMessage.contains("外套")) query.append("外套 ");
        if (lowerMessage.contains("鞋子")) query.append("鞋子 ");
        if (lowerMessage.contains("毛衣")) query.append("毛衣 ");
        if (lowerMessage.contains("t恤")) query.append("T恤 ");
        if (lowerMessage.contains("牛仔裤")) query.append("牛仔裤 ");

        if (query.length() == 0) {
            query.append(userMessage);
        }

        return query.toString().trim();
    }

    /**
     * 搜索衣物
     */
    private String readIntentText(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String text) {
            String normalized = text.trim();
            return normalized.isEmpty() ? null : normalized;
        }

        if (value instanceof Collection<?> values) {
            String joined = values.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(item -> !item.isEmpty())
                    .collect(Collectors.joining(" "));
            return joined.isEmpty() ? null : joined;
        }

        String normalized = String.valueOf(value).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private List<Map<String, Object>> searchClothes(Long userId, String query, List<String> negativePreferences) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return results;
        }

        try {
            var searchResults = ragService.searchClothes(userId, query, 8, negativePreferences);
            for (var result : searchResults) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", result.getClothingId());
                item.put("name", result.getName());
                item.put("category", result.getCategory());
                item.put("color", result.getPrimaryColor());
                item.put("imageUrl", result.getImageUrl());
                item.put("similarity", result.getSimilarity());
                results.add(item);
            }
        } catch (Exception e) {
            log.warn("搜索衣物失败: {}", e.getMessage());
        }

        return results;
    }

    /**
     * 构建上下文信息
     */
    private Map<String, Object> buildContext(String weatherInfo, String occasion,
                                             List<Map<String, Object>> recommendedClothes,
                                             Map<String, Object> intentAnalysis,
                                             List<String> negativePreferences,
                                             boolean needClothingSearch) {
        Map<String, Object> context = new HashMap<>();
        if (weatherInfo != null) {
            context.put("weather", weatherInfo);
        }
        if (occasion != null) {
            context.put("occasion", occasion);
        }
        if (!recommendedClothes.isEmpty()) {
            context.put("clothes", recommendedClothes);
        }
        if (intentAnalysis != null) {
            context.put("intent", intentAnalysis);
        }
        if (!negativePreferences.isEmpty()) {
            context.put("negativePreferences", negativePreferences);
        }
        // 添加是否需要检索衣柜的标志，用于AI理解上下文
        context.put("needClothingSearch", needClothingSearch);
        return context;
    }

    /**
     * 更新会话标题（如果是新会话）
     */
    private void updateConversationTitleIfNeeded(AiConversation conversation, boolean isNewConversation, String userMessage, String aiReply, Long userId) {
        if (isNewConversation && conversation.getTitle() == null) {
            String title = generateConversationTitle(userMessage, aiReply, userId);
            conversation.setTitle(title);
            conversationMapper.updateById(conversation);
        }
    }

    /**
     * 用 AI 生成会话标题，失败时降级为截断
     */
    private String generateConversationTitle(String userMessage, String aiReply, Long userId) {
        try {
            String systemPrompt = promptSettingsService.getConversationTitlePrompt();
            String prompt = "用户：" + truncateStr(userMessage, 80) + "\nAI：" + truncateStr(aiReply, 80);
            String title = textGeneratorService.generateCustomResponse(systemPrompt, prompt, null, userId);
            return normalizeConversationTitle(title, userMessage);
        } catch (Exception e) {
            log.warn("AI标题生成失败，降级为截断: {}", e.getMessage());
            return truncateStr(userMessage, 15);
        }
    }

    private String normalizeConversationTitle(String title, String userMessage) {
        String normalized = title == null ? "" : title.trim()
                .replaceAll("[\"'“”‘’]", "")
                .replaceAll("\\s+", " ");

        if (!normalized.isEmpty()) {
            normalized = normalized.substring(0, Math.min(normalized.length(), 15));
        }

        if (normalized.isEmpty() || GENERIC_TITLES.contains(normalized)) {
            return truncateStr(userMessage, 15);
        }

        return normalized;
    }

    private String truncateStr(String s, int max) {
        if (s == null || s.isEmpty()) return "新对话";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private ChatResponseDTO buildChatResponse(AiConversation conversation,
                                              String aiResponse,
                                              List<Map<String, Object>> recommendedClothes,
                                              String weatherInfo,
                                              Long aiMessageId,
                                              boolean isNewConversation) {
        ChatResponseDTO response = new ChatResponseDTO();
        response.setSessionId(conversation.getSessionId());
        response.setResponse(aiResponse);
        response.setRecommendedClothes(recommendedClothes);
        response.setWeatherInfo(weatherInfo);
        response.setMessageId(aiMessageId);
        response.setConversationTitle(conversation.getTitle());
        response.setIsNewConversation(isNewConversation);
        return response;
    }

    private Integer updateConversationMessageCount(Long conversationId) {
        Integer count = chatMessageMapper.countByConversationId(conversationId);
        conversationMapper.updateMessageCount(conversationId, count);
        return count == null ? 0 : count;
    }

    /**
     * 缓存聊天上下文到Redis
     */
    private void cacheChatContext(String sessionId, List<Map<String, String>> history,
                                  String userMessage, String aiResponse) {
        if (redisTemplate == null) {
            return;
        }

        try {
            // 添加最新消息到历史
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            history.add(userMsg);

            Map<String, String> aiMsg = new HashMap<>();
            aiMsg.put("role", "assistant");
            aiMsg.put("content", aiResponse);
            history.add(aiMsg);

            // 只保留最近20条消息
            if (history.size() > 20) {
                history = history.subList(history.size() - 20, history.size());
            }

            String key = CHAT_CONTEXT_PREFIX + sessionId;
            redisTemplate.opsForValue().set(key, JSON.toJSONString(history),
                    CONTEXT_EXPIRE_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("缓存聊天上下文失败: {}", e.getMessage());
        }
    }

    // ==================== 会话管理接口 ====================

    /**
     * 获取用户的会话列表
     */
    public Page<ConversationVO> getUserConversations(Long userId, Integer page, Integer size) {
        PageHelper.startPage(page, size);
        Page<AiConversation> conversations = conversationMapper.selectByUserId(userId);

        Page<ConversationVO> result = new Page<>();
        result.setPageNum(conversations.getPageNum());
        result.setPageSize(conversations.getPageSize());
        result.setTotal(conversations.getTotal());
        result.setPages(conversations.getPages());

        List<ConversationVO> voList = conversations.getResult().stream()
                .map(this::convertToConversationVO)
                .collect(Collectors.toList());
        result.addAll(voList);

        return result;
    }

    /**
     * 获取会话详情
     */
    public ConversationVO getConversationDetail(Long userId, String sessionId) {
        AiConversation conversation = conversationMapper.selectBySessionId(sessionId);
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            return null;
        }
        return convertToConversationVO(conversation);
    }

    /**
     * 更新会话标题
     */
    @Transactional
    public boolean updateConversationTitle(Long userId, String sessionId, String title) {
        AiConversation conversation = conversationMapper.selectBySessionId(sessionId);
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            return false;
        }
        conversation.setTitle(title);
        conversation.setUpdatedAt(LocalDateTime.now());
        return conversationMapper.updateById(conversation) > 0;
    }

    /**
     * 删除会话
     */
    @Transactional
    public boolean deleteConversation(Long userId, String sessionId) {
        AiConversation conversation = conversationMapper.selectBySessionId(sessionId);
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            return false;
        }

        // 删除会话的所有消息
        chatMessageMapper.deleteByConversationId(conversation.getId());

        // 删除会话
        return conversationMapper.deleteById(conversation.getId()) > 0;
    }

    /**
     * 设置会话活跃状态
     */
    @Transactional
    public boolean setConversationActive(Long userId, String sessionId, Boolean isActive) {
        AiConversation conversation = conversationMapper.selectBySessionId(sessionId);
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            return false;
        }
        conversation.setIsActive(isActive);
        conversation.setUpdatedAt(LocalDateTime.now());
        return conversationMapper.updateById(conversation) > 0;
    }

    // ==================== 消息管理接口 ====================

    /**
     * 获取会话的消息列表
     */
    public Page<ChatMessageVO> getConversationMessages(Long userId, String sessionId, Integer page, Integer size) {
        AiConversation conversation = conversationMapper.selectBySessionId(sessionId);
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            return new Page<>();
        }

        PageHelper.startPage(page, size);
        Page<AiChatMessage> messages = chatMessageMapper.selectByConversationIdPage(conversation.getId());

        Page<ChatMessageVO> result = new Page<>();
        result.setPageNum(messages.getPageNum());
        result.setPageSize(messages.getPageSize());
        result.setTotal(messages.getTotal());
        result.setPages(messages.getPages());

        List<ChatMessageVO> voList = messages.getResult().stream()
                .map(this::convertToChatMessageVO)
                .collect(Collectors.toList());
        result.addAll(voList);

        return result;
    }

    /**
     * 获取会话的所有消息(不分页)
     */
    public List<ChatMessageVO> getAllConversationMessages(Long userId, String sessionId) {
        AiConversation conversation = conversationMapper.selectBySessionId(sessionId);
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            return new ArrayList<>();
        }

        List<AiChatMessage> messages = chatMessageMapper.selectByConversationId(conversation.getId());
        return messages.stream()
                .map(this::convertToChatMessageVO)
                .collect(Collectors.toList());
    }

    // ==================== 转换方法 ====================

    private ConversationVO convertToConversationVO(AiConversation conversation) {
        ConversationVO vo = new ConversationVO();
        vo.setId(conversation.getId());
        vo.setSessionId(conversation.getSessionId());
        vo.setTitle(conversation.getTitle());
        vo.setContext(conversation.getContext());
        vo.setIsActive(conversation.getIsActive());
        vo.setMessageCount(conversation.getMessageCount());
        vo.setCreatedAt(conversation.getCreatedAt());
        vo.setUpdatedAt(conversation.getUpdatedAt());

        if (conversation.getMetadata() != null) {
            vo.setMetadata(JSON.parseObject(conversation.getMetadata(), Map.class));
        }

        // 获取最后一条消息作为预览
        List<AiChatMessage> recentMessages = chatMessageMapper.selectRecentMessages(conversation.getId(), 1);
        if (!recentMessages.isEmpty()) {
            String content = recentMessages.get(0).getContent();
            vo.setLastMessagePreview(content.length() > 50 ? content.substring(0, 50) + "..." : content);
        }

        return vo;
    }

    private ChatMessageVO convertToChatMessageVO(AiChatMessage message) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(message.getId());
        vo.setRole(message.getRole());
        vo.setContent(message.getContent());
        vo.setMessageType(message.getMessageType());
        vo.setCreatedAt(message.getCreatedAt());

        if (message.getRecommendations() != null) {
            vo.setRecommendations(JSON.parseObject(message.getRecommendations(), List.class));
        }

        if (message.getContextSnapshot() != null) {
            vo.setContextSnapshot(JSON.parseObject(message.getContextSnapshot(), Map.class));
        }

        if (message.getMetadata() != null) {
            vo.setMetadata(JSON.parseObject(message.getMetadata(), Map.class));
        }

        return vo;
    }

}
