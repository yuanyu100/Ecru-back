package com.ecru.common.entity.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI API调用记录实体
 * 记录每次AI API调用的详细信息
 */
@Data
@TableName("ai_api_call_record")
public class AiApiCallRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 调用场景/接口类型
     */
    private String scene;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 请求ID（用于链路追踪）
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
     * 错误类型：timeout/parse_error/http_error/business_error
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
     * 提示词长度（字符数）
     */
    private Integer promptLength;

    /**
     * 响应长度（字符数）
     */
    private Integer responseLength;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 创建日期（用于分区）
     */
    private String createDate;
}
