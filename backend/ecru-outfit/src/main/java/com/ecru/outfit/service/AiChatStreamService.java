package com.ecru.outfit.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.ecru.common.service.ai.AiPromptSettingsService;
import com.ecru.common.service.ai.AiTextGeneratorService;
import com.ecru.common.service.ai.AiTextGeneratorStreamService;
import com.ecru.outfit.dto.request.ChatRequestDTO;
import com.ecru.outfit.entity.AiChatMessage;
import com.ecru.outfit.entity.AiConversation;
import com.ecru.outfit.entity.UserStyleArchive;
import com.ecru.outfit.mapper.AiChatMessageMapper;
import com.ecru.outfit.mapper.AiConversationMapper;
import com.ecru.outfit.mapper.UserStyleArchiveMapper;
import com.ecru.outfit.service.mcp.McpWeatherService;
import com.ecru.outfit.service.rag.RagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class AiChatStreamService {

    private static final int SIMULATED_STREAM_MIN_CHUNK_SIZE = 8;
    private static final int SIMULATED_STREAM_MAX_CHUNK_SIZE = 24;
    private static final Duration SIMULATED_STREAM_CHUNK_DELAY = Duration.ofMillis(45);

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
    private UserStyleArchiveMapper userStyleArchiveMapper;

    public Flux<String> chatStream(Long userId, ChatRequestDTO request) {
        AtomicReference<ChatContext> contextRef = new AtomicReference<>();

        return Mono.fromCallable(() -> {
                    // 流式对话与普通对话复用同一套上下文准备逻辑，
                    // 只是最终改为把回复按 chunk 连续推送给前端。
                    // 这里先在阻塞线程池里完成“准备阶段”，避免数据库查询和天气/RAG 操作阻塞 Reactor 事件线程。
                    AiConversation conversation = getOrCreateConversation(userId, request);
                    boolean isNewConversation = conversation.getMessageCount() == 0;
                    String weatherInfo = getWeatherInfo(request.getLocation());
                    List<Map<String, String>> chatHistory = getChatHistoryFromDb(conversation.getId());

                    String userMessage = normalizeUserMessage(request.getMessage());
                    // 用户消息会先落库，再开始流式生成。
                    // 这样即使后续生成中断，历史记录里也能保留用户这一次输入。
                    saveUserMessage(conversation.getId(), userId, userMessage, weatherInfo, request.getOccasion());

                    ChatContext chatContext = new ChatContext();
                    chatContext.setConversation(conversation);
                    chatContext.setUserId(userId);
                    chatContext.setChatHistory(chatHistory);
                    chatContext.setIsNewConversation(isNewConversation);
                    chatContext.setUserMessage(userMessage);
                    Map<String, Object> userStyleProfile = buildUserStyleProfile(userId);

                    if (isSimpleGreetingMessage(userMessage)) {
                        // 纯问候语直接走短路分支，不做意图识别、不走检索、不调大模型。
                        chatContext.setContext(buildContext(
                                weatherInfo,
                                request.getOccasion(),
                                Collections.emptyList(),
                                Collections.emptyMap(),
                                Collections.emptyList(),
                                false,
                                userStyleProfile
                        ));
                        chatContext.setRecommendedClothes(Collections.emptyList());
                        chatContext.setDirectResponse(buildGreetingResponse());
                    } else if (isIdentityQuestionMessage(userMessage)) {
                        // “你是谁”这类身份问题也走短路分支，减少延迟和模型成本。
                        chatContext.setContext(buildContext(
                                weatherInfo,
                                request.getOccasion(),
                                Collections.emptyList(),
                                Collections.emptyMap(),
                                Collections.emptyList(),
                                false,
                                userStyleProfile
                        ));
                        chatContext.setRecommendedClothes(Collections.emptyList());
                        chatContext.setDirectResponse(buildIdentityResponse());
                    } else {
                        // 普通问题进入完整链路：
                        // 意图识别 -> 是否需要衣柜检索 -> 负面偏好合并 -> 构造模型上下文。
                        Map<String, Object> intentAnalysis = textGeneratorService.analyzeQueryIntent(userMessage, userId);
                        boolean needClothingSearch = isNeedClothingSearch(intentAnalysis, userMessage);
                        List<Map<String, Object>> recommendedClothes = new ArrayList<>();
                        List<String> negativePreferences = mergeNegativePreferences(
                                extractNegativePreferences(intentAnalysis),
                                userStyleProfile
                        );
                        if (needClothingSearch) {
                            // 对于推荐类问题，先召回候选衣物，再让模型基于候选组织自然语言回答。
                            String query = generateClothingQuery(
                                    userMessage,
                                    weatherInfo,
                                    request.getOccasion(),
                                    intentAnalysis,
                                    userStyleProfile
                            );
                            recommendedClothes = searchClothes(userId, query, negativePreferences);
                        }
                        chatContext.setContext(buildContext(
                                weatherInfo,
                                request.getOccasion(),
                                recommendedClothes,
                                intentAnalysis,
                                negativePreferences,
                                needClothingSearch,
                                userStyleProfile
                        ));
                        chatContext.setRecommendedClothes(recommendedClothes);
                    }

                    contextRef.set(chatContext);
                    // 首个流片段不是正文，而是会话元信息。
                    // 前端可据此立即拿到 sessionId，用于后续继续追问或刷新会话标题。
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
            // 问候语、身份类问题直接走固定回复，避免不必要的模型调用。
            // 这类回复仍然会沿用统一的流式协议：正文 -> [DONE] -> 异步落库。
            return Flux.just(chatContext.getDirectResponse())
                    .concatWith(Flux.just("[DONE]"))
                    .doOnComplete(() -> saveAiMessageAsync(chatContext, chatContext.getDirectResponse()));
        }

        // 优先使用实时流式模型；失败时自动回退到旧流式实现，保证接口稳定。
        // 这里的回退是“同一次请求内无感切换”，前端不需要重试。
        return generateRealtimeStreamResponse(chatContext)
                .onErrorResume(error -> {
                    log.warn("Realtime stream generation failed, fallback to legacy stream: {}", error.getMessage());
                    return generateLegacyStreamResponse(chatContext);
                });
    }

    private Flux<String> generateRealtimeStreamResponse(ChatContext chatContext) {
        StringBuilder fullResponse = new StringBuilder();
        return streamService.generateStreamResponseWithCustomPrompt(
                        // 这里传入的是流式专用 system prompt。
                        // 相比普通对话，它更强调输出格式约束和“只能引用上下文衣物”。
                        buildRealtimeStreamSystemPrompt(chatContext),
                        chatContext.getUserMessage(),
                        chatContext.getChatHistory(),
                        chatContext.getContext(),
                        chatContext.getUserId()
                )
                .doOnNext(chunk -> {
                    // 边推边累计完整文本，结束后再统一落库，避免数据库里只存碎片响应。
                    if (!"[DONE]".equals(chunk) && !chunk.startsWith("[ERROR]")) {
                        fullResponse.append(chunk);
                    }
                })
                // 注意：streamService 会在正常完成时自己输出 [DONE]。
                // 这里不额外拼接，避免前端收到两个结束标记。
                .doOnComplete(() -> saveAiMessageAsync(chatContext, fullResponse.toString()))
                .doOnError(streamError -> log.error("Realtime stream failed: {}", streamError.getMessage(), streamError));
    }

    private Flux<String> generateLegacyStreamResponse(ChatContext chatContext) {
        StringBuilder fullResponse = new StringBuilder();
        return streamService.generateStreamResponse(
                        chatContext.getUserMessage(),
                        chatContext.getChatHistory(),
                        chatContext.getContext(),
                        chatContext.getUserId()
                )
                .doOnNext(chunk -> {
                    // 旧实现与实时实现保持相同协议，方便前端无感兼容。
                    if (!"[DONE]".equals(chunk) && !chunk.startsWith("[ERROR]")) {
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

        StringBuilder current = new StringBuilder();
        for (int i = 0; i < reply.length(); i++) {
            char ch = reply.charAt(i);
            current.append(ch);

            if (ch == '\n') {
                flushChunk(chunks, current);
                continue;
            }

            if (current.length() >= SIMULATED_STREAM_MIN_CHUNK_SIZE && isNaturalChunkBoundary(ch)) {
                flushChunk(chunks, current);
                continue;
            }

            if (current.length() >= SIMULATED_STREAM_MAX_CHUNK_SIZE) {
                flushChunk(chunks, current);
            }
        }

        flushChunk(chunks, current);
        return chunks;
    }

    private boolean isNaturalChunkBoundary(char ch) {
        return Character.isWhitespace(ch)
                || ch == '，'
                || ch == '。'
                || ch == '、'
                || ch == '；'
                || ch == '：'
                || ch == '！'
                || ch == '？'
                || ch == ','
                || ch == '.'
                || ch == ';'
                || ch == ':'
                || ch == '!'
                || ch == '?';
    }

    private void flushChunk(List<String> chunks, StringBuilder current) {
        if (current.length() == 0) {
            return;
        }
        chunks.add(current.toString());
        current.setLength(0);
    }

    private String buildRealtimeStreamSystemPrompt(ChatContext chatContext) {
        // 在系统默认 prompt 之上追加输出约束，
        // 重点限制模型只能引用上下文里的候选衣物，避免凭空编造单品。
        // 这类约束放在 system prompt，而不是 user prompt，优先级更高、更稳定。
        StringBuilder prompt = new StringBuilder(promptSettingsService.getChatSystemPrompt());
        prompt.append("\nReturn only the user-facing reply text.");
        prompt.append("\nDo not output JSON, code fences, or any control markers.");
        prompt.append("\nIf you mention clothing recommendations, only use items from the provided context.");
        prompt.append("\nIf the provided context has no clothes, answer naturally without inventing wardrobe items.");
        prompt.append("\nKeep the tone practical, natural, and sufficiently detailed for styling advice.");
        prompt.append("\nFor recommendation or styling questions, give a complete answer, usually around 2-4 short paragraphs or an equally substantial structured answer.");
        prompt.append("\nExplain why the suggested items fit the weather, occasion, and user preferences instead of giving only a brief conclusion.");
        prompt.append("\nRespect negative preferences and avoid recommending colors or styles marked as avoided in the context.");

        if (chatContext.getRecommendedClothes() != null && !chatContext.getRecommendedClothes().isEmpty()) {
            prompt.append("\nThe context includes grounded wardrobe items. Reference the most relevant items and explain their fit clearly.");
        }
        return prompt.toString();
    }

    private void saveAiMessageAsync(ChatContext chatContext, String fullResponse) {
        try {
            // 流式响应结束后统一保存完整消息，并同步更新会话标题和消息计数。
            // 这里保存的是“最终拼接后的整段 assistant 回复”，不是逐 token 入库。
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
                // 如果本轮命中了 RAG 候选衣物，会把候选一起快照到消息表，
                // 便于历史会话回看时还原当时的推荐依据。
                message.setRecommendations(JSON.toJSONString(chatContext.getRecommendedClothes()));
            }

            if (chatContext.getContext() != null && !chatContext.getContext().isEmpty()) {
                // contextSnapshot 记录生成当时的天气、场景、偏好、候选等上下文，
                // 便于后续排查模型输出为什么会这样回答。
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
            // 流式和非流式共用 sessionId 机制，多轮追问会落到同一个 conversation。
            AiConversation conversation = conversationMapper.selectBySessionId(sessionId);
            if (conversation != null && conversation.getUserId().equals(userId)) {
                return conversation;
            }
        }
        return createNewConversation(userId, request);
    }

    private AiConversation createNewConversation(Long userId, ChatRequestDTO request) {
        // 新会话在首轮请求时就创建，后续流式返回的 [SESSION] 会把这个 sessionId 带给前端。
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
        // 这里只取最近若干轮历史，而不是整段长会话全部塞给模型，
        // 目的是控制 token 消耗，并降低长上下文噪音。
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
        // 用户消息和 assistant 消息分开落库：
        // 用户消息在生成前保存，assistant 消息在流结束后保存。
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

    private String generateClothingQuery(String userMessage,
                                        String weatherInfo,
                                        String occasion,
                                        Map<String, Object> intentAnalysis,
                                        Map<String, Object> userStyleProfile) {
        // 推荐衣物的检索词不是简单使用用户原话，而是把多种信号拼成一个“语义检索查询”：
        // 1. 意图识别得到的风格/季节/单品类型/关键词
        // 2. 用户原始问题
        // 3. 天气与场景
        // 4. 用户长期偏好的风格和颜色
        // 这样向量检索更容易召回“适合这次问题”的衣物，而不是只召回词面相近的衣物。
        StringBuilder builder = new StringBuilder();
        if (intentAnalysis != null) {
            appendIntentText(builder, intentAnalysis.get("style"));
            appendIntentText(builder, intentAnalysis.get("season"));
            appendIntentText(builder, intentAnalysis.get("clothingType"));
            Object keywords = intentAnalysis.get("keywords");
            if (keywords instanceof List<?> values) {
                for (Object value : values) {
                    appendIntentText(builder, value);
                }
            }
        }
        if (userMessage != null && !userMessage.isBlank()) {
            builder.append(userMessage).append(' ');
        }
        if (weatherInfo != null && !weatherInfo.isBlank()) {
            builder.append(weatherInfo).append(' ');
        }
        if (occasion != null && !occasion.isBlank()) {
            builder.append(occasion).append(' ');
        }
        if (userStyleProfile != null) {
            appendTopPreferences(builder, userStyleProfile.get("preferredStyles"), 3);
            appendTopPreferences(builder, userStyleProfile.get("preferredColors"), 2);
        }
        return builder.toString().trim();
    }

    private List<Map<String, Object>> searchClothes(Long userId, String query, List<String> negativePreferences) {
        // 这里只保留前端和模型真正需要的字段，避免把检索结果原样塞进上下文导致 prompt 过大。
        List<Map<String, Object>> results = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return results;
        }

        try {
            // RAG 召回通常取前 8 个候选，但真正写进 prompt 时会在下游再做更紧的裁剪。
            // 这里的返回值还只是“候选衣物池”，不是最终推荐结果。
            // 最终前端看到哪些衣物，还要经过 Agent 回复阶段的二次筛选。
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
            log.warn("Failed to search clothes: {}", e.getMessage());
        }
        return results;
    }

    private boolean isNeedClothingSearch(Map<String, Object> intentAnalysis, String userMessage) {
        // 先相信意图识别结果；只有意图不明显时，才回退到关键词启发式判断。
        // 只有被判断为“确实和穿搭/衣物推荐有关”的问题，才会触发衣柜检索。
        // 这样可以避免闲聊、问候、知识问答也误触发推荐流程。
        if (intentAnalysis != null) {
            String intent = readIntentText(intentAnalysis.get("intent"));
            if (intent != null) {
                String normalizedIntent = intent.toLowerCase(Locale.ROOT);
                if (normalizedIntent.contains("greeting")
                        || normalizedIntent.contains("identity")
                        || normalizedIntent.contains("chat")
                        || normalizedIntent.contains("thanks")
                        || normalizedIntent.contains("goodbye")
                        || normalizedIntent.contains("farewell")) {
                    return false;
                }
            }

            if (StringUtils.hasText(readIntentText(intentAnalysis.get("clothingType")))
                    || StringUtils.hasText(readIntentText(intentAnalysis.get("style")))) {
                return true;
            }
        }
        return isNeedClothingSearchNormalized(userMessage);
    }

    private List<String> extractNegativePreferences(Map<String, Object> intentAnalysis) {
        if (intentAnalysis == null) {
            return Collections.emptyList();
        }

        Object rawValue = intentAnalysis.get("negativePreferences");
        if (!(rawValue instanceof List<?> values)) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            String text = String.valueOf(value).trim();
            if (!text.isEmpty()) {
                result.add(text);
            }
        }
        return result;
    }

    private List<String> mergeNegativePreferences(List<String> negativePreferences, Map<String, Object> userStyleProfile) {
        Set<String> merged = new LinkedHashSet<>();
        if (negativePreferences != null) {
            merged.addAll(negativePreferences);
        }
        if (userStyleProfile != null && !userStyleProfile.isEmpty()) {
            merged.addAll(asStringList(userStyleProfile.get("avoidedColors")));
        }
        return new ArrayList<>(merged);
    }

    private void appendIntentText(StringBuilder builder, Object value) {
        String text = readIntentText(value);
        if (text != null) {
            builder.append(text).append(' ');
        }
    }

    private String readIntentText(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof List<?> values) {
            StringBuilder builder = new StringBuilder();
            for (Object item : values) {
                if (item == null) {
                    continue;
                }
                String text = String.valueOf(item).trim();
                if (text.isEmpty()) {
                    continue;
                }
                if (builder.length() > 0) {
                    builder.append(' ');
                }
                builder.append(text);
            }
            return builder.length() == 0 ? null : builder.toString();
        }

        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
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
                                             Map<String, Object> intentAnalysis,
                                             List<String> negativePreferences,
                                             boolean needClothingSearch,
                                             Map<String, Object> userStyleProfile) {
        // 与非流式对话保持一致的上下文字段，方便两条链路使用相同的业务约束。
        // 这个 context 既会参与 prompt 构造，也会作为快照存进消息表。
        // 其中 clothes 字段就是“候选衣物池”，后续模型回答必须尽量围绕这批衣物展开。
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
        if (intentAnalysis != null && !intentAnalysis.isEmpty()) {
            context.put("intent", intentAnalysis);
        }
        if (negativePreferences != null && !negativePreferences.isEmpty()) {
            context.put("negativePreferences", negativePreferences);
        }
        if (userStyleProfile != null && !userStyleProfile.isEmpty()) {
            context.put("userStyleProfile", userStyleProfile);
        }
        context.put("needClothingSearch", needClothingSearch);
        return context;
    }

    private Map<String, Object> buildUserStyleProfile(Long userId) {
        UserStyleArchive archive = userStyleArchiveMapper.selectByUserId(userId);
        if (archive == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> profile = new HashMap<>();
        putListField(profile, "preferredStyles", archive.getPreferredStyles());
        putListField(profile, "avoidedStyles", archive.getAvoidedStyles());
        putListField(profile, "preferredColors", archive.getPreferredColors());
        putListField(profile, "avoidedColors", archive.getAvoidedColors());
        putTextField(profile, "bodyType", archive.getBodyType());
        putTextField(profile, "skinTone", archive.getSkinTone());
        putTextField(profile, "occupation", archive.getOccupation());
        putListField(profile, "lifestyleTags", archive.getLifestyleTags());
        return profile;
    }

    private void putTextField(Map<String, Object> profile, String key, String value) {
        if (StringUtils.hasText(value)) {
            profile.put(key, value.trim());
        }
    }

    private void putListField(Map<String, Object> profile, String key, String rawValue) {
        List<String> values = parseListField(rawValue);
        if (!values.isEmpty()) {
            profile.put(key, values);
        }
    }

    private List<String> extractArchiveNegativePreferences(Map<String, Object> userStyleProfile) {
        if (userStyleProfile == null || userStyleProfile.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> merged = new LinkedHashSet<>();
        merged.addAll(asStringList(userStyleProfile.get("avoidedColors")));
        return new ArrayList<>(merged);
    }

    private void appendTopPreferences(StringBuilder builder, Object value, int limit) {
        if (limit <= 0) {
            return;
        }
        List<String> values = asStringList(value);
        for (int i = 0; i < Math.min(values.size(), limit); i++) {
            builder.append(values.get(i)).append(' ');
        }
    }

    private List<String> asStringList(Object value) {
        if (value instanceof List<?> rawList) {
            List<String> result = new ArrayList<>();
            for (Object item : rawList) {
                if (item == null) {
                    continue;
                }
                String text = String.valueOf(item).trim();
                if (!text.isEmpty()) {
                    result.add(text);
                }
            }
            return result;
        }
        return Collections.emptyList();
    }

    private List<String> parseListField(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyList();
        }
        try {
            if (raw.trim().startsWith("[")) {
                return JSON.parseObject(raw, new TypeReference<List<String>>() {
                }).stream().filter(StringUtils::hasText).map(String::trim).toList();
            }
        } catch (Exception ex) {
            log.debug("Parse style profile list failed, fallback to split: {}", raw, ex);
        }

        List<String> result = new ArrayList<>();
        for (String part : raw.split("[,，/]")) {
            if (StringUtils.hasText(part)) {
                result.add(part.trim());
            }
        }
        return result;
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
