package com.ecru.common.dto.ai;

import lombok.Builder;
import lombok.Data;

/**
 * AI API调用上下文
 * 用于在调用链中传递监控信息
 */
@Data
@Builder
public class AiApiCallContext {

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 调用场景
     */
    private String scene;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 提示词长度
     */
    private Integer promptLength;

    /**
     * 开始时间（毫秒时间戳）
     */
    private Long startTime;

    /**
     * 输入token数（从响应中获取）
     */
    private Integer inputTokens;

    /**
     * 输出token数（从响应中获取）
     */
    private Integer outputTokens;

    /**
     * 总token数
     */
    private Integer totalTokens;

    /**
     * 响应长度
     */
    private Integer responseLength;

    /**
     * HTTP状态码
     */
    private Integer httpCode;

    /**
     * 调用状态：0-失败，1-成功
     */
    private Integer status;

    /**
     * 错误类型
     */
    private String errorType;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建上下文
     */
    public static AiApiCallContext create(String scene, String model, Long userId) {
        return AiApiCallContext.builder()
                .requestId(generateRequestId())
                .scene(scene)
                .model(model)
                .userId(userId)
                .startTime(System.currentTimeMillis())
                .status(1)
                .build();
    }

    /**
     * 生成请求ID
     */
    private static String generateRequestId() {
        return "ai-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 10000);
    }

    /**
     * 计算响应时间
     */
    public long getResponseTime() {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * 标记成功
     */
    public void markSuccess(Integer httpCode) {
        this.status = 1;
        this.httpCode = httpCode;
    }

    /**
     * 标记失败
     */
    public void markFailed(String errorType, String errorMessage, Integer httpCode) {
        this.status = 0;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.httpCode = httpCode;
    }
}
