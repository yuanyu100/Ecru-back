package com.ecru.common.service.ai;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

    public Flux<String> generateStreamResponse(String prompt,
                                               List<Map<String, String>> chatHistory,
                                               Map<String, Object> context) {
        return generateStreamResponseWithCustomPrompt(null, prompt, chatHistory, context);
    }

    public Flux<String> generateStreamResponseWithCustomPrompt(String systemPrompt,
                                                               String prompt,
                                                               List<Map<String, String>> chatHistory,
                                                               Map<String, Object> context) {
        return Flux.create(sink -> {
            try {
                String endpoint = "/chat/completions";
                String fullUrl = baseUrl + endpoint;
                log.info("AI Stream API URL: {}", fullUrl);
                log.info("AI Stream Model: {}", modelName);

                JSONObject requestBody = new JSONObject();
                requestBody.put("model", modelName);
                requestBody.put("temperature", 0.7);
                requestBody.put("max_tokens", 2048);
                requestBody.put("stream", true);

                JSONArray messages = new JSONArray();

                JSONObject systemMessage = new JSONObject();
                systemMessage.put("role", "system");
                systemMessage.put(
                        "content",
                        systemPrompt != null && !systemPrompt.trim().isEmpty()
                                ? systemPrompt
                                : buildSystemPrompt(context)
                );
                messages.add(systemMessage);

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

                JSONObject userMessage = new JSONObject();
                userMessage.put("role", "user");
                userMessage.put("content", buildUserPrompt(prompt, context));
                messages.add(userMessage);

                requestBody.put("messages", messages);

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

                Call call = okHttpClient.newCall(request);
                sink.onDispose(() -> {
                    if (!call.isCanceled()) {
                        log.info("AI stream request cancelled by client");
                        call.cancel();
                    }
                });

                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        if (call.isCanceled() || sink.isCancelled()) {
                            log.info("AI stream request cancelled before completion");
                            sink.complete();
                            return;
                        }

                        log.error("流式请求失败: {}", e.getMessage(), e);
                        sink.next("[ERROR]" + e.getMessage());
                        sink.complete();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (!response.isSuccessful()) {
                            String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                            log.error("API调用失败: {} {} - {}", response.code(), response.message(), errorBody);
                            if (!sink.isCancelled()) {
                                sink.next("[ERROR]API调用失败: " + response.code());
                                sink.complete();
                            }
                            return;
                        }

                        try (ResponseBody responseBody = response.body();
                             BufferedReader reader = new BufferedReader(
                                     new InputStreamReader(responseBody.byteStream(), StandardCharsets.UTF_8))) {
                            String line;
                            while (!sink.isCancelled() && (line = reader.readLine()) != null) {
                                if (!line.startsWith("data: ")) {
                                    continue;
                                }

                                String data = line.substring(6);
                                if ("[DONE]".equals(data)) {
                                    sink.next("[DONE]");
                                    sink.complete();
                                    return;
                                }

                                try {
                                    JSONObject jsonData = JSON.parseObject(data);
                                    if (!jsonData.containsKey("choices")) {
                                        continue;
                                    }

                                    JSONArray choices = jsonData.getJSONArray("choices");
                                    if (choices.isEmpty()) {
                                        continue;
                                    }

                                    JSONObject choice = choices.getJSONObject(0);
                                    if (!choice.containsKey("delta")) {
                                        continue;
                                    }

                                    JSONObject delta = choice.getJSONObject("delta");
                                    if (!delta.containsKey("content")) {
                                        continue;
                                    }

                                    String content = delta.getString("content");
                                    if (content != null && !content.isEmpty()) {
                                        sink.next(content);
                                    }
                                } catch (Exception e) {
                                    log.warn("解析流式数据失败: {}", e.getMessage());
                                }
                            }
                        } catch (Exception e) {
                            if (call.isCanceled() || sink.isCancelled()) {
                                log.info("AI stream response closed after cancellation");
                                sink.complete();
                                return;
                            }

                            log.error("读取流式响应失败: {}", e.getMessage(), e);
                            sink.next("[ERROR]" + e.getMessage());
                        } finally {
                            if (!sink.isCancelled()) {
                                sink.complete();
                            }
                        }
                    }
                });
            } catch (Exception e) {
                log.error("生成流式响应失败: {}", e.getMessage(), e);
                sink.next("[ERROR]" + e.getMessage());
                sink.complete();
            }
        });
    }

    private String buildSystemPrompt(Map<String, Object> context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(promptSettingsService.getChatSystemPrompt());
        prompt.append("你是一位专业的时尚搭配顾问。");

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
