package com.ecru.common.service.ai;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ecru.common.dto.ai.AiApiCallContext;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.TokenUsage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service("aiTextGeneratorService")
public class AiTextGeneratorService {

    private static final double CHAT_TEMPERATURE = 0.7d;
    private static final int CHAT_MAX_TOKENS = 2048;
    private static final double CUSTOM_TEMPERATURE = 0.4d;
    private static final int CUSTOM_MAX_TOKENS = 1024;
    private static final double INTENT_TEMPERATURE = 0.3d;
    private static final int INTENT_MAX_TOKENS = 512;

    @Value("${ai.siliconflow.model:Qwen/Qwen3-VL-8B-Instruct}")
    private String modelName;

    @Autowired
    private AiApiMonitorService monitorService;

    @Autowired
    private AiPromptSettingsService promptSettingsService;

    @Autowired
    private OpenAiChatModel chatModel;

    public String generateResponse(String prompt, List<Map<String, String>> chatHistory,
                                   Map<String, Object> context, Long userId) {
        AiApiCallContext monitorContext = AiApiCallContext.create(
                AiApiMonitorWrapper.Scene.CHAT_GENERATE, modelName, userId);

        try {
            ChatRequest request = ChatRequest.builder()
                    .messages(buildChatMessages(prompt, chatHistory, context))
                    .temperature(CHAT_TEMPERATURE)
                    .maxOutputTokens(CHAT_MAX_TOKENS)
                    .build();
            monitorContext.setPromptLength(request.toString().length());

            ChatResponse response = chatModel.chat(request);
            applyTokenUsage(monitorContext, response.tokenUsage());
            monitorContext.markSuccess(200);

            String content = parseChatResponse(response);
            monitorContext.setResponseLength(content.length());
            return content;
        } catch (Exception e) {
            log.error("Failed to generate AI response: {}", e.getMessage(), e);
            if (monitorContext.getStatus() == 1) {
                monitorContext.markFailed(AiApiMonitorWrapper.ErrorType.BUSINESS_ERROR,
                        e.getMessage(), null);
            }
            return "当前智能服务暂时不可用，请稍后再试。";
        } finally {
            monitorService.recordApiCall(monitorContext);
        }
    }

    @Deprecated
    public String generateResponse(String prompt, List<Map<String, String>> chatHistory,
                                   Map<String, Object> context) {
        return generateResponse(prompt, chatHistory, context, null);
    }

    public String generateCustomResponse(String systemPrompt, String prompt,
                                         Map<String, Object> context, Long userId) {
        AiApiCallContext monitorContext = AiApiCallContext.create(
                AiApiMonitorWrapper.Scene.CHAT_GENERATE, modelName, userId);

        try {
            ChatRequest request = ChatRequest.builder()
                    .messages(buildCustomMessages(systemPrompt, prompt, context))
                    .temperature(CUSTOM_TEMPERATURE)
                    .maxOutputTokens(CUSTOM_MAX_TOKENS)
                    .build();
            monitorContext.setPromptLength(request.toString().length());

            ChatResponse response = chatModel.chat(request);
            applyTokenUsage(monitorContext, response.tokenUsage());
            monitorContext.markSuccess(200);

            String content = parseChatResponse(response);
            monitorContext.setResponseLength(content.length());
            return content;
        } catch (Exception e) {
            log.error("Failed to generate custom AI response: {}", e.getMessage(), e);
            if (monitorContext.getStatus() == 1) {
                monitorContext.markFailed(AiApiMonitorWrapper.ErrorType.BUSINESS_ERROR,
                        e.getMessage(), null);
            }
            return null;
        } finally {
            monitorService.recordApiCall(monitorContext);
        }
    }

    public Map<String, Object> analyzeQueryIntent(String query, Long userId) {
        String normalizedQuery = normalizeQuery(query);
        if (isSimpleGreetingQuery(normalizedQuery) || isIdentityQuestionQuery(normalizedQuery)) {
            return buildFallbackIntentResult(normalizedQuery);
        }

        AiApiCallContext monitorContext = AiApiCallContext.create(
                AiApiMonitorWrapper.Scene.INTENT_ANALYZE, modelName, userId);

        try {
            ChatRequest request = ChatRequest.builder()
                    .messages(buildIntentMessages(normalizedQuery))
                    .temperature(INTENT_TEMPERATURE)
                    .maxOutputTokens(INTENT_MAX_TOKENS)
                    .build();
            monitorContext.setPromptLength(request.toString().length());

            ChatResponse response = chatModel.chat(request);
            applyTokenUsage(monitorContext, response.tokenUsage());
            monitorContext.markSuccess(200);

            String content = parseChatResponse(response);
            Map<String, Object> result = parseIntentResponse(content);
            monitorContext.setResponseLength(result.toString().length());
            return result;
        } catch (Exception e) {
            log.error("Failed to analyze query intent: {}", e.getMessage(), e);
            if (monitorContext.getStatus() == 1) {
                monitorContext.markFailed(AiApiMonitorWrapper.ErrorType.BUSINESS_ERROR,
                        e.getMessage(), null);
            }
            return buildFallbackIntentResult(normalizedQuery);
        } finally {
            monitorService.recordApiCall(monitorContext);
        }
    }

