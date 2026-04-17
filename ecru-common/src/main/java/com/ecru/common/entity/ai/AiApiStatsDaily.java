package com.ecru.common.entity.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI API日级统计实体
 * 按天聚合的统计数据
 */
@Data
@TableName("ai_api_stats_daily")
public class AiApiStatsDaily {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 统计日期（yyyy-MM-dd）
     */
    private String statsDate;

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

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
