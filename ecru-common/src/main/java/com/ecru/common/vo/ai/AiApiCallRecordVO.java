package com.ecru.common.vo.ai;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI API调用记录视图对象
 */
@Data
public class AiApiCallRecordVO {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 调用场景
     */
    private String scene;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 调用状态：0-失败，1-成功
     */
    private Integer status;

    /**
     * HTTP状态码
     */
    private Integer httpCode;

    /**
     * 错误类型
     */
    private String errorType;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 响应时间（毫秒）
     */
    private Long responseTime;

    /**
     * 输入token数
     */
    private Integer inputTokens;

    /**
     * 输出token数
     */
    private Integer outputTokens;

    /**
     * 总token数
     */
    private Integer totalTokens;

    /**
     * 提示词长度
     */
    private Integer promptLength;

    /**
     * 响应长度
     */
    private Integer responseLength;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
