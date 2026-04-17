package com.ecru.common.service.ai;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ecru.common.dto.ai.AiApiCallContext;
import com.ecru.common.entity.ai.AiApiCallRecord;
import com.ecru.common.entity.ai.AiApiStatsDaily;
import com.ecru.common.entity.ai.AiApiStatsHourly;
import com.ecru.common.mapper.ai.AiApiCallRecordMapper;
import com.ecru.common.mapper.ai.AiApiStatsDailyMapper;
import com.ecru.common.mapper.ai.AiApiStatsHourlyMapper;
import com.ecru.common.vo.ai.AiApiCallRecordVO;
import com.ecru.common.vo.ai.AiApiDashboardVO;
import com.ecru.common.vo.ai.AiApiStatsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * AI API监控服务
 * 负责记录调用日志、统计数据、提供监控查询
 */
@Slf4j
@Service
public class AiApiMonitorService {

    @Autowired
    private AiApiCallRecordMapper callRecordMapper;

    @Autowired
    private AiApiStatsHourlyMapper hourlyStatsMapper;

    @Autowired
    private AiApiStatsDailyMapper dailyStatsMapper;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private static final String RECENT_CALLS_KEY = "ai:monitor:recent_calls";
    private static final String REALTIME_STATS_KEY = "ai:monitor:realtime:";
    private static final int RECENT_CALLS_LIMIT = 100;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 异步记录API调用
     */
    @Async("taskExecutor")
    public void recordApiCall(AiApiCallContext context) {
        try {
            // 1. 保存调用记录到数据库
            AiApiCallRecord record = convertToRecord(context);
            callRecordMapper.insert(record);

            // 2. 更新Redis实时统计
            updateRealtimeStats(context);

            // 3. 更新最近调用列表（Redis）
            updateRecentCalls(record);

            log.debug("AI API调用记录已保存: requestId={}, scene={}, status={}",
                    context.getRequestId(), context.getScene(), context.getStatus());
        } catch (Exception e) {
            log.error("记录AI API调用失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 从API响应中提取token使用量
     */
    public void extractTokenUsage(AiApiCallContext context, JSONObject response) {
        try {
            if (response != null && response.containsKey("usage")) {
                JSONObject usage = response.getJSONObject("usage");
                context.setInputTokens(usage.getInteger("prompt_tokens"));
                context.setOutputTokens(usage.getInteger("completion_tokens"));
                context.setTotalTokens(usage.getInteger("total_tokens"));
            }
        } catch (Exception e) {
            log.warn("提取token使用量失败: {}", e.getMessage());
        }
    }

    /**
     * 获取仪表盘数据
     */
    public AiApiDashboardVO getDashboardData() {
        AiApiDashboardVO dashboard = new AiApiDashboardVO();
        String today = LocalDate.now().format(DATE_FORMATTER);

        try {
            // 1. 今日统计
            Map<String, Object> todayStats = callRecordMapper.selectTodayStats(today);
            if (todayStats != null) {
                dashboard.setTodayTotalCalls(getIntValue(todayStats.get("total_calls")));
                dashboard.setTodaySuccessCalls(getIntValue(todayStats.get("success_calls")));
                dashboard.setTodayFailedCalls(getIntValue(todayStats.get("failed_calls")));
                dashboard.setTodayAvgResponseTime(getBigDecimalValue(todayStats.get("avg_response_time")));
                dashboard.setTodayTotalTokens(getIntValue(todayStats.get("total_tokens")));

                // 计算成功率
                int total = dashboard.getTodayTotalCalls();
                int success = dashboard.getTodaySuccessCalls();
                if (total > 0) {
                    dashboard.setTodaySuccessRate(
                            BigDecimal.valueOf(success * 100.0 / total).setScale(2, RoundingMode.HALF_UP)
                    );
                } else {
                    dashboard.setTodaySuccessRate(BigDecimal.ZERO);
                }
            }

            // 2. 近7天趋势
            LocalDate sevenDaysAgo = LocalDate.now().minusDays(6);
            List<AiApiStatsDaily> weeklyStats = dailyStatsMapper.selectByDateRange(
                    sevenDaysAgo.format(DATE_FORMATTER), today);
            dashboard.setWeeklyTrend(convertToTrendList(weeklyStats, "daily"));

            // 3. 近24小时趋势
            List<AiApiStatsHourly> hourlyStats = hourlyStatsMapper.selectTodayHourly(today);
            dashboard.setHourlyTrend(convertToTrendList(hourlyStats, "hourly"));

            // 4. 按场景统计
            List<AiApiStatsHourly> sceneStats = hourlyStatsMapper.selectSceneStats(today);
            dashboard.setSceneStats(convertToStatsList(sceneStats, "scene"));

            // 5. 按模型统计
            List<AiApiStatsHourly> modelStats = hourlyStatsMapper.selectModelStats(today);
            dashboard.setModelStats(convertToStatsList(modelStats, "model"));

            // 6. 最近调用记录
            dashboard.setRecentCalls(getRecentCalls(20));

            // 7. 错误分布
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            dashboard.setErrorDistribution(callRecordMapper.selectErrorDistribution(yesterday));

        } catch (Exception e) {
            log.error("获取仪表盘数据失败: {}", e.getMessage(), e);
        }

        return dashboard;
    }

    /**
     * 获取最近调用记录
     */
    public List<AiApiCallRecordVO> getRecentCalls(int limit) {
        List<AiApiCallRecordVO> result = new ArrayList<>();

        try {
            // 优先从Redis获取
            if (redisTemplate != null) {
                String json = redisTemplate.opsForValue().get(RECENT_CALLS_KEY);
                if (json != null) {
                    List<AiApiCallRecord> records = JSON.parseArray(json, AiApiCallRecord.class);
                    int endIndex = Math.min(limit, records.size());
                    for (int i = 0; i < endIndex; i++) {
                        result.add(convertToVO(records.get(i)));
                    }
                    return result;
                }
            }

            // 从数据库获取
            List<AiApiCallRecord> records = callRecordMapper.selectRecentCalls(limit);
            for (AiApiCallRecord record : records) {
                result.add(convertToVO(record));
            }
        } catch (Exception e) {
            log.error("获取最近调用记录失败: {}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * 获取实时统计（从Redis）
     */
    public Map<String, Object> getRealtimeStats(String scene, String model) {
        Map<String, Object> stats = new HashMap<>();
        String key = buildRealtimeKey(scene, model);

        try {
            if (redisTemplate != null) {
                Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
                for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                    stats.put(entry.getKey().toString(), entry.getValue());
                }
            }
        } catch (Exception e) {
            log.error("获取实时统计失败: {}", e.getMessage(), e);
        }

        return stats;
    }

    /**
     * 聚合小时级统计（定时任务调用）
     */
    public void aggregateHourlyStats() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime oneHourAgo = now.minusHours(1);
            String statsDate = oneHourAgo.format(DATE_FORMATTER);
            int statsHour = oneHourAgo.getHour();

            // TODO: 实现小时级统计聚合逻辑
            log.info("聚合小时级统计: date={}, hour={}", statsDate, statsHour);

        } catch (Exception e) {
            log.error("聚合小时级统计失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 聚合日级统计（定时任务调用）
     */
    public void aggregateDailyStats() {
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            String statsDate = yesterday.format(DATE_FORMATTER);

            // TODO: 实现日级统计聚合逻辑
            log.info("聚合日级统计: date={}", statsDate);

        } catch (Exception e) {
            log.error("聚合日级统计失败: {}", e.getMessage(), e);
        }
    }

    // ==================== 私有方法 ====================

    private AiApiCallRecord convertToRecord(AiApiCallContext context) {
        AiApiCallRecord record = new AiApiCallRecord();
        record.setScene(context.getScene());
        record.setModel(context.getModel());
        record.setRequestId(context.getRequestId());
        record.setUserId(context.getUserId());
        record.setStatus(context.getStatus());
        record.setHttpCode(context.getHttpCode());
        record.setErrorType(context.getErrorType());
        record.setErrorMessage(context.getErrorMessage());
        record.setResponseTime(context.getResponseTime());
        record.setInputTokens(context.getInputTokens());
        record.setOutputTokens(context.getOutputTokens());
        record.setTotalTokens(context.getTotalTokens());
        record.setPromptLength(context.getPromptLength());
        record.setResponseLength(context.getResponseLength());
        record.setCreatedAt(LocalDateTime.now());
        record.setCreateDate(LocalDate.now().format(DATE_FORMATTER));
        return record;
    }

    private AiApiCallRecordVO convertToVO(AiApiCallRecord record) {
        AiApiCallRecordVO vo = new AiApiCallRecordVO();
        vo.setId(record.getId());
        vo.setScene(record.getScene());
        vo.setModel(record.getModel());
        vo.setRequestId(record.getRequestId());
        vo.setUserId(record.getUserId());
        vo.setStatus(record.getStatus());
        vo.setHttpCode(record.getHttpCode());
        vo.setErrorType(record.getErrorType());
        vo.setErrorMessage(record.getErrorMessage());
        vo.setResponseTime(record.getResponseTime());
        vo.setInputTokens(record.getInputTokens());
        vo.setOutputTokens(record.getOutputTokens());
        vo.setTotalTokens(record.getTotalTokens());
        vo.setPromptLength(record.getPromptLength());
        vo.setResponseLength(record.getResponseLength());
        vo.setCreatedAt(record.getCreatedAt());
        return vo;
    }

    private void updateRealtimeStats(AiApiCallContext context) {
        if (redisTemplate == null) {
            return;
        }

        try {
            String key = buildRealtimeKey(context.getScene(), context.getModel());
            long responseTime = context.getResponseTime();

            // 使用Redis Hash累加统计
            redisTemplate.opsForHash().increment(key, "total_calls", 1);
            redisTemplate.opsForHash().increment(key, "total_response_time", responseTime);

            if (context.getStatus() == 1) {
                redisTemplate.opsForHash().increment(key, "success_calls", 1);
            } else {
                redisTemplate.opsForHash().increment(key, "failed_calls", 1);
                redisTemplate.opsForHash().increment(key, "error_" + context.getErrorType(), 1);
            }

            if (context.getTotalTokens() != null) {
                redisTemplate.opsForHash().increment(key, "total_tokens", context.getTotalTokens());
            }

            // 设置过期时间（1小时）
            redisTemplate.expire(key, 1, TimeUnit.HOURS);

        } catch (Exception e) {
            log.warn("更新实时统计失败: {}", e.getMessage());
        }
    }

    private void updateRecentCalls(AiApiCallRecord record) {
        if (redisTemplate == null) {
            return;
        }

        try {
            // 获取现有列表
            String json = redisTemplate.opsForValue().get(RECENT_CALLS_KEY);
            List<AiApiCallRecord> recentCalls;
            if (json != null) {
                recentCalls = JSON.parseArray(json, AiApiCallRecord.class);
            } else {
                recentCalls = new ArrayList<>();
            }

            // 添加到列表头部
            recentCalls.add(0, record);

            // 限制列表长度
            if (recentCalls.size() > RECENT_CALLS_LIMIT) {
                recentCalls = recentCalls.subList(0, RECENT_CALLS_LIMIT);
            }

            // 保存回Redis
            redisTemplate.opsForValue().set(RECENT_CALLS_KEY, JSON.toJSONString(recentCalls), 1, TimeUnit.HOURS);

        } catch (Exception e) {
            log.warn("更新最近调用列表失败: {}", e.getMessage());
        }
    }

    private String buildRealtimeKey(String scene, String model) {
        String today = LocalDate.now().format(DATE_FORMATTER);
        return REALTIME_STATS_KEY + today + ":" + scene + ":" + model;
    }

    private List<Map<String, Object>> convertToTrendList(List<?> stats, String type) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object stat : stats) {
            Map<String, Object> map = new HashMap<>();
            if ("daily".equals(type) && stat instanceof AiApiStatsDaily) {
                AiApiStatsDaily daily = (AiApiStatsDaily) stat;
                map.put("date", daily.getStatsDate());
                map.put("totalCalls", daily.getTotalCalls());
                map.put("successRate", daily.getSuccessRate());
                map.put("avgResponseTime", daily.getAvgResponseTime());
                map.put("totalTokens", daily.getTotalTokens());
            } else if ("hourly".equals(type) && stat instanceof AiApiStatsHourly) {
                AiApiStatsHourly hourly = (AiApiStatsHourly) stat;
                map.put("hour", hourly.getStatsHour());
                map.put("totalCalls", hourly.getTotalCalls());
                map.put("successRate", hourly.getSuccessRate());
                map.put("avgResponseTime", hourly.getAvgResponseTime());
                map.put("totalTokens", hourly.getTotalTokens());
            }
            result.add(map);
        }
        return result;
    }

    private List<Map<String, Object>> convertToStatsList(List<AiApiStatsHourly> stats, String groupBy) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (AiApiStatsHourly stat : stats) {
            Map<String, Object> map = new HashMap<>();
            if ("scene".equals(groupBy)) {
                map.put("scene", stat.getScene());
            } else {
                map.put("model", stat.getModel());
            }
            map.put("totalCalls", stat.getTotalCalls());
            map.put("successCalls", stat.getSuccessCalls());
            map.put("failedCalls", stat.getFailedCalls());
            map.put("successRate", stat.getSuccessRate());
            map.put("avgResponseTime", stat.getAvgResponseTime());
            result.add(map);
        }
        return result;
    }

    private Integer getIntValue(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private BigDecimal getBigDecimalValue(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
