package com.ecru.outfit.service;

import com.alibaba.fastjson2.JSON;
import com.ecru.common.service.ai.AiPromptSettingsService;
import com.ecru.common.service.ai.AiTextGeneratorService;
import com.ecru.common.service.ai.AiTextGeneratorStreamService;
import com.ecru.outfit.dto.request.ChatRequestDTO;
import com.ecru.outfit.entity.AiChatMessage;
import com.ecru.outfit.entity.AiConversation;
import com.ecru.outfit.mapper.AiChatMessageMapper;
import com.ecru.outfit.mapper.AiConversationMapper;
import com.ecru.outfit.service.agent.WardrobeChatAgentService;
import com.ecru.outfit.service.mcp.McpWeatherService;
import com.ecru.outfit.service.rag.RagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class AiChatStreamService {

    private static final Set<String> GENERIC_TITLES = new HashSet<>(Arrays.asList(
            "聊天",
            "对话",
            "新对话",
            "AI聊天",
            "chat"
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
    private AiTextGeneratorStreamService streamService;

    @Autowired
    private AiTextGeneratorService textGeneratorService;

    @Autowired
    private AiPromptSettingsService promptSettingsService;

    @Autowired
    private WardrobeChatAgentService wardrobeChatAgentService;

    public Flux<String> chatStream(Long userId, ChatRequestDTO request) {
        AtomicReference<ChatContext> contextRef = new AtomicReference<>();

        return Mono.fromCallable(() -> {
                    AiConversation conversation = getOrCreateConversation(userId, request);
                    boolean isNewConversation = conversation.getMessageCount() == 0;
                    String weatherInfo = getWeatherInfo(request.getLocation());
                    List<Map<String, String>> chatHistory = getChatHistoryFromDb(conversation.getId());

                    String userMessage = normalizeUserMessage(request.getMessage());
                    saveUserMessage(conversation.getId(), userId, userMessage, weatherInfo, request.getOccasion());

                    ChatContext chatContext = new ChatContext();
                    chatContext.setConversation(conversation);
                    chatContext.setUserId(userId);
                    chatContext.setChatHistory(chatHistory);
                    chatContext.setIsNewConversation(isNewConversation);
                    chatContext.setUserMessage(userMessage);

                    if (isSimpleGreetingMessage(userMessage)) {
                        chatContext.setContext(buildContext(weatherInfo, request.getOccasion(), Collections.emptyList(), false));
                        chatContext.setRecommendedClothes(Collections.emptyList());
                        chatContext.setDirectResponse(buildGreetingResponse());
                    } else if (isIdentityQuestionMessage(userMessage)) {
                        chatContext.setContext(buildContext(weatherInfo, request.getOccasion(), Collections.emptyList(), false));
                        chatContext.setRecommendedClothes(Collections.emptyList());
                        chatContext.setDirectResponse(buildIdentityResponse());
                    } else {
                        boolean needClothingSearch = isNeedClothingSearchNormalized(userMessage);
                        List<Map<String, Object>> recommendedClothes = new ArrayList<>();
                        if (needClothingSearch) {
                            String query = generateClothingQuery(userMessage, weatherInfo, request.getOccasion());
                            recommendedClothes = searchClothes(userId, query);
                        }
                        chatContext.setContext(buildContext(weatherInfo, request.getOccasion(), recommendedClothes, needClothingSearch));
                        chatContext.setRecommendedClothes(recommendedClothes);
                    }

                    contextRef.set(chatContext);
                    return String.format("[SESSION]%s|%s|%s",
                            conversation.getSessionId(),
                            conversation.getTitle() != null ? conversation.getTitle() : "",
                            isNewConversation);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(sessionInfo -> Flux.just(sessionInfo).concatWith(processStreamResponse(userId, request, contextRef)));
    }

    private Flux<String> processStreamResponse(Long userId, ChatRequestDTO request, AtomicReference<ChatContext> contextRef) {
        ChatContext chatContext = contextRef.get();
        if (chatContext == null) {
            return Flux.error(new RuntimeException("chat context missing"));
        }

        if (chatContext.getDirectResponse() != null) {
            return Flux.just(chatContext.getDirectResponse())
                    .concatWith(Flux.just("[DONE]"))
                    .doOnComplete(() -> saveAiMessageAsync(chatContext, chatContext.getDirectResponse()));
        }

        return Mono.fromCallable(() -> wardrobeChatAgentService.generateChatReply(
                        userId,
                        request.getMessage(),
                        request.getLocation(),
                        request.getOccasion(),
                        (String) chatContext.getContext().get("weather"),
                        chatContext.getChatHistory(),
                        chatContext.getContext(),
                        chatContext.getRecommendedClothes()
                ))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(result -> {
                    if (result.getRecommendedClothes() != null && !result.getRecommendedClothes().isEmpty()) {
                        chatContext.setRecommendedClothes(result.getRecommendedClothes());
                    }

                    String reply = result.getReply() == null ? "" : result.getReply();
                    if (reply.isBlank()) {
                        log.warn("LangChain4j stream agent returned empty reply, fallback to legacy stream");
                        return generateLegacyStreamResponse(request, chatContext);
                    }
                    return Flux.fromIterable(chunkReply(reply))
                            .concatWith(Flux.just("[DONE]"))
                            .doOnComplete(() -> saveAiMessageAsync(chatContext, reply));
                })
                .onErrorResume(error -> {
                    log.warn("LangChain4j stream agent failed, fallback to legacy stream: {}", error.getMessage());
                    return generateLegacyStreamResponse(request, chatContext);
                });
    }

    private Flux<String> generateLegacyStreamResponse(ChatRequestDTO request, ChatContext chatContext) {
        StringBuilder fullResponse = new StringBuilder();
        return streamService.generateStreamResponse(
                        request.getMessage(),
                        chatContext.getChatHistory(),
                        chatContext.getContext(),
                        chatContext.getUserId()
                )
                .doOnNext(chunk -> {
                    if (!chunk.startsWith("[") && !chunk.startsWith("{")) {
                        fullResponse.append(chunk);
                    }
                })
                .doOnComplete(() -> saveAiMessageAsync(chatContext, fullResponse.toString()))
                .doOnError(streamError -> log.error("Legacy stream failed: {}", streamError.getMessage(), streamError));
    }

    private List<String> chunkReply(String reply) {
        List<String> chunks = new ArrayList<>();
        if (reply == null || reply.isEmpty()) {
            return chunks;
        }

        int chunkSize = 24;
        for (int i = 0; i < reply.length(); i += chunkSize) {
            chunks.add(reply.substring(i, Math.min(reply.length(), i + chunkSize)));
        }
        return chunks;
    }

    private void saveAiMessageAsync(ChatContext chatContext, String fullResponse) {
        try {
            AiConversation conversation = chatContext.getConversation();
            AiChatMessage message = new AiChatMessage();
            message.setConversationId(conversation.getId());
            message.setUserId(chatContext.getUserId());
            message.setRole("assistant");
            message.setContent(fullResponse);
            message.setMessageType(chatContext.getRecommendedClothes() != null && !chatContext.getRecommendedClothes().isEmpty()
                    ? "recommendation"
                    : "text");
            message.setCreatedAt(LocalDateTime.now());

            if (chatContext.getRecommendedClothes() != null && !chatContext.getRecommendedClothes().isEmpty()) {
                message.setRecommendations(JSON.toJSONString(chatContext.getRecommendedClothes()));
            }

            if (chatContext.getContext() != null && !chatContext.getContext().isEmpty()) {
                message.setContextSnapshot(JSON.toJSONString(chatContext.getContext()));
            }

            chatMessageMapper.insert(message);

            Integer count = chatMessageMapper.countByConversationId(conversation.getId());
            conversationMapper.updateMessageCount(conversation.getId(), count);
            conversation.setMessageCount(count);

            if (chatContext.isNewConversation() && conversation.getTitle() == null) {
                String title = generateConversationTitle(chatContext.getUserMessage(), fullResponse, chatContext.getUserId());
                conversation.setTitle(title);
            }
            conversation.setUpdatedAt(LocalDateTime.now());
            conversationMapper.updateById(conversation);
        } catch (Exception e) {
            log.error("Failed to save stream chat response: {}", e.getMessage(), e);
        }
    }

    private boolean isNeedClothingSearchNormalized(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return false;
        }

        String lowerMessage = userMessage.toLowerCase(Locale.ROOT);
        Set<String> clothingKeywords = new HashSet<>(Arrays.asList(
                "穿什么", "穿啥", "穿哪件", "穿哪一件", "怎么穿", "怎么搭", "如何搭配", "怎么搭配",
                "穿搭", "搭配", "搭什么", "配什么", "推荐衣服", "推荐穿搭", "推荐单品", "出门穿什么",
                "今天穿什么", "明天穿什么", "上班穿什么", "约会穿什么", "通勤穿什么", "衣服", "外套",
                "上衣", "裤子", "裙子", "鞋子", "look", "outfit", "wear", "dress", "jacket",
                "shirt", "pants", "coat", "skirt", "hoodie", "sweater"
        ));
        for (String keyword : clothingKeywords) {
            if (lowerMessage.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean isNeedClothingSearch(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return false;
        }

        String lowerMessage = userMessage.toLowerCase(Locale.ROOT);
        Set<String> clothingKeywords = new HashSet<>(Arrays.asList(
                "穿搭", "衣服", "搭配", "外套", "裤子", "裙子", "衬衫", "毛衣", "t恤",
                "look", "outfit", "wear", "dress", "jacket", "shirt", "pants"
        ));
        for (String keyword : clothingKeywords) {
            if (lowerMessage.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

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

    private String generateSessionId(Long userId) {
        return "chat:" + userId + ":" + System.currentTimeMillis() + ":" + UUID.randomUUID().toString().replace("-", "");
    }

    private String getWeatherInfo(String location) {
        if (location == null || location.trim().isEmpty()) {
            return null;
        }
        try {
            McpWeatherService.WeatherInfo weatherInfo = weatherService.getWeatherByLocation(location);
            if (weatherInfo != null) {
                return weatherInfo.getLocation() + " " + weatherInfo.getTemperature() + "C " + weatherInfo.getWeatherCondition();
            }
        } catch (Exception e) {
            log.warn("Failed to get weather info: {}", e.getMessage());
        }
        return null;
    }

    private List<Map<String, String>> getChatHistoryFromDb(Long conversationId) {
        List<AiChatMessage> messages = chatMessageMapper.selectRecentMessages(conversationId, 10);
        Collections.reverse(messages);

        List<Map<String, String>> history = new ArrayList<>();
        for (AiChatMessage message : messages) {
            Map<String, String> map = new HashMap<>();
            map.put("role", message.getRole());
            map.put("content", message.getContent());
            history.add(map);
        }
        return history;
    }

    private void saveUserMessage(Long conversationId, Long userId, String content, String weatherInfo, String occasion) {
        AiChatMessage message = new AiChatMessage();
        message.setConversationId(conversationId);
        message.setUserId(userId);
        message.setRole("user");
        message.setContent(content);
        message.setMessageType("text");
        message.setCreatedAt(LocalDateTime.now());

        Map<String, Object> snapshot = new HashMap<>();
        if (weatherInfo != null) {
            snapshot.put("weather", weatherInfo);
        }
        if (occasion != null) {
            snapshot.put("occasion", occasion);
        }
        if (!snapshot.isEmpty()) {
            message.setContextSnapshot(JSON.toJSONString(snapshot));
        }

        chatMessageMapper.insert(message);
    }

    private String generateClothingQuery(String userMessage, String weatherInfo, String occasion) {
        StringBuilder builder = new StringBuilder();
        if (userMessage != null && !userMessage.isBlank()) {
            builder.append(userMessage).append(' ');
        }
        if (weatherInfo != null && !weatherInfo.isBlank()) {
            builder.append(weatherInfo).append(' ');
        }
        if (occasion != null && !occasion.isBlank()) {
            builder.append(occasion).append(' ');
        }
        return builder.toString().trim();
    }

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
            log.warn("Failed to search clothes: {}", e.getMessage());
        }
        return results;
    }

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
                "你好", "您好", "哈喽", "嗨", "在吗", "有人吗", "hello", "hi", "hey", "goodmorning", "goodafternoon", "goodevening"
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
                "你是谁", "你叫什么", "你是做什么的", "你是什么", "介绍一下你自己", "whoareyou", "whatareyou", "introduceyourself"
        ));
        return identityQuestions.contains(normalized);
    }

    private String buildGreetingResponse() {
        return promptSettingsService.getGreetingReply();
    }

    private String buildIdentityResponse() {
        return promptSettingsService.getIdentityReply();
    }

    private Map<String, Object> buildContext(String weatherInfo,
                                             String occasion,
                                             List<Map<String, Object>> recommendedClothes,
                                             boolean needClothingSearch) {
        Map<String, Object> context = new HashMap<>();
        if (weatherInfo != null) {
            context.put("weather", weatherInfo);
        }
        if (occasion != null) {
            context.put("occasion", occasion);
        }
        if (recommendedClothes != null && !recommendedClothes.isEmpty()) {
            context.put("clothes", recommendedClothes);
        }
        context.put("needClothingSearch", needClothingSearch);
        return context;
    }

    private String generateConversationTitle(String userMessage, String aiReply, Long userId) {
        try {
            String systemPrompt = promptSettingsService.getConversationTitlePrompt();
            String prompt = "用户：" + truncateStr(userMessage, 80) + "\nAI：" + truncateStr(aiReply, 80);
            String title = textGeneratorService.generateCustomResponse(systemPrompt, prompt, null, userId);
            return normalizeConversationTitle(title, userMessage);
        } catch (Exception e) {
            log.warn("Failed to generate conversation title: {}", e.getMessage());
            return truncateStr(userMessage, 15);
        }
    }

    private String normalizeConversationTitle(String title, String userMessage) {
        String normalized = title == null ? "" : title.trim()
                .replaceAll("[\"'\\u201c\\u201d\\u2018\\u2019]", "")
                .replaceAll("\\s+", " ");

        if (!normalized.isEmpty()) {
            normalized = normalized.substring(0, Math.min(normalized.length(), 15));
        }
        if (normalized.isEmpty() || GENERIC_TITLES.contains(normalized)) {
            return truncateStr(userMessage, 15);
        }
        return normalized;
    }

    private String truncateStr(String value, int max) {
        if (value == null || value.isEmpty()) {
            return "新对话";
        }
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }

    private static class ChatContext {
        private AiConversation conversation;
        private Long userId;
        private List<Map<String, String>> chatHistory;
        private Map<String, Object> context;
        private List<Map<String, Object>> recommendedClothes;
        private boolean isNewConversation;
        private String directResponse;
        private String userMessage;

        public AiConversation getConversation() {
            return conversation;
        }

        public void setConversation(AiConversation conversation) {
            this.conversation = conversation;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public List<Map<String, String>> getChatHistory() {
            return chatHistory;
        }

        public void setChatHistory(List<Map<String, String>> chatHistory) {
            this.chatHistory = chatHistory;
        }

        public Map<String, Object> getContext() {
            return context;
        }

        public void setContext(Map<String, Object> context) {
            this.context = context;
        }

        public List<Map<String, Object>> getRecommendedClothes() {
            return recommendedClothes;
        }

        public void setRecommendedClothes(List<Map<String, Object>> recommendedClothes) {
            this.recommendedClothes = recommendedClothes;
        }

        public boolean isNewConversation() {
            return isNewConversation;
        }

        public void setIsNewConversation(boolean newConversation) {
            isNewConversation = newConversation;
        }

        public String getDirectResponse() {
            return directResponse;
        }

        public void setDirectResponse(String directResponse) {
            this.directResponse = directResponse;
        }

        public String getUserMessage() {
            return userMessage;
        }

        public void setUserMessage(String userMessage) {
            this.userMessage = userMessage;
        }
    }
}
