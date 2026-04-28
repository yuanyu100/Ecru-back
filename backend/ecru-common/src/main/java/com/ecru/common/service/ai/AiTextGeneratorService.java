package com.ecru.common.service.ai;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ecru.common.dto.ai.AiApiCallContext;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AI 文本生成服务
 */
@Slf4j
@Service("aiTextGeneratorService")
public class AiTextGeneratorService {

    @Value("${ai.siliconflow.api-key:sk-rfukaeuhisaxyfjyfnigayueguxsfwikfswwjubmggxhwvvb}")
    private String apiKey;

    @Value("${ai.siliconflow.base-url:https://api.siliconflow.cn/v1}")
    private String baseUrl;

    @Value("${ai.siliconflow.model:Qwen/Qwen3-VL-8B-Instruct}")
    private String modelName;

    @Value("${ai.siliconflow.timeout:300000}")
    private Integer timeout;

    @Autowired
    private AiApiMonitorService monitorService;

    @Autowired
    private AiPromptSettingsService promptSettingsService;

    private OkHttpClient okHttpClient;

    @PostConstruct
    public void init() {
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(timeout, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(timeout, java.util.concurrent.TimeUnit.MILLISECONDS)
                .writeTimeout(timeout, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * 生成文本响应
     */
    public String generateResponse(String prompt, List<Map<String, String>> chatHistory,
                                   Map<String, Object> context, Long userId) {
        AiApiCallContext monitorContext = AiApiCallContext.create(
                AiApiMonitorWrapper.Scene.CHAT_GENERATE, modelName, userId);

        try {
            String endpoint = "/chat/completions";
            String fullUrl = baseUrl + endpoint;
            log.debug("AI API URL: {}", fullUrl);
            log.debug("AI Model: {}", modelName);

            JSONObject requestBody = buildChatRequestBody(prompt, chatHistory, context);
            monitorContext.setPromptLength(requestBody.toJSONString().length());

            RequestBody body = RequestBody.create(
                    requestBody.toJSONString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(fullUrl)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                String responseBody = response.body().string();

                if (!response.isSuccessful()) {
                    monitorContext.markFailed(AiApiMonitorWrapper.ErrorType.HTTP_ERROR,
                            "API调用失败: " + response.code(), response.code());
                    throw new IOException("API调用失败: " + response.code() + " " + response.message() + " - " + responseBody);
                }

                JSONObject responseJson = JSON.parseObject(responseBody);
                monitorService.extractTokenUsage(monitorContext, responseJson);
                monitorContext.markSuccess(response.code());

                String content = parseChatResponse(responseJson);
                monitorContext.setResponseLength(content != null ? content.length() : 0);
                return content;
            }
        } catch (Exception e) {
            log.error("生成文本响应失败: {}", e.getMessage(), e);
            if (monitorContext.getStatus() == 1) {
                monitorContext.markFailed(AiApiMonitorWrapper.ErrorType.BUSINESS_ERROR,
                        e.getMessage(), null);
            }
            return "根据您的需求，我为您推荐了一些合适的衣物搭配。请查看推荐结果。";
        } finally {
            monitorService.recordApiCall(monitorContext);
        }
    }

    /**
     * 兼容旧版本，无 userId。
     */
    @Deprecated
    public String generateResponse(String prompt, List<Map<String, String>> chatHistory,
                                   Map<String, Object> context) {
        return generateResponse(prompt, chatHistory, context, null);
    }

    /**
     * 生成自定义系统提示的文本响应，失败时返回 null，交由上层决定回退策略。
     */
    public String generateCustomResponse(String systemPrompt, String prompt,
                                         Map<String, Object> context, Long userId) {
        AiApiCallContext monitorContext = AiApiCallContext.create(
                AiApiMonitorWrapper.Scene.CHAT_GENERATE, modelName, userId);

        try {
            String endpoint = "/chat/completions";
            String fullUrl = baseUrl + endpoint;

            JSONObject requestBody = buildCustomChatRequestBody(systemPrompt, prompt, context);
            monitorContext.setPromptLength(requestBody.toJSONString().length());

            RequestBody body = RequestBody.create(
                    requestBody.toJSONString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(fullUrl)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                String responseBody = response.body().string();

                if (!response.isSuccessful()) {
                    monitorContext.markFailed(AiApiMonitorWrapper.ErrorType.HTTP_ERROR,
                            "API调用失败: " + response.code(), response.code());
                    throw new IOException("API调用失败: " + response.code() + " " + response.message() + " - " + responseBody);
                }

                JSONObject responseJson = JSON.parseObject(responseBody);
                monitorService.extractTokenUsage(monitorContext, responseJson);
                monitorContext.markSuccess(response.code());

                String content = parseChatResponse(responseJson);
                monitorContext.setResponseLength(content != null ? content.length() : 0);
                return content;
            }
        } catch (Exception e) {
            log.error("生成自定义文本回复失败: {}", e.getMessage(), e);
            if (monitorContext.getStatus() == 1) {
                monitorContext.markFailed(AiApiMonitorWrapper.ErrorType.BUSINESS_ERROR,
                        e.getMessage(), null);
            }
            return null;
        } finally {
            monitorService.recordApiCall(monitorContext);
        }
    }

    /**
     * 分析用户查询意图。
     */
    public Map<String, Object> analyzeQueryIntent(String query, Long userId) {
        String normalizedQuery = normalizeQuery(query);
        if (isSimpleGreetingQuery(normalizedQuery) || isIdentityQuestionQuery(normalizedQuery)) {
            return buildFallbackIntentResult(normalizedQuery);
        }

        AiApiCallContext monitorContext = AiApiCallContext.create(
                AiApiMonitorWrapper.Scene.INTENT_ANALYZE, modelName, userId);

        try {
            String endpoint = "/chat/completions";
            String fullUrl = baseUrl + endpoint;

            JSONObject requestBody = buildIntentAnalysisRequestBody(normalizedQuery);
            monitorContext.setPromptLength(requestBody.toJSONString().length());

            RequestBody body = RequestBody.create(
                    requestBody.toJSONString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(fullUrl)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                String responseBody = response.body().string();

                if (!response.isSuccessful()) {
                    monitorContext.markFailed(AiApiMonitorWrapper.ErrorType.HTTP_ERROR,
                            "API调用失败: " + response.code(), response.code());
                    throw new IOException("API调用失败: " + response.code() + " " + response.message() + " - " + responseBody);
                }

                JSONObject responseJson = JSON.parseObject(responseBody);
                monitorService.extractTokenUsage(monitorContext, responseJson);
                monitorContext.markSuccess(response.code());

                Map<String, Object> result = parseIntentResponse(responseJson);
                monitorContext.setResponseLength(result != null ? result.toString().length() : 0);
                return result;
            }
        } catch (Exception e) {
            log.error("分析查询意图失败: {}", e.getMessage(), e);
            if (monitorContext.getStatus() == 1) {
                monitorContext.markFailed(AiApiMonitorWrapper.ErrorType.BUSINESS_ERROR,
                        e.getMessage(), null);
            }
            return buildFallbackIntentResult(normalizedQuery);
        } finally {
            monitorService.recordApiCall(monitorContext);
        }
    }

    /**
     * 兼容旧版本，无 userId。
     */
    @Deprecated
    public Map<String, Object> analyzeQueryIntent(String query) {
        return analyzeQueryIntent(query, null);
    }

    private JSONObject buildChatRequestBody(String prompt, List<Map<String, String>> chatHistory,
                                            Map<String, Object> context) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", modelName);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 2048);

        JSONArray messages = new JSONArray();

        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");

        systemMessage.put("content", promptSettingsService.getChatSystemPrompt());
        messages.add(systemMessage);

        if (chatHistory != null && !chatHistory.isEmpty()) {
            int startIndex = Math.max(0, chatHistory.size() - 5);
            for (int i = startIndex; i < chatHistory.size(); i++) {
                Map<String, String> message = chatHistory.get(i);
                JSONObject historyMessage = new JSONObject();
                historyMessage.put("role", message.get("role"));
                historyMessage.put("content", message.get("content"));
                messages.add(historyMessage);
            }
        }

        StringBuilder contextBuilder = new StringBuilder();
        if (context != null) {
            if (context.containsKey("weather")) {
                contextBuilder.append("天气信息：").append(context.get("weather")).append("\n");
            }
            if (context.containsKey("occasion")) {
                contextBuilder.append("场合：").append(context.get("occasion")).append("\n");
            }
            if (context.containsKey("clothes")) {
                contextBuilder.append("衣橱中的衣物：\n");
                List<Map<String, Object>> clothes = (List<Map<String, Object>>) context.get("clothes");
                int clothesLimit = Math.min(5, clothes.size());
                for (int i = 0; i < clothesLimit; i++) {
                    Map<String, Object> cloth = clothes.get(i);
                    contextBuilder.append(i + 1).append(". ")
                            .append(cloth.get("name"))
                            .append(" (").append(cloth.get("category")).append(")")
                            .append(" - 颜色：").append(cloth.get("color"))
                            .append("\n");
                }
            }
            if (context.containsKey("negativePreferences")) {
                contextBuilder.append("用户不喜欢：").append(context.get("negativePreferences")).append("\n");
            }
        }

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        String userContent = prompt + "\n\n" + contextBuilder;
        if (userContent.length() > 1000) {
            userContent = userContent.substring(0, 1000) + "...";
        }
        userMessage.put("content", userContent);
        messages.add(userMessage);

        requestBody.put("messages", messages);
        return requestBody;
    }

    private JSONObject buildCustomChatRequestBody(String systemPrompt, String prompt,
                                                  Map<String, Object> context) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", modelName);
        requestBody.put("temperature", 0.4);
        requestBody.put("max_tokens", 1024);

        JSONArray messages = new JSONArray();

        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");

        StringBuilder userContent = new StringBuilder();
        userContent.append(prompt == null ? "" : prompt);
        if (context != null && !context.isEmpty()) {
            userContent.append("\n\n参考上下文：").append(JSON.toJSONString(context));
        }
        String finalContent = userContent.toString();
        if (finalContent.length() > 3000) {
            finalContent = finalContent.substring(0, 3000) + "...";
        }
        userMessage.put("content", finalContent);
        messages.add(userMessage);

        requestBody.put("messages", messages);
        return requestBody;
    }

    private JSONObject buildIntentAnalysisRequestBody(String query) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", modelName);
        requestBody.put("temperature", 0.3);
        requestBody.put("max_tokens", 512);

        JSONArray messages = new JSONArray();

        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        String systemPrompt = "你是一个查询意图分析器，需要分析用户的时尚搭配相关查询，提取关键信息。请以JSON格式返回分析结果，包含以下字段：\n"
                + "1. intent: 查询意图（如：搭配推荐、单品推荐、风格咨询、天气相关等）\n"
                + "2. occasion: 场合（如：日常、工作、约会、聚会等）\n"
                + "3. season: 季节（如：春、夏、秋、冬）\n"
                + "4. style: 风格偏好（如：休闲、正式、运动、时尚等）\n"
                + "5. weather: 天气相关需求（如：保暖、凉爽、防雨等）\n"
                + "6. keywords: 关键词列表\n"
                + "7. clothingType: 衣物类型需求（如：上衣、裤子、裙子、外套等）\n"
                + "8. negativePreferences: 负面偏好（如：不喜欢的颜色、款式等）\n"
                + "9. isNegative: 是否为负面偏好表达（true/false）";
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", query);
        messages.add(userMessage);

        requestBody.put("messages", messages);
        return requestBody;
    }

