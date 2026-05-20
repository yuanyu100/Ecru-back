package com.ecru.outfit.service.agent;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ecru.common.service.ai.AiPromptSettingsService;
import com.ecru.common.service.ai.AiTextGeneratorService;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WardrobeChatAgentService {

    @Autowired
    private OpenAiChatModel openAiChatModel;

    @Autowired
    private OutfitAdvisorTools outfitAdvisorTools;

    @Autowired
    private AiTextGeneratorService textGeneratorService;

    @Autowired
    private AiPromptSettingsService promptSettingsService;

    public ChatAgentResult generateChatReply(Long userId,
                                             String userMessage,
                                             String location,
                                             String occasion,
                                             String weatherInfo,
                                             List<Map<String, String>> chatHistory,
                                             Map<String, Object> context,
                                             List<Map<String, Object>> recommendedClothes) {
        try {
            String wardrobeQuery = buildWardrobeQueryForChat(userMessage, context);
            LangChain4jWardrobeChatAgent agent = AiServices.builder(LangChain4jWardrobeChatAgent.class)
                    .chatModel(openAiChatModel)
                    .tools(outfitAdvisorTools)
                    .build();

            String rawResponse = agent.chat(
                    defaultString(promptSettingsService.getChatSystemPrompt()),
                    userId,
                    defaultString(location),
                    defaultString(occasion),
                    defaultString(weatherInfo),
                    wardrobeQuery,
                    JSON.toJSONString(context),
                    JSON.toJSONString(chatHistory),
                    userMessage
            );

            ChatAgentResult result = parseChatAgentResult(rawResponse, recommendedClothes);
            ensureReply(result, userMessage, chatHistory, context, recommendedClothes);
            return result;
        } catch (Exception e) {
            log.warn("LangChain4j chat agent failed, fallback to direct generator: {}", e.getMessage());
            return buildFallbackResult(userMessage, chatHistory, context, recommendedClothes);
        }
    }

    private String buildWardrobeQueryForChat(String userMessage, Map<String, Object> context) {
        StringBuilder builder = new StringBuilder();
        if (userMessage != null && !userMessage.isBlank()) {
            builder.append(userMessage).append(' ');
        }
        if (context != null) {
            Object occasion = context.get("occasion");
            if (occasion instanceof String value && !value.isBlank()) {
                builder.append(value).append(' ');
            }
            Object weather = context.get("weather");
            if (weather instanceof String value && !value.isBlank()) {
                builder.append(value).append(' ');
            }
            Object intent = context.get("intent");
            if (intent != null) {
                builder.append(JSON.toJSONString(intent)).append(' ');
            }
        }
        return builder.toString().trim();
    }

    private ChatAgentResult parseChatAgentResult(String rawResponse, List<Map<String, Object>> recommendedClothes) {
        ChatAgentResult result = new ChatAgentResult();
        result.setRecommendedClothes(recommendedClothes);

        try {
            JSONObject json = extractJson(rawResponse);
            if (json == null) {
                result.setReply(rawResponse);
                return result;
            }

            result.setReply(defaultString(json.getString("reply")));
            JSONArray ids = json.getJSONArray("recommendedItemIds");
            if (ids == null || ids.isEmpty() || recommendedClothes == null || recommendedClothes.isEmpty()) {
                return result;
            }

            Set<Long> selectedIds = new LinkedHashSet<>();
            for (int i = 0; i < ids.size(); i++) {
                Long id = ids.getLong(i);
                if (id != null) {
                    selectedIds.add(id);
                }
            }

            if (selectedIds.isEmpty()) {
                return result;
            }

            List<Map<String, Object>> filtered = recommendedClothes.stream()
                    .filter(item -> {
                        Object id = item.get("id");
                        if (id instanceof Number number) {
                            return selectedIds.contains(number.longValue());
                        }
                        return false;
                    })
                    .collect(Collectors.toList());

            if (!filtered.isEmpty()) {
                result.setRecommendedClothes(filtered);
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to parse LangChain4j chat response: {}", e.getMessage());
            result.setReply(rawResponse);
            return result;
        }
    }

    private void ensureReply(ChatAgentResult result,
                             String userMessage,
                             List<Map<String, String>> chatHistory,
                             Map<String, Object> context,
                             List<Map<String, Object>> recommendedClothes) {
        if (result != null && !defaultString(result.getReply()).isBlank()) {
            return;
        }

        ChatAgentResult fallback = buildFallbackResult(userMessage, chatHistory, context, recommendedClothes);
        if (result != null) {
            result.setReply(fallback.getReply());
            result.setRecommendedClothes(fallback.getRecommendedClothes());
        }
    }

    private ChatAgentResult buildFallbackResult(String userMessage,
                                                List<Map<String, String>> chatHistory,
                                                Map<String, Object> context,
                                                List<Map<String, Object>> recommendedClothes) {
        ChatAgentResult fallback = new ChatAgentResult();
        fallback.setRecommendedClothes(recommendedClothes);

        String fallbackReply = textGeneratorService.generateResponse(userMessage, chatHistory, context);
        if (defaultString(fallbackReply).isBlank()) {
            fallbackReply = buildDefaultReply(userMessage, recommendedClothes);
        }
        fallback.setReply(fallbackReply);
        return fallback;
    }

    private String buildDefaultReply(String userMessage, List<Map<String, Object>> recommendedClothes) {
        String normalized = defaultString(userMessage).toLowerCase(Locale.ROOT);
        if (normalized.contains("穿") || normalized.contains("搭") || normalized.contains("衣") || normalized.contains("outfit")) {
            if (recommendedClothes != null && !recommendedClothes.isEmpty()) {
                return "我先根据你的衣橱筛了几件可搭配的单品，你可以继续告诉我天气、场景或风格，我再帮你细化整套穿搭。";
            }
            return "我可以继续帮你推荐穿搭。你可以直接告诉我天气、出行场景，或者先完善衣橱里的衣物信息。";
        }
        return "我已经收到你的问题。你可以再补充一点场景或目标，我会继续给你更具体的建议。";
    }

    private JSONObject extractJson(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return null;
        }

        String content = rawResponse.trim();
        if (content.startsWith("```")) {
            int firstLineBreak = content.indexOf('\n');
            if (firstLineBreak >= 0) {
                content = content.substring(firstLineBreak + 1);
            }
            if (content.endsWith("```")) {
                content = content.substring(0, content.length() - 3);
            }
            content = content.trim();
        }

        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            content = content.substring(start, end + 1);
        }
        return JSON.parseObject(content);
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    @Data
    public static class ChatAgentResult {
        private String reply;
        private List<Map<String, Object>> recommendedClothes = new ArrayList<>();
    }
}
