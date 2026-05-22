package com.ecru.common.service.ai;

import com.ecru.common.dto.ai.AiApiCallContext;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.TokenUsage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service("aiTextGeneratorStreamService")
public class AiTextGeneratorStreamService {

    private static final double STREAM_TEMPERATURE = 0.7d;
    private static final int STREAM_MAX_TOKENS = 2048;

    @Value("${ai.siliconflow.model:Qwen/Qwen3-VL-8B-Instruct}")
    private String modelName;

    @Autowired
    private AiPromptSettingsService promptSettingsService;

    @Autowired
    private AiApiMonitorService monitorService;

    @Autowired
    private OpenAiStreamingChatModel streamingChatModel;

    public Flux<String> generateStreamResponse(String prompt,
                                               List<Map<String, String>> chatHistory,
                                               Map<String, Object> context) {
        return generateStreamResponse(prompt, chatHistory, context, null);
    }

    public Flux<String> generateStreamResponse(String prompt,
                                               List<Map<String, String>> chatHistory,
                                               Map<String, Object> context,
                                               Long userId) {
        return generateStreamResponseWithCustomPrompt(null, prompt, chatHistory, context, userId);
    }

    public Flux<String> generateStreamResponseWithCustomPrompt(String systemPrompt,
                                                               String prompt,
                                                               List<Map<String, String>> chatHistory,
                                                               Map<String, Object> context) {
        return generateStreamResponseWithCustomPrompt(systemPrompt, prompt, chatHistory, context, null);
    }