    @Deprecated
    public Map<String, Object> analyzeQueryIntent(String query) {
        return analyzeQueryIntent(query, null);
    }

    private List<ChatMessage> buildChatMessages(String prompt,
                                                List<Map<String, String>> chatHistory,
                                                Map<String, Object> context) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(promptSettingsService.getChatSystemPrompt()));

        if (chatHistory != null && !chatHistory.isEmpty()) {
            int startIndex = Math.max(0, chatHistory.size() - 5);
            for (int i = startIndex; i < chatHistory.size(); i++) {
                Map<String, String> history = chatHistory.get(i);
                if (history == null) {
                    continue;
                }
                messages.add(toChatMessage(history.get("role"), history.get("content")));
            }
        }

        messages.add(UserMessage.from(buildUserPrompt(prompt, context)));
        return messages;
    }

    private List<ChatMessage> buildCustomMessages(String systemPrompt,
                                                  String prompt,
                                                  Map<String, Object> context) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(systemPrompt == null ? "" : systemPrompt));

        StringBuilder userContent = new StringBuilder();
        userContent.append(prompt == null ? "" : prompt);
        if (context != null && !context.isEmpty()) {
            userContent.append("\n\n补充上下文:\n").append(JSON.toJSONString(context));
        }
        messages.add(UserMessage.from(limitLength(userContent.toString(), 3000)));
        return messages;
    }

    private List<ChatMessage> buildIntentMessages(String query) {
        String systemPrompt = """
                你是穿搭推荐系统中的意图识别器。请分析用户输入，并严格输出 JSON，不要输出解释。
                返回字段包括:
                1. intent: 查询意图，例如 搭配推荐 / 问候 / 身份介绍 / 穿搭知识
                2. occasion: 场景，没有则为 null
                3. season: 季节，没有则为 null
                4. style: 风格偏好，没有则为 null
                5. weather: 天气需求，没有则为 null
                6. keywords: 关键词数组
                7. clothingType: 关注的衣物类型，没有则为 null
                8. negativePreferences: 不喜欢的颜色或风格数组
                9. isNegative: 是否包含明显否定偏好，布尔值
                示例:
                {"intent":"搭配推荐","occasion":"通勤","season":"夏季","style":"简约","weather":null,"keywords":["通勤","夏天"],"clothingType":"上衣","negativePreferences":["荧光色"],"isNegative":true}
                """;

        return List.of(
                SystemMessage.from(systemPrompt),
                UserMessage.from(query)
        );
    }

    private ChatMessage toChatMessage(String role, String content) {
        String safeContent = content == null ? "" : content;
        if ("system".equalsIgnoreCase(role)) {
            return SystemMessage.from(safeContent);
        }
        if ("assistant".equalsIgnoreCase(role)) {
            return AiMessage.from(safeContent);
        }
        return UserMessage.from(safeContent);
    }

    private void applyTokenUsage(AiApiCallContext monitorContext, TokenUsage tokenUsage) {
        if (tokenUsage == null) {
            return;
        }
        monitorContext.setInputTokens(tokenUsage.inputTokenCount());
        monitorContext.setOutputTokens(tokenUsage.outputTokenCount());
        monitorContext.setTotalTokens(tokenUsage.totalTokenCount());
    }

    private String parseChatResponse(ChatResponse response) throws IOException {
        if (response != null && response.aiMessage() != null && response.aiMessage().text() != null) {
            return response.aiMessage().text();
        }
        throw new IOException("Empty chat response");
    }

    private Map<String, Object> parseIntentResponse(String content) throws IOException {
        if (content != null) {
            int start = content.indexOf('{');
            int end = content.lastIndexOf('}') + 1;
            if (start != -1 && end > start) {
                String jsonStr = content.substring(start, end);
                return JSON.parseObject(jsonStr, Map.class);
            }
        }
        throw new IOException("Invalid intent response: " + content);
    }

    private Map<String, Object> buildFallbackIntentResult(String query) {
        String normalizedQuery = normalizeQuery(query);
        Map<String, Object> defaultResult = new HashMap<>();
        defaultResult.put("intent", resolveFallbackIntent(normalizedQuery));
        defaultResult.put("keywords", splitKeywords(normalizedQuery));
        defaultResult.put("isNegative", containsNegativeSignal(normalizedQuery));
        defaultResult.put("occasion", null);
        defaultResult.put("season", null);
        defaultResult.put("style", null);
        defaultResult.put("weather", null);
        defaultResult.put("clothingType", null);
        defaultResult.put("negativePreferences", new ArrayList<String>());
        return defaultResult;
    }

    private String buildUserPrompt(String prompt, Map<String, Object> context) {
        StringBuilder userContent = new StringBuilder();
        userContent.append(prompt == null ? "" : prompt);

        if (context != null && !context.isEmpty()) {
            userContent.append("\n\n参考上下文:\n");

            Object weather = context.get("weather");
            if (weather != null) {
                userContent.append("天气: ").append(weather).append("\n");
            }

            Object occasion = context.get("occasion");
            if (occasion != null) {
                userContent.append("场景: ").append(occasion).append("\n");
            }

            Object negativePreferences = context.get("negativePreferences");
            if (negativePreferences != null) {
                userContent.append("用户不喜欢: ").append(negativePreferences).append("\n");
            }

            if (context.containsKey("clothes")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> clothes = (List<Map<String, Object>>) context.get("clothes");
                if (clothes != null && !clothes.isEmpty()) {
                    userContent.append("可用衣物:\n");
                    int limit = Math.min(5, clothes.size());
                    for (int i = 0; i < limit; i++) {
                        Map<String, Object> cloth = clothes.get(i);
                        userContent.append(i + 1)
                                .append(". ")
                                .append(cloth.get("name"))
                                .append(" (")
                                .append(cloth.get("category"))
                                .append(")")
                                .append(" - 颜色: ")
                                .append(cloth.get("color"))
                                .append("\n");
                    }
                }
            }
        }

        return limitLength(userContent.toString(), 1000);
    }

    private String limitLength(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text == null ? "" : text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private String normalizeQuery(String query) {
        return query == null ? "" : query.trim();
    }

    private boolean isSimpleGreetingQuery(String query) {
        String normalized = normalizeQuery(query).toLowerCase();
        if (normalized.isEmpty()) {
            return false;
        }

        normalized = normalized.replaceAll("[\\p{Punct}\\s\\u3000-\\u303F\\uFF00-\\uFF65]+", "");

        Set<String> greetingMessages = new HashSet<>(Arrays.asList(
                "你好",
                "您好",
                "哈喽",
                "嗨",
                "早上好",
                "上午好",
                "中午好",
                "下午好",
                "晚上好",
                "在吗",
                "在嘛",
                "有人吗",
                "hello",
                "hi",
                "hey",
                "goodmorning",
                "goodafternoon",
                "goodevening"
        ));

        return greetingMessages.contains(normalized);
    }

    private boolean isIdentityQuestionQuery(String query) {
        String normalized = normalizeQuery(query).toLowerCase();
        if (normalized.isEmpty()) {
            return false;
        }

        normalized = normalized.replaceAll("[\\p{Punct}\\s\\u3000-\\u303F\\uFF00-\\uFF65]+", "");

        Set<String> identityQuestions = new HashSet<>(Arrays.asList(
                "你是谁",
                "你叫什么",
                "你是做什么的",
                "你是什么",
                "介绍一下你自己",
                "whoareyou",
                "whatareyou",
                "introduceyourself"
        ));

        return identityQuestions.contains(normalized);
    }

    private String resolveFallbackIntent(String query) {
        if (isSimpleGreetingQuery(query)) {
            return "greeting";
        }
        if (isIdentityQuestionQuery(query)) {
            return "identity";
        }
        return "搭配推荐";
    }

    private List<String> splitKeywords(String query) {
        String normalized = normalizeQuery(query);
        if (normalized.isEmpty()) {
            return new ArrayList<>();
        }

        String[] parts = normalized.split("\\s+");
        if (parts.length <= 1) {
            return new ArrayList<>(Collections.singletonList(normalized));
        }

        return new ArrayList<>(Arrays.asList(parts));
    }

    private boolean containsNegativeSignal(String query) {
        String normalized = normalizeQuery(query);
        return normalized.contains("不喜欢")
                || normalized.contains("不要")
                || normalized.contains("讨厌");
    }
}