    private String parseChatResponse(JSONObject responseJson) throws IOException {
        if (responseJson.containsKey("choices") && !responseJson.getJSONArray("choices").isEmpty()) {
            JSONObject choice = responseJson.getJSONArray("choices").getJSONObject(0);
            if (choice.containsKey("message")) {
                JSONObject message = choice.getJSONObject("message");
                if (message.containsKey("content")) {
                    return message.getString("content");
                }
            }
        }
        throw new IOException("API响应格式错误: " + responseJson.toJSONString());
    }

    private Map<String, Object> parseIntentResponse(JSONObject responseJson) throws IOException {
        if (responseJson.containsKey("choices") && !responseJson.getJSONArray("choices").isEmpty()) {
            JSONObject choice = responseJson.getJSONArray("choices").getJSONObject(0);
            if (choice.containsKey("message")) {
                JSONObject message = choice.getJSONObject("message");
                if (message.containsKey("content")) {
                    String content = message.getString("content");
                    int start = content.indexOf('{');
                    int end = content.lastIndexOf('}') + 1;
                    if (start != -1 && end != -1) {
                        String jsonStr = content.substring(start, end);
                        return JSON.parseObject(jsonStr, Map.class);
                    }
                }
            }
        }
        throw new IOException("API响应格式错误: " + responseJson.toJSONString());
    }

