package com.ecru.common.service.ai;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecru.common.dto.ai.AiApiCallContext;
import com.ecru.common.entity.ai.AiApiCallRecord;
import com.ecru.common.entity.ai.AiApiStatsDaily;
import com.ecru.common.entity.ai.AiApiStatsHourly;
import com.ecru.common.mapper.ai.AiApiCallRecordMapper;
import com.ecru.common.mapper.ai.AiApiStatsDailyMapper;
import com.ecru.common.mapper.ai.AiApiStatsHourlyMapper;
import com.ecru.common.vo.ai.AiApiCallRecordVO;
import com.ecru.common.vo.ai.AiApiDashboardVO;
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
 * AI API monitor service.
 */
@Slf4j
@Service
public class AiApiMonitorService {

    private static final String RECENT_CALLS_KEY = "ai:monitor:recent_calls";
    private static final String REALTIME_STATS_KEY = "ai:monitor:realtime:";
    private static final int RECENT_CALLS_LIMIT = 100;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private AiApiCallRecordMapper callRecordMapper;

    @Autowired
    private AiApiStatsHourlyMapper hourlyStatsMapper;

    @Autowired
    private AiApiStatsDailyMapper dailyStatsMapper;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    /**
     * Record one API call asynchronously.
     */
    @Async("taskExecutor")
    public void recordApiCall(AiApiCallContext context) {
        try {
            AiApiCallRecord record = convertToRecord(context);
            callRecordMapper.insert(record);

            updateRealtimeStats(context);
            updateRecentCalls(record);

            log.debug("AI API call recorded: requestId={}, scene={}, status={}",
                    context.getRequestId(), context.getScene(), context.getStatus());
        } catch (Exception e) {
            log.error("Failed to record AI API call: {}", e.getMessage(), e);
        }
    }

    /**
     * Extract token usage from a model response payload.
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
            log.warn("Failed to extract token usage: {}", e.getMessage());
        }
    }

    /**
     * Build dashboard data directly from raw call records so the admin page
     * stays accurate even before scheduled aggregation has run.
     */
    public AiApiDashboardVO getDashboardData() {
        AiApiDashboardVO dashboard = new AiApiDashboardVO();
        LocalDate today = LocalDate.now();
        String todayText = today.format(DATE_FORMATTER);

        try {
            Map<String, Object> todayStats = callRecordMapper.selectTodayStats(todayText);
            if (todayStats != null) {
                dashboard.setTodayTotalCalls(getIntValue(todayStats.get("total_calls")));
                dashboard.setTodaySuccessCalls(getIntValue(todayStats.get("success_calls")));
                dashboard.setTodayFailedCalls(getIntValue(todayStats.get("failed_calls")));
                dashboard.setTodayAvgResponseTime(getBigDecimalValue(todayStats.get("avg_response_time")));
                dashboard.setTodayTotalTokens(getIntValue(todayStats.get("total_tokens")));
            } else {
                dashboard.setTodayTotalCalls(0);
                dashboard.setTodaySuccessCalls(0);
                dashboard.setTodayFailedCalls(0);
                dashboard.setTodayAvgResponseTime(BigDecimal.ZERO);
                dashboard.setTodayTotalTokens(0);
            }

            int totalCalls = dashboard.getTodayTotalCalls();
            if (totalCalls > 0) {
                dashboard.setTodaySuccessRate(
                        BigDecimal.valueOf(dashboard.getTodaySuccessCalls() * 100.0 / totalCalls)
                                .setScale(2, RoundingMode.HALF_UP)
                );
            } else {
                dashboard.setTodaySuccessRate(BigDecimal.ZERO);
            }

            LocalDate weekStart = today.minusDays(6);
            LocalDateTime weekStartTime = weekStart.atStartOfDay();
            LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
            List<AiApiCallRecord> lastSevenDaysRecords = loadRecordsBetween(weekStartTime, tomorrowStart);
            List<AiApiCallRecord> todayRecords = filterRecordsByDate(lastSevenDaysRecords, today);

            dashboard.setWeeklyTrend(buildDailyTrend(lastSevenDaysRecords, weekStart, today));
            dashboard.setHourlyTrend(buildHourlyTrend(todayRecords));
            dashboard.setSceneStats(buildDimensionStats(todayRecords, "scene"));
            dashboard.setModelStats(buildDimensionStats(todayRecords, "model"));
            dashboard.setRecentCalls(getRecentCalls(20));
            dashboard.setErrorDistribution(callRecordMapper.selectErrorDistribution(LocalDateTime.now().minusDays(1)));
        } catch (Exception e) {
            log.error("Failed to build AI monitor dashboard: {}", e.getMessage(), e);
        }

        return dashboard;
    }