    public Flux<String> generateStreamResponseWithCustomPrompt(String systemPrompt,
                                                               String prompt,
                                                               List<Map<String, String>> chatHistory,
                                                               Map<String, Object> context,
                                                               Long userId) {
        return Flux.create(sink -> {
            // 这一层是真正对接流式大模型 SDK 的地方。
            // 上层服务只管准备 prompt/上下文，这里负责把模型回调桥接成 Reactor Flux。
            AiApiCallContext monitorContext = AiApiCallContext.create(
                    AiApiMonitorWrapper.Scene.STREAM_CHAT, modelName, userId
            );
            AtomicBoolean recorded = new AtomicBoolean(false);
            AtomicBoolean cancelled = new AtomicBoolean(false);
            AtomicInteger responseLength = new AtomicInteger(0);

            try {
                ChatRequest request = ChatRequest.builder()
                        // buildChatMessages 会把 system prompt、最近历史和当前用户问题组织成最终消息列表。
                        .messages(buildChatMessages(systemPrompt, prompt, chatHistory, context))
                        .temperature(STREAM_TEMPERATURE)
                        .maxOutputTokens(STREAM_MAX_TOKENS)
                        .build();
                monitorContext.setPromptLength(request.toString().length());

                sink.onDispose(() -> {
                    // 如果前端中途断开 SSE，这里会收到取消信号，并把监控状态记为 cancelled。
                    cancelled.set(true);
                    recordCancelledIfNeeded(monitorContext, recorded, responseLength.get());
                });

                streamingChatModel.chat(request, new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String partialResponse) {
                        // 模型每吐出一小段文本，就立即转发给上层 Flux，
                        // 上层控制器再原样推送给前端，实现“打字机效果”。
                        if (cancelled.get() || sink.isCancelled() || partialResponse == null || partialResponse.isEmpty()) {
                            return;
                        }
                        responseLength.addAndGet(partialResponse.length());
                        sink.next(partialResponse);
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse response) {
                        if (cancelled.get() || sink.isCancelled()) {
                            recordCancelledIfNeeded(monitorContext, recorded, responseLength.get());
                            return;
                        }

                        // 只有模型完整结束时，才记录 token usage 并输出统一结束标记 [DONE]。
                        applyTokenUsage(monitorContext, response.tokenUsage());
                        monitorContext.markSuccess(200);
                        monitorContext.setResponseLength(responseLength.get());
                        recordOnce(monitorContext, recorded);
                        sink.next("[DONE]");
                        sink.complete();
                    }

                    @Override
                    public void onError(Throwable error) {
                        if (cancelled.get() || sink.isCancelled()) {
                            recordCancelledIfNeeded(monitorContext, recorded, responseLength.get());
                            return;
                        }

                        // 这里把异常转换为流内的 [ERROR] 片段，而不是直接抛出，
                        // 这样前端可以按统一协议处理错误状态。
                        log.error("Failed to stream AI response: {}", error.getMessage(), error);
                        monitorContext.markFailed(AiApiMonitorWrapper.ErrorType.BUSINESS_ERROR, error.getMessage(), null);
                        monitorContext.setResponseLength(responseLength.get());
                        recordOnce(monitorContext, recorded);
                        sink.next("[ERROR]" + error.getMessage());
                        sink.complete();
                    }
                });
            } catch (Exception e) {
                log.error("Failed to start AI stream response: {}", e.getMessage(), e);
                monitorContext.markFailed(AiApiMonitorWrapper.ErrorType.BUSINESS_ERROR, e.getMessage(), null);
                monitorContext.setResponseLength(responseLength.get());
                recordOnce(monitorContext, recorded);
                sink.next("[ERROR]" + e.getMessage());
                sink.complete();
            }
        });
    }

    private List<ChatMessage> buildChatMessages(String systemPrompt,
                                                String prompt,
                                                List<Map<String, String>> chatHistory,
                                                Map<String, Object> context) {
        // 消息组装顺序固定为：
        // system -> 最近几轮历史 -> 当前用户消息（内嵌业务上下文）
        // 这是当前流式对话 prompt 的最终入模结构。
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(resolveSystemPrompt(systemPrompt, context)));

        if (chatHistory != null && !chatHistory.isEmpty()) {
            // 这里只截最近 5 轮历史，避免长会话把流式首包时间拉长。
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

    private void applyTokenUsage(AiApiCallContext context, TokenUsage tokenUsage) {
        if (tokenUsage == null) {
            return;
        }
        context.setInputTokens(tokenUsage.inputTokenCount());
        context.setOutputTokens(tokenUsage.outputTokenCount());
        context.setTotalTokens(tokenUsage.totalTokenCount());
    }

    private void recordCancelledIfNeeded(AiApiCallContext context, AtomicBoolean recorded, int responseLength) {
        if (recorded.get()) {
            return;
        }
        context.markFailed(AiApiMonitorWrapper.ErrorType.BUSINESS_ERROR, "stream cancelled", 499);
        context.setResponseLength(responseLength);
        recordOnce(context, recorded);
    }

    private void recordOnce(AiApiCallContext context, AtomicBoolean recorded) {
        if (recorded.compareAndSet(false, true)) {
            monitorService.recordApiCall(context);
        }
    }

    private String resolveSystemPrompt(String systemPrompt, Map<String, Object> context) {
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            // 上层若显式传入 system prompt，则优先使用上层定义的更强约束版本。
            return systemPrompt;
        }

        StringBuilder prompt = new StringBuilder(promptSettingsService.getChatSystemPrompt());
        boolean needClothingSearch = context != null && Boolean.TRUE.equals(context.get("needClothingSearch"));
        if (needClothingSearch) {
            prompt.append("\n请优先基于候选衣物给出可执行的穿搭建议。");
        } else {
            prompt.append("\n如果用户只是闲聊、问候或问身份，不要强行推荐衣物。");
        }
        return prompt.toString();
    }

    private String buildUserPrompt(String prompt, Map<String, Object> context) {
        // 用户消息正文与业务上下文会被合并成一条最终 UserMessage。
        // 这样模型看到的是“问题 + 参考信息”，而不是只看到一句裸问题。
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

            boolean needClothingSearch = Boolean.TRUE.equals(context.get("needClothingSearch"));
            if (needClothingSearch && context.containsKey("clothes")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> clothes = (List<Map<String, Object>>) context.get("clothes");
                if (clothes != null && !clothes.isEmpty()) {
                    userContent.append("可用衣物:\n");
                    // 候选衣物在 prompt 里只保留前 5 个，控制 token，同时减少模型选择困难。
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
                } else {
                    userContent.append("当前没有可直接推荐的衣物候选。\n");
                }
            }
        }

        // 对最终用户提示做长度截断，避免上下文过长挤占模型输出空间。
        return limitLength(userContent.toString(), 1000);
    }

    private String limitLength(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text == null ? "" : text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
