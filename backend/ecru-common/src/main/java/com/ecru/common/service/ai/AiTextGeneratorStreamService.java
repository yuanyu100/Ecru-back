package com.ecru.common.service.ai;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * AI文本生成流式服务
 */
@Slf4j
@Service("aiTextGeneratorStreamService")
public class AiTextGeneratorStreamService {

    @Value("${ai.siliconflow.api-key:sk-rfukaeuhisaxyfjyfnigayueguxsfwikfswwjubmggxhwvvb}")
    private String apiKey;

    @Value("${ai.siliconflow.base-url:https://api.siliconflow.cn/v1}")
    private String baseUrl;

    @Value("${ai.siliconflow.model:Qwen/Qwen3-VL-8B-Instruct}")
    private String modelName;

    @Value("${ai.siliconflow.timeout:300000}")
    private Integer timeout;

    @Autowired
    private AiPromptSettingsService promptSettingsService;

    private OkHttpClient okHttpClient;

    @PostConstruct
    public void init() {
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                .readTimeout(timeout, TimeUnit.MILLISECONDS)
                .writeTimeout(timeout, TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * 生成流式文本响应
     * @param prompt 提示文本
     * @param chatHistory 聊天历史
     * @param context 上下文信息
     * @return 流式响应
     */
    public Flux<String> generateStreamResponse(String prompt, List<Map<String, String>> chatHistory, Map<String, Object> context) {
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();

        try {
            String endpoint = "/chat/completions";
            String fullUrl = baseUrl + endpoint;
            log.info("AI Stream API URL: {}", fullUrl);
            log.info("AI Stream Model: {}", modelName);

            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", modelName);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2048);
            requestBody.put("stream", true); // 启用流式输出

            // 构建消息
            JSONArray messages = new JSONArray();

            // 添加系统提示
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", buildSystemPrompt(context));
            messages.add(systemMessage);

            // 添加聊天历史
            if (chatHistory != null && !chatHistory.isEmpty()) {
                log.info("聊天历史数量: {}", chatHistory.size());
                int startIndex = Math.max(0, chatHistory.size() - 5);
                for (int i = startIndex; i < chatHistory.size(); i++) {
                    Map<String, String> message = chatHistory.get(i);
                    JSONObject historyMessage = new JSONObject();
                    historyMessage.put("role", message.get("role"));
                    historyMessage.put("content", message.get("content"));
                    messages.add(historyMessage);
                }
            }

            // 添加用户提示
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", buildUserPrompt(prompt, context));
            messages.add(userMessage);

            requestBody.put("messages", messages);

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

            // 在异步线程中执行请求
            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    log.error("流式请求失败: {}", e.getMessage(), e);
                    sink.tryEmitNext("[ERROR]" + e.getMessage());
                    sink.tryEmitComplete();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        log.error("API调用失败: {} {} - {}", response.code(), response.message(), errorBody);
                        sink.tryEmitNext("[ERROR]API调用失败: " + response.code());
                        sink.tryEmitComplete();
                        return;
                    }

                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(response.body().byteStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("data: ")) {
                                String data = line.substring(6);
                                if ("[DONE]".equals(data)) {
                                    sink.tryEmitNext("[DONE]");
                                    sink.tryEmitComplete();
                                    return;
                                }

                                try {
                                    JSONObject jsonData = JSON.parseObject(data);
                                    if (jsonData.containsKey("choices")) {
                                        JSONArray choices = jsonData.getJSONArray("choices");
                                        if (!choices.isEmpty()) {
                                            JSONObject choice = choices.getJSONObject(0);
                                            if (choice.containsKey("delta")) {
                                                JSONObject delta = choice.getJSONObject("delta");
                                                if (delta.containsKey("content")) {
                                                    String content = delta.getString("content");
                                                    if (content != null && !content.isEmpty()) {
                                                        sink.tryEmitNext(content);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    log.warn("解析流式数据失败: {}", e.getMessage());
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("读取流式响应失败: {}", e.getMessage(), e);
                        sink.tryEmitNext("[ERROR]" + e.getMessage());
                    } finally {
                        sink.tryEmitComplete();
                    }
                }
            });

        } catch (Exception e) {
            log.error("生成流式响应失败: {}", e.getMessage(), e);
            sink.tryEmitNext("[ERROR]" + e.getMessage());
            sink.tryEmitComplete();
        }

        return sink.asFlux();
    }

    /**
     * 构建系统提示
     */
    private String buildSystemPrompt(Map<String, Object> context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(promptSettingsService.getChatSystemPrompt());
        prompt.append("你是一位专业的时尚搭配顾问。");

        // 根据是否需要检索衣柜调整提示
        prompt.setLength(0);
        prompt.append(promptSettingsService.getChatSystemPrompt());
        Boolean needClothingSearch = context != null && Boolean.TRUE.equals(context.get("needClothingSearch"));
        if (needClothingSearch != null && !needClothingSearch) {
            prompt.append("用户只是打招呼或闲聊，请友好地回应，不需要推荐衣物。");
        } else {
            prompt.append("根据用户提供的信息和衣橱中的衣物，生成个性化的搭配建议。");
            prompt.append("请考虑天气、场合、用户风格偏好等因素，提供详细的搭配方案和专业分析。");
        }

        return prompt.toString();
    }

    /**
     * 构建用户提示
     */
    private String buildUserPrompt(String prompt, Map<String, Object> context) {
        StringBuilder userContent = new StringBuilder();
        userContent.append(prompt);

        if (context != null) {
            userContent.append("\n\n");

            if (context.containsKey("weather")) {
                userContent.append("天气信息：").append(context.get("weather")).append("\n");
            }
            if (context.containsKey("occasion")) {
                userContent.append("场合：").append(context.get("occasion")).append("\n");
            }

            // 只在需要检索衣柜时添加衣物信息
            Boolean needClothingSearch = Boolean.TRUE.equals(context.get("needClothingSearch"));
            if (needClothingSearch && context.containsKey("clothes")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> clothes = (List<Map<String, Object>>) context.get("clothes");
                if (clothes != null && !clothes.isEmpty()) {
                    userContent.append("衣橱中的衣物：\n");
                    int limit = Math.min(5, clothes.size());
                    for (int i = 0; i < limit; i++) {
                        Map<String, Object> cloth = clothes.get(i);
                        userContent.append(i + 1).append(". ")
                                .append(cloth.get("name"))
                                .append(" (").append(cloth.get("category")).append(")")
                                .append(" - 颜色：").append(cloth.get("color"))
                                .append("\n");
                    }
                } else {
                    userContent.append("（用户衣橱中暂无匹配衣物）\n");
                }
            }

            if (context.containsKey("negativePreferences")) {
                userContent.append("用户不喜欢：").append(context.get("negativePreferences")).append("\n");
            }
        }

        return userContent.toString();
    }
}