    /**
     * Return recent call records, preferring Redis when available.
     */
    public List<AiApiCallRecordVO> getRecentCalls(int limit) {
        List<AiApiCallRecordVO> result = new ArrayList<>();

        try {
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

            List<AiApiCallRecord> records = callRecordMapper.selectRecentCalls(limit);
            for (AiApiCallRecord record : records) {
                result.add(convertToVO(record));
            }
        } catch (Exception e) {
            log.error("Failed to load recent AI API calls: {}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * Return realtime counters stored in Redis.
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
            log.error("Failed to load realtime AI monitor stats: {}", e.getMessage(), e);
        }

        return stats;
    }

    /**
     * Aggregate the previous hour into the hourly stats table.
     */
    public void aggregateHourlyStats() {
        try {
            LocalDateTime previousHour = LocalDateTime.now().minusHours(1);
            refreshHourlyStats(previousHour.toLocalDate(), previousHour.getHour());
            log.info("Hourly AI monitor aggregation completed: date={}, hour={}",
                    previousHour.toLocalDate().format(DATE_FORMATTER), previousHour.getHour());
        } catch (Exception e) {
            log.error("Failed to aggregate hourly AI monitor stats: {}", e.getMessage(), e);
        }
    }

    /**
     * Aggregate yesterday into the daily stats table.
     */
    public void aggregateDailyStats() {
        try {
            LocalDate targetDate = LocalDate.now().minusDays(1);
            refreshDailyStats(targetDate);
            log.info("Daily AI monitor aggregation completed: date={}", targetDate.format(DATE_FORMATTER));
        } catch (Exception e) {
            log.error("Failed to aggregate daily AI monitor stats: {}", e.getMessage(), e);
        }
    }

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

            redisTemplate.opsForHash().increment(key, "total_calls", 1);
            redisTemplate.opsForHash().increment(key, "total_response_time", responseTime);

            if (context.getStatus() == 1) {
                redisTemplate.opsForHash().increment(key, "success_calls", 1);
            } else {
                redisTemplate.opsForHash().increment(key, "failed_calls", 1);
                redisTemplate.opsForHash().increment(key, "error_" + normalizeDimensionValue(context.getErrorType()), 1);
            }

            if (context.getTotalTokens() != null) {
                redisTemplate.opsForHash().increment(key, "total_tokens", context.getTotalTokens());
            }

            redisTemplate.expire(key, 1, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to update realtime AI monitor stats: {}", e.getMessage());
        }
    }

