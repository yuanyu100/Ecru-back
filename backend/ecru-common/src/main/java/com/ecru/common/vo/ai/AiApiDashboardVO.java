package com.ecru.common.vo.ai;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * AI API监控仪表盘视图对象
 */
@Data
public class AiApiDashboardVO {

    /**
     * 今日总调用次数
     */
    private Integer todayTotalCalls;

    /**
     * 今日成功次数
     */
    private Integer todaySuccessCalls;

    /**
     * 今日失败次数
     */
    private Integer todayFailedCalls;

    /**
     * 今日成功率
     */
    private BigDecimal todaySuccessRate;

    /**
     * 今日平均响应时间（毫秒）
     */
    private BigDecimal todayAvgResponseTime;

    /**
     * 今日总token消耗
     */
    private Integer todayTotalTokens;

    /**
     * 近24小时总调用次数
     */
    private Integer recent24hTotalCalls;

    /**
     * 近24小时成功次数
     */
    private Integer recent24hSuccessCalls;

    /**
     * 近24小时失败次数
     */
    private Integer recent24hFailedCalls;

    /**
     * 近24小时成功率
     */
    private BigDecimal recent24hSuccessRate;

    /**
     * 近24小时平均响应时间（毫秒）
     */
    private BigDecimal recent24hAvgResponseTime;

    /**
     * 近24小时总token消耗
     */
    private Integer recent24hTotalTokens;

    /**
     * 近7天调用趋势（按天）
     */
    private List<Map<String, Object>> weeklyTrend;

    /**
     * 近24小时调用趋势（按小时）
     */
    private List<Map<String, Object>> hourlyTrend;

    /**
     * 按场景统计
     */
    private List<Map<String, Object>> sceneStats;

    /**
     * 按模型统计
     */
    private List<Map<String, Object>> modelStats;

    /**
     * 最近调用记录
     */
    private List<AiApiCallRecordVO> recentCalls;

    /**
     * 错误类型分布
     */
    private List<Map<String, Object>> errorDistribution;

    /**
     * 按用户聚合的调用统计
     */
    private List<UserAiMonitorStatsVO> userStats;
}
