package com.ecru.common.dto.ai;

import lombok.Data;

import java.time.LocalDate;

/**
 * AI API统计查询请求
 */
@Data
public class AiApiStatsQueryRequest {

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    /**
     * 调用场景（可选）
     */
    private String scene;

    /**
     * 模型名称（可选）
     */
    private String model;

    /**
     * 统计类型：hourly/daily
     */
    private String statsType;
}
