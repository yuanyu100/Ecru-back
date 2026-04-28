package com.ecru.common.vo.ai;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 按用户聚合的 AI 调用监控视图对象
 */
@Data
public class UserAiMonitorStatsVO {

    private Long userId;

    private String username;

    private String nickname;

    private String role;

    private Integer totalCalls;

    private Integer successCalls;

    private Integer failedCalls;

    private BigDecimal successRate;

    private BigDecimal avgResponseTime;

    private Integer totalTokens;

    private LocalDateTime lastCallAt;
}
