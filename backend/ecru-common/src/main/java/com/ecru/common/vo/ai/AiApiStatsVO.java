package com.ecru.common.vo.ai;

import lombok.Data;

import java.math.BigDecimal;

/**
 * AI API统计视图对象
 */
@Data
public class AiApiStatsVO {

    /**
     * 统计日期
     */
    private String statsDate;

    /**
     * 统计小时（小时级统计时有值）
     */
    private Integer statsHour;

    /**
     * 调用场景
     */
    private String scene;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 总调用次数
     */
    private Integer totalCalls;

    /**
     * 成功次数
     */
    private Integer successCalls;

    /**
     * 失败次数
     */
    private Integer failedCalls;

    /**
     * 成功率（百分比）
     */
    private BigDecimal successRate;

    /**
     * 平均响应时间（毫秒）
     */
    private BigDecimal avgResponseTime;

    /**
     * 最小响应时间（毫秒）
     */
    private Long minResponseTime;

    /**
     * 最大响应时间（毫秒）
     */
    private Long maxResponseTime;

    /**
     * P50响应时间（毫秒）
     */
    private BigDecimal p50ResponseTime;

    /**
     * P95响应时间（毫秒）
     */
    private BigDecimal p95ResponseTime;

    /**
     * P99响应时间（毫秒）
     */
    private BigDecimal p99ResponseTime;

    /**
     * 总输入token数
     */
    private Integer totalInputTokens;

    /**
     * 总输出token数
     */
    private Integer totalOutputTokens;

    /**
     * 总token数
     */
    private Integer totalTokens;
}