    private void updateRecentCalls(AiApiCallRecord record) {
        if (redisTemplate == null) {
            return;
        }

        try {
            String json = redisTemplate.opsForValue().get(RECENT_CALLS_KEY);
            List<AiApiCallRecord> recentCalls = json != null
                    ? JSON.parseArray(json, AiApiCallRecord.class)
                    : new ArrayList<>();

            recentCalls.add(0, record);
            if (recentCalls.size() > RECENT_CALLS_LIMIT) {
                recentCalls = new ArrayList<>(recentCalls.subList(0, RECENT_CALLS_LIMIT));
            }

            redisTemplate.opsForValue().set(RECENT_CALLS_KEY, JSON.toJSONString(recentCalls), 1, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to update recent AI monitor calls: {}", e.getMessage());
        }
    }

    private String buildRealtimeKey(String scene, String model) {
        String today = LocalDate.now().format(DATE_FORMATTER);
        return REALTIME_STATS_KEY + today + ":" + normalizeDimensionValue(scene) + ":" + normalizeDimensionValue(model);
    }

    private void refreshHourlyStats(LocalDate date, int hour) {
        LocalDateTime start = date.atTime(hour, 0);
        LocalDateTime end = start.plusHours(1);
        List<AiApiCallRecord> records = loadRecordsBetween(start, end);
        Map<String, StatsAccumulator> grouped = groupBySceneAndModel(records);
        LocalDateTime now = LocalDateTime.now();
        String statsDate = date.format(DATE_FORMATTER);

        hourlyStatsMapper.delete(new LambdaQueryWrapper<AiApiStatsHourly>()
                .eq(AiApiStatsHourly::getStatsDate, statsDate)
                .eq(AiApiStatsHourly::getStatsHour, hour));

        for (StatsAccumulator accumulator : grouped.values()) {
            hourlyStatsMapper.insert(accumulator.toHourlyEntity(statsDate, hour, now));
        }
    }

    private void refreshDailyStats(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        List<AiApiCallRecord> records = loadRecordsBetween(start, end);
        Map<String, StatsAccumulator> grouped = groupBySceneAndModel(records);
        LocalDateTime now = LocalDateTime.now();
        String statsDate = date.format(DATE_FORMATTER);

        dailyStatsMapper.delete(new LambdaQueryWrapper<AiApiStatsDaily>()
                .eq(AiApiStatsDaily::getStatsDate, statsDate));

        for (StatsAccumulator accumulator : grouped.values()) {
            dailyStatsMapper.insert(accumulator.toDailyEntity(statsDate, now));
        }
    }

    private List<AiApiCallRecord> loadRecordsBetween(LocalDateTime start, LocalDateTime end) {
        return callRecordMapper.selectList(new LambdaQueryWrapper<AiApiCallRecord>()
                .ge(AiApiCallRecord::getCreatedAt, start)
                .lt(AiApiCallRecord::getCreatedAt, end)
                .orderByAsc(AiApiCallRecord::getCreatedAt));
    }

    private List<AiApiCallRecord> filterRecordsByDate(List<AiApiCallRecord> records, LocalDate date) {
        List<AiApiCallRecord> result = new ArrayList<>();
        for (AiApiCallRecord record : records) {
            if (record.getCreatedAt() != null && date.equals(record.getCreatedAt().toLocalDate())) {
                result.add(record);
            }
        }
        return result;
    }

    private List<Map<String, Object>> buildDailyTrend(List<AiApiCallRecord> records, LocalDate start, LocalDate end) {
        Map<String, StatsAccumulator> grouped = new HashMap<>();
        for (AiApiCallRecord record : records) {
            if (record.getCreatedAt() == null) {
                continue;
            }
            String key = record.getCreatedAt().toLocalDate().format(DATE_FORMATTER);
            grouped.computeIfAbsent(key, ignored -> new StatsAccumulator()).add(record);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            String key = cursor.format(DATE_FORMATTER);
            StatsAccumulator accumulator = grouped.get(key);
            result.add(buildTrendEntry("date", key, accumulator));
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    private List<Map<String, Object>> buildHourlyTrend(List<AiApiCallRecord> records) {
        Map<Integer, StatsAccumulator> grouped = new HashMap<>();
        for (AiApiCallRecord record : records) {
            if (record.getCreatedAt() == null) {
                continue;
            }
            int hour = record.getCreatedAt().getHour();
            grouped.computeIfAbsent(hour, ignored -> new StatsAccumulator()).add(record);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            result.add(buildTrendEntry("hour", hour, grouped.get(hour)));
        }
        return result;
    }

    private List<Map<String, Object>> buildDimensionStats(List<AiApiCallRecord> records, String dimension) {
        Map<String, StatsAccumulator> grouped = new HashMap<>();
        for (AiApiCallRecord record : records) {
            String key = "scene".equals(dimension) ? record.getScene() : record.getModel();
            grouped.computeIfAbsent(normalizeDimensionValue(key), ignored -> new StatsAccumulator()).add(record);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, StatsAccumulator> entry : grouped.entrySet()) {
            Map<String, Object> item = entry.getValue().toStatsMap();
            item.put(dimension, entry.getKey());
            result.add(item);
        }

        result.sort((left, right) -> Integer.compare(
                getIntValue(right.get("totalCalls")),
                getIntValue(left.get("totalCalls"))
        ));
        return result;
    }

    private Map<String, StatsAccumulator> groupBySceneAndModel(List<AiApiCallRecord> records) {
        Map<String, StatsAccumulator> grouped = new HashMap<>();
        for (AiApiCallRecord record : records) {
            String scene = normalizeDimensionValue(record.getScene());
            String model = normalizeDimensionValue(record.getModel());
            String key = scene + "||" + model;
            StatsAccumulator accumulator = grouped.computeIfAbsent(key, ignored -> new StatsAccumulator());
            accumulator.setScene(scene);
            accumulator.setModel(model);
            accumulator.add(record);
        }
        return grouped;
    }

    private Map<String, Object> buildTrendEntry(String keyName, Object keyValue, StatsAccumulator accumulator) {
        Map<String, Object> item = accumulator == null ? new StatsAccumulator().toStatsMap() : accumulator.toStatsMap();
        item.put(keyName, keyValue);
        return item;
    }

    private String normalizeDimensionValue(String value) {
        return value == null || value.isBlank() ? "UNKNOWN" : value;
    }

    private Integer getIntValue(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
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
        if (value instanceof BigDecimal decimal) {
            return decimal.setScale(2, RoundingMode.HALF_UP);
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        try {
            return new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal calculateSuccessRate(int successCalls, int totalCalls) {
        if (totalCalls <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(successCalls * 100.0 / totalCalls).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAverage(long total, int count) {
        if (count <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(total)
                .divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePercentile(List<Long> values, double percentile) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }

        List<Long> sorted = new ArrayList<>(values);
        sorted.sort(Long::compareTo);
        int index = (int) Math.ceil(percentile * sorted.size()) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));
        return BigDecimal.valueOf(sorted.get(index)).setScale(2, RoundingMode.HALF_UP);
    }

    private final class StatsAccumulator {

        private String scene;
        private String model;
        private int totalCalls;
        private int successCalls;
        private int failedCalls;
        private long totalResponseTime;
        private long minResponseTime = Long.MAX_VALUE;
        private long maxResponseTime = Long.MIN_VALUE;
        private int totalInputTokens;
        private int totalOutputTokens;
        private int totalTokens;
        private final List<Long> responseTimes = new ArrayList<>();

        void setScene(String scene) {
            this.scene = scene;
        }

        void setModel(String model) {
            this.model = model;
        }

        void add(AiApiCallRecord record) {
            totalCalls++;
            if (record.getStatus() != null && record.getStatus() == 1) {
                successCalls++;
            } else {
                failedCalls++;
            }

            long responseTime = record.getResponseTime() == null ? 0L : record.getResponseTime();
            totalResponseTime += responseTime;
            minResponseTime = Math.min(minResponseTime, responseTime);
            maxResponseTime = Math.max(maxResponseTime, responseTime);
            responseTimes.add(responseTime);

            totalInputTokens += getIntValue(record.getInputTokens());
            totalOutputTokens += getIntValue(record.getOutputTokens());
            totalTokens += getIntValue(record.getTotalTokens());
        }

        Map<String, Object> toStatsMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("totalCalls", totalCalls);
            map.put("successCalls", successCalls);
            map.put("failedCalls", failedCalls);
            map.put("successRate", calculateSuccessRate(successCalls, totalCalls));
            map.put("avgResponseTime", calculateAverage(totalResponseTime, totalCalls));
            map.put("minResponseTime", totalCalls > 0 ? minResponseTime : 0L);
            map.put("maxResponseTime", totalCalls > 0 ? maxResponseTime : 0L);
            map.put("p50ResponseTime", calculatePercentile(responseTimes, 0.50));
            map.put("p95ResponseTime", calculatePercentile(responseTimes, 0.95));
            map.put("p99ResponseTime", calculatePercentile(responseTimes, 0.99));
            map.put("totalInputTokens", totalInputTokens);
            map.put("totalOutputTokens", totalOutputTokens);
            map.put("totalTokens", totalTokens);
            return map;
        }

        AiApiStatsHourly toHourlyEntity(String statsDate, int statsHour, LocalDateTime now) {
            AiApiStatsHourly entity = new AiApiStatsHourly();
            entity.setStatsDate(statsDate);
            entity.setStatsHour(statsHour);
            entity.setScene(scene);
            entity.setModel(model);
            applyEntityMetrics(entity);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            return entity;
        }

        AiApiStatsDaily toDailyEntity(String statsDate, LocalDateTime now) {
            AiApiStatsDaily entity = new AiApiStatsDaily();
            entity.setStatsDate(statsDate);
            entity.setScene(scene);
            entity.setModel(model);
            applyEntityMetrics(entity);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            return entity;
        }

        private void applyEntityMetrics(AiApiStatsHourly entity) {
            entity.setTotalCalls(totalCalls);
            entity.setSuccessCalls(successCalls);
            entity.setFailedCalls(failedCalls);
            entity.setSuccessRate(calculateSuccessRate(successCalls, totalCalls));
            entity.setAvgResponseTime(calculateAverage(totalResponseTime, totalCalls));
            entity.setMinResponseTime(totalCalls > 0 ? minResponseTime : 0L);
            entity.setMaxResponseTime(totalCalls > 0 ? maxResponseTime : 0L);
            entity.setP50ResponseTime(calculatePercentile(responseTimes, 0.50));
            entity.setP95ResponseTime(calculatePercentile(responseTimes, 0.95));
            entity.setP99ResponseTime(calculatePercentile(responseTimes, 0.99));
            entity.setTotalInputTokens(totalInputTokens);
            entity.setTotalOutputTokens(totalOutputTokens);
            entity.setTotalTokens(totalTokens);
        }

        private void applyEntityMetrics(AiApiStatsDaily entity) {
            entity.setTotalCalls(totalCalls);
            entity.setSuccessCalls(successCalls);
            entity.setFailedCalls(failedCalls);
            entity.setSuccessRate(calculateSuccessRate(successCalls, totalCalls));
            entity.setAvgResponseTime(calculateAverage(totalResponseTime, totalCalls));
            entity.setMinResponseTime(totalCalls > 0 ? minResponseTime : 0L);
            entity.setMaxResponseTime(totalCalls > 0 ? maxResponseTime : 0L);
            entity.setP50ResponseTime(calculatePercentile(responseTimes, 0.50));
            entity.setP95ResponseTime(calculatePercentile(responseTimes, 0.95));
            entity.setP99ResponseTime(calculatePercentile(responseTimes, 0.99));
            entity.setTotalInputTokens(totalInputTokens);
            entity.setTotalOutputTokens(totalOutputTokens);
            entity.setTotalTokens(totalTokens);
        }
    }
}
