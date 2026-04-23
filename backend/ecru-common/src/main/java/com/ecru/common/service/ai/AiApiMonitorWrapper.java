package com.ecru.common.service.ai;

import com.alibaba.fastjson2.JSONObject;
import com.ecru.common.dto.ai.AiApiCallContext;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * AI API监控包装器
 * 提供便捷的监控集成方式，包装AI API调用
 */
@Slf4j
@Component
public class AiApiMonitorWrapper {

    @Autowired
    private AiApiMonitorService monitorService;

    /**
     * 场景常量定义
     */
    public static class Scene {
        public static final String CHAT_GENERATE = "chat_generate";
        public static final String INTENT_ANALYZE = "intent_analyze";
        public static final String IMAGE_ANALYZE = "image_analyze";
        public static final String CLOTHING_ANALYZE = "clothing_analyze";
        public static final String STREAM_CHAT = "stream_chat";
        public static final String TITLE_GENERATE = "title_generate";
    }

    /**
     * 错误类型常量
     */
    public static class ErrorType {
        public static final String TIMEOUT = "timeout";
        public static final String HTTP_ERROR = "http_error";
        public static final String PARSE_ERROR = "parse_error";
        public static final String BUSINESS_ERROR = "business_error";
        public static final String NETWORK_ERROR = "network_error";
    }

    /**
     * 执行带监控的AI API调用
     *
     * @param scene     调用场景
     * @param model     模型名称
     * @param userId    用户ID
     * @param prompt    提示词（用于计算长度）
     * @param executor  实际执行逻辑
     * @return API响应
     */
    public JSONObject executeWithMonitor(String scene, String model, Long userId, String prompt,
                                          ApiExecutor executor) {
        // 创建监控上下文
        AiApiCallContext context = AiApiCallContext.create(scene, model, userId);
        context.setPromptLength(prompt != null ? prompt.length() : 0);

        JSONObject response = null;
        try {
            // 执行API调用
            ApiResult result = executor.execute();
            response = result.getResponse();
            Response okResponse = result.getOkResponse();

            // 设置响应信息
            context.markSuccess(okResponse.code());
            context.setResponseLength(response != null ? response.toString().length() : 0);

            // 提取token使用量
            monitorService.extractTokenUsage(context, response);

        } catch (IOException e) {
            handleException(context, e);
        } catch (Exception e) {
            handleException(context, e);
        } finally {
            // 异步记录调用
            monitorService.recordApiCall(context);
        }

        return response;
    }

    /**
     * 执行带监控的AI API调用（简化版，无返回值）
     *
     * @param scene     调用场景
     * @param model     模型名称
     * @param userId    用户ID
     * @param prompt    提示词
     * @param executor  实际执行逻辑
     */
    public void executeWithMonitorVoid(String scene, String model, Long userId, String prompt,
                                        ApiExecutorVoid executor) {
        AiApiCallContext context = AiApiCallContext.create(scene, model, userId);
        context.setPromptLength(prompt != null ? prompt.length() : 0);

        try {
            executor.execute();
            context.markSuccess(200);
        } catch (IOException e) {
            handleException(context, e);
        } catch (Exception e) {
            handleException(context, e);
        } finally {
            monitorService.recordApiCall(context);
        }
    }

    /**
     * 手动记录成功的API调用
     * 用于流式响应等无法自动监控的场景
     */
    public void recordSuccess(String scene, String model, Long userId, String prompt,
                              JSONObject response, int httpCode) {
        AiApiCallContext context = AiApiCallContext.create(scene, model, userId);
        context.setPromptLength(prompt != null ? prompt.length() : 0);
        context.markSuccess(httpCode);
        context.setResponseLength(response != null ? response.toString().length() : 0);
        monitorService.extractTokenUsage(context, response);
        monitorService.recordApiCall(context);
    }

    /**
     * 手动记录失败的API调用
     */
    public void recordFailure(String scene, String model, Long userId, String prompt,
                              String errorType, String errorMessage, Integer httpCode) {
        AiApiCallContext context = AiApiCallContext.create(scene, model, userId);
        context.setPromptLength(prompt != null ? prompt.length() : 0);
        context.markFailed(errorType, errorMessage, httpCode);
        monitorService.recordApiCall(context);
    }

    /**
     * 处理异常并设置错误信息
     */
    private void handleException(AiApiCallContext context, IOException e) {
        String errorType;
        String errorMessage = e.getMessage();
        Integer httpCode = null;

        if (errorMessage != null) {
            if (errorMessage.contains("timeout") || errorMessage.contains("Timeout")) {
                errorType = ErrorType.TIMEOUT;
            } else if (errorMessage.contains("HTTP") || errorMessage.contains("code")) {
                errorType = ErrorType.HTTP_ERROR;
                // 尝试提取HTTP状态码
                httpCode = extractHttpCode(errorMessage);
            } else if (errorMessage.contains("network") || errorMessage.contains("connect")) {
                errorType = ErrorType.NETWORK_ERROR;
            } else {
                errorType = ErrorType.BUSINESS_ERROR;
            }
        } else {
            errorType = ErrorType.BUSINESS_ERROR;
        }

        context.markFailed(errorType, errorMessage, httpCode);
        log.warn("AI API调用失败: scene={}, errorType={}, message={}",
                context.getScene(), errorType, errorMessage);
    }

    /**
     * 处理通用异常
     */
    private void handleException(AiApiCallContext context, Exception e) {
        context.markFailed(ErrorType.BUSINESS_ERROR, e.getMessage(), null);
        log.warn("AI API调用异常: scene={}, message={}",
                context.getScene(), e.getMessage());
    }

    /**
     * 从错误消息中提取HTTP状态码
     */
    private Integer extractHttpCode(String errorMessage) {
        try {
            // 尝试匹配 "code: 500" 或 "500" 等格式
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\b(\\d{3})\\b");
            java.util.regex.Matcher matcher = pattern.matcher(errorMessage);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * API执行器接口
     */
    @FunctionalInterface
    public interface ApiExecutor {
        ApiResult execute() throws IOException;
    }

    /**
     * 无返回值API执行器接口
     */
    @FunctionalInterface
    public interface ApiExecutorVoid {
        void execute() throws IOException;
    }

    /**
     * API执行结果
     */
    public static class ApiResult {
        private final JSONObject response;
        private final Response okResponse;

        public ApiResult(JSONObject response, Response okResponse) {
            this.response = response;
            this.okResponse = okResponse;
        }

        public JSONObject getResponse() {
            return response;
        }

        public Response getOkResponse() {
            return okResponse;
        }

        public static ApiResult of(JSONObject response, Response okResponse) {
            return new ApiResult(response, okResponse);
        }
    }
}
