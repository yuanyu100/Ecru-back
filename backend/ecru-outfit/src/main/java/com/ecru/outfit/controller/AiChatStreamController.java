package com.ecru.outfit.controller;

import com.ecru.common.util.UserContext;
import com.ecru.outfit.dto.request.ChatRequestDTO;
import com.ecru.outfit.service.AiChatStreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * AI对话流式控制器
 */
@Slf4j
@RestController
@RequestMapping("/ai-chat-stream")
@Tag(name = "AI对话流式", description = "AI智能对话流式输出接口")
public class AiChatStreamController {

    @Autowired
    private AiChatStreamService aiChatStreamService;

    /**
     * 流式发送消息并获取AI回复
     * 使用SSE(Server-Sent Events)格式返回流式响应
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI对话流式", description = "发送消息给AI助手并获取流式回复，支持打字机效果")
    public Flux<String> chatStream(@Valid @RequestBody ChatRequestDTO request) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return Flux.just("[ERROR]用户未登录");
        }

        log.info("流式对话请求, 用户ID: {}, 消息: {}", userId, request.getMessage());

        return aiChatStreamService.chatStream(userId, request)
                .doOnNext(chunk -> {
                    // 记录日志，但不记录内容（避免日志过大）
                    if (chunk.startsWith("[SESSION]")) {
                        log.info("发送会话信息: {}", chunk);
                    } else if (chunk.startsWith("[ERROR]")) {
                        log.error("流式响应错误: {}", chunk);
                    } else if (chunk.equals("[DONE]")) {
                        log.info("流式响应完成");
                    }
                })
                .doOnError(error -> {
                    log.error("流式对话失败: {}", error.getMessage(), error);
                })
                .onErrorResume(error -> {
                    return Flux.just("[ERROR]" + error.getMessage());
                });
    }

    /**
     * 测试流式接口
     */
    @GetMapping(value = "/test", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "测试流式接口", description = "测试SSE流式输出是否正常工作")
    public Flux<String> testStream() {
        return Flux.interval(java.time.Duration.ofMillis(100))
                .take(10)
                .map(i -> "消息 " + (i + 1) + "\n")
                .concatWith(Flux.just("[DONE]"));
    }

}
