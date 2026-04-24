package com.ecru.common.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for AI monitor aggregation jobs.
 */
@Slf4j
@Component
public class AiApiMonitorAggregationScheduler {

    @Autowired
    private AiApiMonitorService monitorService;

    /**
     * Aggregate the previous hour a few minutes after the hour flips.
     */
    @Scheduled(cron = "0 5 * * * ?")
    public void aggregateHourlyStats() {
        log.debug("Starting scheduled hourly AI monitor aggregation");
        monitorService.aggregateHourlyStats();
    }

    /**
     * Aggregate yesterday shortly after midnight.
     */
    @Scheduled(cron = "0 10 0 * * ?")
    public void aggregateDailyStats() {
        log.debug("Starting scheduled daily AI monitor aggregation");
        monitorService.aggregateDailyStats();
    }
}