    private Map<String, Object> buildDefaultIntentResult(String query) {
        Map<String, Object> defaultResult = new HashMap<>();
        defaultResult.put("intent", "搭配推荐");
        defaultResult.put("keywords", Arrays.asList(query.split(" ")));
        defaultResult.put("isNegative", query.contains("不喜欢") || query.contains("不要") || query.contains("讨厌"));
        defaultResult.put("occasion", null);
        defaultResult.put("season", null);
        defaultResult.put("style", null);
        defaultResult.put("weather", null);
        defaultResult.put("clothingType", null);
        defaultResult.put("negativePreferences", new ArrayList<String>());
        return defaultResult;
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
                "\u4f60\u597d",
                "\u60a8\u597d",
                "\u54c8\u55bd",
                "\u55e8",
                "\u65e9\u4e0a\u597d",
                "\u4e0a\u5348\u597d",
                "\u4e2d\u5348\u597d",
                "\u4e0b\u5348\u597d",
                "\u665a\u4e0a\u597d",
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

    private boolean isIdentityQuestionQuery(String query) {
        String normalized = normalizeQuery(query).toLowerCase();
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

    private String resolveFallbackIntent(String query) {
        if (isSimpleGreetingQuery(query)) {
            return "greeting";
        }
        if (isIdentityQuestionQuery(query)) {
            return "identity";
        }
        return "\u642d\u914d\u63a8\u8350";
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
        return normalized.contains("\u4e0d\u559c\u6b22")
                || normalized.contains("\u4e0d\u8981")
                || normalized.contains("\u8ba8\u538c");
    }
}
