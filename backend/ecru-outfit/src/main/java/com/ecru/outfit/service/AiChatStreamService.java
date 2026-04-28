package com.ecru.outfit.service;

import com.alibaba.fastjson2.JSON;
import com.ecru.common.service.ai.AiTextGeneratorStreamService;
import com.ecru.outfit.dto.request.ChatRequestDTO;
import com.ecru.outfit.entity.AiChatMessage;
import com.ecru.outfit.entity.AiConversation;
import com.ecru.outfit.mapper.AiChatMessageMapper;
import com.ecru.outfit.mapper.AiConversationMapper;
import com.ecru.outfit.service.mcp.McpWeatherService;
import com.ecru.outfit.service.rag.RagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Locale;

/**
 * AI对话流式服务
 */
@Slf4j
@Service
public class AiChatStreamService {

    @Autowired
    private AiConversationMapper conversationMapper;

    @Autowired
    private AiChatMessageMapper chatMessageMapper;

    @Autowired
    private McpWeatherService weatherService;

    @Autowired
    private RagService ragService;

    @Autowired
    private AiTextGeneratorStreamService streamService;

    @Autowired
    private com.ecru.common.service.ai.AiTextGeneratorService textGeneratorService;

    @Autowired
    private com.ecru.common.service.ai.AiPromptSettingsService promptSettingsService;

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
     * 流式发送消息并获取AI回复
     * @param userId 用户ID
     * @param request 聊天请求
     * @return 流式响应
     */
    public Flux<String> chatStream(Long userId, ChatRequestDTO request) {
        // 创建上下文容器
        AtomicReference<ChatContext> contextRef = new AtomicReference<>();

        return Mono.fromCallable(() -> {
            // 1. 获取或创建会话
            AiConversation conversation = getOrCreateConversation(userId, request);
            boolean isNewConversation = conversation.getMessageCount() == 0;

            // 2. 获取天气信息
            String weatherInfo = getWeatherInfo(request.getLocation());

            // 3. 获取对话历史
            List<Map<String, String>> chatHistory = getChatHistoryFromDb(conversation.getId());

            // 4. 保存用户消息
            saveUserMessage(conversation.getId(), userId, request.getMessage(), weatherInfo, request.getOccasion());

            String normalizedMessage = normalizeUserMessage(request.getMessage());
            if (isSimpleGreetingMessage(normalizedMessage)) {
                Map<String, Object> context = buildContext(weatherInfo, request.getOccasion(), Collections.emptyList(), false);
                ChatContext chatContext = new ChatContext();
                chatContext.setConversation(conversation);
                chatContext.setUserId(userId);
                chatContext.setChatHistory(chatHistory);
                chatContext.setContext(context);
                chatContext.setRecommendedClothes(Collections.emptyList());
                chatContext.setNeedClothingSearch(false);
                chatContext.setIsNewConversation(isNewConversation);
                chatContext.setUserMessage(normalizedMessage);
                chatContext.setDirectResponse(buildGreetingResponse());
                contextRef.set(chatContext);
                return String.format("[SESSION]%s|%s|%s",
                        conversation.getSessionId(),
                        conversation.getTitle() != null ? conversation.getTitle() : "",
                        isNewConversation);
            }

            if (isIdentityQuestionMessage(normalizedMessage)) {
                Map<String, Object> context = buildContext(weatherInfo, request.getOccasion(), Collections.emptyList(), false);
                ChatContext chatContext = new ChatContext();
                chatContext.setConversation(conversation);
                chatContext.setUserId(userId);
                chatContext.setChatHistory(chatHistory);
                chatContext.setContext(context);
                chatContext.setRecommendedClothes(Collections.emptyList());
                chatContext.setNeedClothingSearch(false);
                chatContext.setIsNewConversation(isNewConversation);
                chatContext.setUserMessage(normalizedMessage);
                chatContext.setDirectResponse(buildIdentityResponse());
                contextRef.set(chatContext);
                return String.format("[SESSION]%s|%s|%s",
                        conversation.getSessionId(),
                        conversation.getTitle() != null ? conversation.getTitle() : "",
                        isNewConversation);
            }

            // 5. 分析用户意图
            // 简化意图分析，直接根据关键词判断
            boolean needClothingSearch = isNeedClothingSearch(request.getMessage());
            log.info("是否需要检索衣柜: {}", needClothingSearch);

            // 6. 检索推荐衣物(仅在需要时)
            List<Map<String, Object>> recommendedClothes = new ArrayList<>();
            if (needClothingSearch) {
                String query = generateClothingQuery(request.getMessage(), weatherInfo, request.getOccasion());
                recommendedClothes = searchClothes(userId, query);
            }

            // 7. 构建上下文信息
            Map<String, Object> context = buildContext(weatherInfo, request.getOccasion(), recommendedClothes, needClothingSearch);

            // 保存上下文
            ChatContext chatContext = new ChatContext();
            chatContext.setConversation(conversation);
            chatContext.setUserId(userId);
            chatContext.setChatHistory(chatHistory);
            chatContext.setContext(context);
            chatContext.setRecommendedClothes(recommendedClothes);
            chatContext.setNeedClothingSearch(needClothingSearch);
            chatContext.setIsNewConversation(isNewConversation);
            chatContext.setUserMessage(normalizedMessage);
            contextRef.set(chatContext);

            // 返回会话信息作为第一条消息
            return String.format("[SESSION]%s|%s|%s",
                    conversation.getSessionId(),
                    conversation.getTitle() != null ? conversation.getTitle() : "",
                    isNewConversation);
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMapMany(sessionInfo -> {
            // 发送会话信息
            return Flux.just(sessionInfo)
                .concatWith(processStreamResponse(userId, request, contextRef));
        });
    }

    /**
     * 处理流式响应
     */
    private Flux<String> processStreamResponse(Long userId, ChatRequestDTO request, AtomicReference<ChatContext> contextRef) {
        ChatContext chatContext = contextRef.get();
        if (chatContext == null) {
            return Flux.error(new RuntimeException("上下文未初始化"));
        }

        if (chatContext.getDirectResponse() != null) {
            return Flux.just(chatContext.getDirectResponse())
                    .concatWith(Flux.just("[DONE]"))
                    .doOnComplete(() -> saveAiMessageAsync(chatContext, chatContext.getDirectResponse()));
        }

        StringBuilder fullResponse = new StringBuilder();

        return streamService.generateStreamResponse(
                request.getMessage(),
                chatContext.getChatHistory(),
                chatContext.getContext()
        )
        .doOnNext(chunk -> {
            // 累积完整响应
            if (!chunk.startsWith("[") && !chunk.startsWith("{")) {
                fullResponse.append(chunk);
            }
        })
        .doOnComplete(() -> {
            // 流完成后保存消息到数据库
            saveAiMessageAsync(chatContext, fullResponse.toString());
        })
        .doOnError(error -> {
            log.error("流式响应错误: {}", error.getMessage(), error);
        });
    }

    /**
     * 异步保存AI消息
     */
    private void saveAiMessageAsync(ChatContext chatContext, String fullResponse) {
        try {
            AiConversation conversation = chatContext.getConversation();
            Long userId = chatContext.getUserId();
            List<Map<String, Object>> recommendedClothes = chatContext.getRecommendedClothes();
            Map<String, Object> context = chatContext.getContext();
            boolean isNewConversation = chatContext.isNewConversation();

            // 保存AI回复
            AiChatMessage message = new AiChatMessage();
            message.setConversationId(conversation.getId());
            message.setUserId(userId);
            message.setRole("assistant");
            message.setContent(fullResponse);
            message.setMessageType(recommendedClothes != null && !recommendedClothes.isEmpty() ? "recommendation" : "text");
            message.setCreatedAt(LocalDateTime.now());

            if (recommendedClothes != null && !recommendedClothes.isEmpty()) {
                message.setRecommendations(JSON.toJSONString(recommendedClothes));
            }

            if (context != null && !context.isEmpty()) {
                message.setContextSnapshot(JSON.toJSONString(context));
            }

            chatMessageMapper.insert(message);

            // 更新会话消息计数
            Integer count = chatMessageMapper.countByConversationId(conversation.getId());
            conversationMapper.updateMessageCount(conversation.getId(), count);

            // 生成会话标题(如果是新会话)
            if (isNewConversation && conversation.getTitle() == null) {
                String userMsg = chatContext.getUserMessage();
                String title = generateConversationTitle(userMsg, fullResponse, chatContext.getUserId());
                conversation.setTitle(title);
                conversation.setUpdatedAt(LocalDateTime.now());
                conversationMapper.updateById(conversation);
            } else {
                conversation.setUpdatedAt(LocalDateTime.now());
                conversationMapper.updateById(conversation);
            }

            log.info("AI消息已保存, 会话ID: {}, 消息长度: {}", conversation.getSessionId(), fullResponse.length());
        } catch (Exception e) {
            log.error("保存AI消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 判断是否需要检索衣柜
     */
    private boolean isNeedClothingSearch(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return false;
        }

        String lowerMessage = userMessage.toLowerCase();
        boolean hasMaterialTerm = MATERIAL_KNOWLEDGE_TERMS.stream().anyMatch(lowerMessage::contains);
        boolean hasKnowledgeQuestion = MATERIAL_KNOWLEDGE_QUESTIONS.stream().anyMatch(lowerMessage::contains);
        if (hasMaterialTerm && hasKnowledgeQuestion) {
            log.info("消息 [{}] 识别为材质知识问答，不检索衣柜", userMessage);
            return false;
        }

        // 打招呼关键词
        Set<String> greetingKeywords = new HashSet<>(Arrays.asList(
            "你好", "您好", "嗨", "hello", "hi", "hey",
            "早上好", "下午好", "晚上好", "good morning", "good afternoon", "good evening",
            "谢谢", "感谢", "thank", "thanks",
            "再见", "拜拜", "bye", "goodbye"
        ));

        // 如果消息主要是打招呼，不需要检索
        for (String keyword : greetingKeywords) {
            String trimmedMessage = lowerMessage.trim();
            if (trimmedMessage.equals(keyword) || trimmedMessage.startsWith(keyword + " ")) {
                if (trimmedMessage.length() < keyword.length() + 10) {
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

        log.info("未检测到衣物相关需求，不需要检索衣柜");
        return false;
    }

    /**
     * 获取或创建会话
     */
    private AiConversation getOrCreateConversation(Long userId, ChatRequestDTO request) {
        String sessionId = request.getSessionId();

        if (sessionId != null && !sessionId.trim().isEmpty()) {
            AiConversation conversation = conversationMapper.selectBySessionId(sessionId);
            if (conversation != null && conversation.getUserId().equals(userId)) {
                return conversation;
            }
        }

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
        return "chat:" + userId + ":" + System.currentTimeMillis();
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

    /**
     * 生成衣物查询
     */
    private String generateClothingQuery(String userMessage, String weatherInfo, String occasion) {
        StringBuilder query = new StringBuilder();

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
    private List<Map<String, Object>> searchClothes(Long userId, String query) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return results;
        }

        try {
            var searchResults = ragService.searchClothes(userId, query, 8, null);
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
    private String normalizeUserMessage(String userMessage) {
        return userMessage == null ? "" : userMessage.trim();
    }

    private boolean isSimpleGreetingMessage(String userMessage) {
        String normalized = normalizeUserMessage(userMessage).toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return false;
        }

        normalized = normalized.replaceAll("[\\p{Punct}\\s\\u3000-\\u303F\\uFF00-\\uFF65]+", "");
        Set<String> greetingMessages = new HashSet<>(Arrays.asList(
                "你好", "您好", "哈喽", "嗨", "在吗", "在嘛", "有人吗",
                "hello", "hi", "hey", "goodmorning", "goodafternoon", "goodevening"
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
                "你是谁", "你叫什么", "你是做什么的", "你是什么", "介绍一下你自己",
                "whoareyou", "whatareyou", "introduceyourself"
        ));
        return identityQuestions.contains(normalized);
    }

    private String buildGreetingResponse() {
        return promptSettingsService.getGreetingReply();
    }

    private String buildIdentityResponse() {
        return promptSettingsService.getIdentityReply();
    }

    private Map<String, Object> buildContext(String weatherInfo, String occasion,
                                             List<Map<String, Object>> recommendedClothes,
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
        context.put("needClothingSearch", needClothingSearch);
        return context;
    }

    /**
     * 用 AI 生成会话标题，失败时降级为截断
     */
    private String generateConversationTitle(String userMessage, String aiReply, Long userId) {
        try {
            String systemPrompt = promptSettingsService.getConversationTitlePrompt();
            String prompt = "用户：" + truncateStr(userMessage, 80) + "\nAI：" + truncateStr(aiReply, 80);
            String title = textGeneratorService.generateCustomResponse(systemPrompt, prompt, null, userId);
            if (title != null) {
                title = title.trim().replaceAll("[\"'\\u201c\\u201d\\u2018\\u2019]", "");
            }
            return (title != null && !title.isEmpty()) ? title.substring(0, Math.min(title.length(), 15)) : truncateStr(userMessage, 15);
        } catch (Exception e) {
            log.warn("AI标题生成失败，降级为截断: {}", e.getMessage());
            return truncateStr(userMessage, 15);
        }
    }

    private String truncateStr(String s, int max) {
        if (s == null || s.isEmpty()) return "新对话";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    /**
     * 聊天上下文内部类
     */
    private static class ChatContext {
        private AiConversation conversation;
        private Long userId;
        private List<Map<String, String>> chatHistory;
        private Map<String, Object> context;
        private List<Map<String, Object>> recommendedClothes;
        private boolean needClothingSearch;
        private boolean isNewConversation;
        private String directResponse;
        private String userMessage;

        // Getters and Setters
        public AiConversation getConversation() { return conversation; }
        public void setConversation(AiConversation conversation) { this.conversation = conversation; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public List<Map<String, String>> getChatHistory() { return chatHistory; }
        public void setChatHistory(List<Map<String, String>> chatHistory) { this.chatHistory = chatHistory; }
        public Map<String, Object> getContext() { return context; }
        public void setContext(Map<String, Object> context) { this.context = context; }
        public List<Map<String, Object>> getRecommendedClothes() { return recommendedClothes; }
        public void setRecommendedClothes(List<Map<String, Object>> recommendedClothes) { this.recommendedClothes = recommendedClothes; }
        public boolean isNeedClothingSearch() { return needClothingSearch; }
        public void setNeedClothingSearch(boolean needClothingSearch) { this.needClothingSearch = needClothingSearch; }
        public boolean isNewConversation() { return isNewConversation; }
        public void setIsNewConversation(boolean isNewConversation) { this.isNewConversation = isNewConversation; }
        public String getDirectResponse() { return directResponse; }
        public void setDirectResponse(String directResponse) { this.directResponse = directResponse; }
        public String getUserMessage() { return userMessage; }
        public void setUserMessage(String userMessage) { this.userMessage = userMessage; }
    }
}
