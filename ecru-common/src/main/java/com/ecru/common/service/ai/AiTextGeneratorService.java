package com.ecru.common.service.ai;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ecru.common.dto.ai.AiApiCallContext;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * AI文本生成服务
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
     * @param prompt 提示文本
     * @param chatHistory 聊天历史
     * @param context 上下文信息
     * @param userId 用户ID（用于监控）
     * @return 生成的文本
     */
    public String generateResponse(String prompt, List<Map<String, String>> chatHistory, 
                                    Map<String, Object> context, Long userId) {
        // 创建监控上下文
        AiApiCallContext monitorContext = AiApiCallContext.create(
                AiApiMonitorWrapper.Scene.CHAT_GENERATE, modelName, userId);
        
        try {
            String endpoint = "/chat/completions";
            String fullUrl = baseUrl + endpoint;
            log.debug("AI API URL: {}", fullUrl);
            log.debug("AI Model: {}", modelName);

            // 构建请求体
            JSONObject requestBody = buildChatRequestBody(prompt, chatHistory, context);
            monitorContext.setPromptLength(requestBody.toJSONString().length());

            // 创建请求
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

            // 执行请求
            try (Response response = okHttpClient.newCall(request).execute()) {
                String responseBody = response.body().string();
                
                if (!response.isSuccessful()) {
                    monitorContext.markFailed(AiApiMonitorWrapper.ErrorType.HTTP_ERROR,
                            "API调用失败: " + response.code(), response.code());
                    throw new IOException("API调用失败: " + response.code() + " " + response.message() + " - " + responseBody);
                }

                JSONObject responseJson = JSON.parseObject(responseBody);
                
                // 提取token使用量
                monitorService.extractTokenUsage(monitorContext, responseJson);
                monitorContext.markSuccess(response.code());

                // 解析响应
                String content = parseChatResponse(responseJson);
                monitorContext.setResponseLength(content != null ? content.length() : 0);
                
                return content;
            }
        } catch (Exception e) {
            log.error("生成文本响应失败: {}", e.getMessage(), e);
            if (monitorContext.getStatus() == 1) {
                // 如果还没有标记失败，则标记
                monitorContext.markFailed(AiApiMonitorWrapper.ErrorType.BUSINESS_ERROR,
                        e.getMessage(), null);
            }
            return "根据您的需求，我为您推荐了一些合适的衣物搭配。请查看推荐结果。";
        } finally {
            // 异步记录调用
            monitorService.recordApiCall(monitorContext);
        }
    }

    /**
     * 生成文本响应（兼容旧版本，无userId）
     * @deprecated 请使用带userId的新版本
     */
    @Deprecated
    public String generateResponse(String prompt, List<Map<String, String>> chatHistory, 
                                    Map<String, Object> context) {
        return generateResponse(prompt, chatHistory, context, null);
    }

    /**
     * 分析用户查询意图
     * @param query 用户查询
     * @param userId 用户ID（用于监控）
     * @return 分析结果
     */
    public Map<String, Object> analyzeQueryIntent(String query, Long userId) {
        AiApiCallContext monitorContext = AiApiCallContext.create(
                AiApiMonitorWrapper.Scene.INTENT_ANALYZE, modelName, userId);
        
        try {
            String endpoint = "/chat/completions";
            String fullUrl = baseUrl + endpoint;

            // 构建请求体
            JSONObject requestBody = buildIntentAnalysisRequestBody(query);
            monitorContext.setPromptLength(requestBody.toJSONString().length());

            // 创建请求
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

            // 执行请求
            try (Response response = okHttpClient.newCall(request).execute()) {
                String responseBody = response.body().string();
                
                if (!response.isSuccessful()) {
                    monitorContext.markFailed(AiApiMonitorWrapper.ErrorType.HTTP_ERROR,
                            "API调用失败: " + response.code(), response.code());
                    throw new IOException("API调用失败: " + response.code() + " " + response.message() + " - " + responseBody);
                }

                JSONObject responseJson = JSON.parseObject(responseBody);
                
                // 提取token使用量
                monitorService.extractTokenUsage(monitorContext, responseJson);
                monitorContext.markSuccess(response.code());

                // 解析响应
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
            return buildDefaultIntentResult(query);
        } finally {
            monitorService.recordApiCall(monitorContext);
        }
    }

    /**
     * 分析用户查询意图（兼容旧版本，无userId）
     * @deprecated 请使用带userId的新版本
     */
    @Deprecated
    public Map<String, Object> analyzeQueryIntent(String query) {
        return analyzeQueryIntent(query, null);
    }

    // ==================== 私有方法 ====================

    private JSONObject buildChatRequestBody(String prompt, List<Map<String, String>> chatHistory, 
                                             Map<String, Object> context) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", modelName);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 2048);

        JSONArray messages = new JSONArray();

        // 添加系统提示
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是一位专业的时尚搭配顾问，根据用户提供的信息和衣橱中的衣物，生成个性化的搭配建议。请考虑天气、场合、用户风格偏好等因素，提供详细的搭配方案和专业分析。");
        messages.add(systemMessage);

        // 添加聊天历史
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

        // 构建上下文信息
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

        // 添加用户提示
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        String userContent = prompt + "\n\n" + contextBuilder.toString();
        if (userContent.length() > 1000) {
            userContent = userContent.substring(0, 1000) + "...";
        }
        userMessage.put("content", userContent);
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

        // 添加系统提示
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        String systemPrompt = "你是一个查询意图分析器，需要分析用户的时尚搭配相关查询，提取关键信息。请以JSON格式返回分析结果，包含以下字段：\n1. intent: 查询意图（如：搭配推荐、单品推荐、风格咨询、天气相关等）\n2. occasion: 场合（如：日常、工作、约会、聚会等）\n3. season: 季节（如：春、夏、秋、冬）\n4. style: 风格偏好（如：休闲、正式、运动、时尚等）\n5. weather: 天气相关需求（如：保暖、凉爽、防雨等）\n6. keywords: 关键词列表\n7. clothingType: 衣物类型需求（如：上衣、裤子、裙子、外套等）\n8. negativePreferences: 负面偏好（如：不喜欢的颜色、款式等）\n9. isNegative: 是否为负面偏好表达（true/false）";
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);

        // 添加用户查询
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
        Map<String, Object> defaultResult = new java.util.HashMap<>();
        defaultResult.put("intent", "搭配推荐");
        defaultResult.put("keywords", java.util.Arrays.asList(query.split(" ")));
        defaultResult.put("isNegative", query.contains("不喜欢") || query.contains("不要") || query.contains("讨厌"));
