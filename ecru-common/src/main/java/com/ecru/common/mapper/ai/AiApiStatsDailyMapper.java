package com.ecru.common.mapper.ai;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecru.common.entity.ai.AiApiStatsDaily;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * AI API日级统计Mapper
 */
@Mapper
public interface AiApiStatsDailyMapper extends BaseMapper<AiApiStatsDaily> {

    /**
     * 查询日期范围内的统计
     */
    @Select("SELECT * FROM ai_api_stats_daily " +
            "WHERE stats_date >= #{startDate} AND stats_date <= #{endDate} " +
            "ORDER BY stats_date")
    List<AiApiStatsDaily> selectByDateRange(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 查询近N天统计
     */
    @Select("SELECT * FROM ai_api_stats_daily " +
            "WHERE stats_date >= #{startDate} " +
            "ORDER BY stats_date")
    List<AiApiStatsDaily> selectRecentDays(@Param("startDate") String startDate);

    /**
     * 按场景聚合统计
     */
    @Select("SELECT scene, " +
            "SUM(total_calls) as total_calls, " +
            "SUM(success_calls) as success_calls, " +
            "SUM(failed_calls) as failed_calls, " +
            "AVG(success_rate) as avg_success_rate, " +
            "AVG(avg_response_time) as avg_response_time " +
            "FROM ai_api_stats_daily " +
            "WHERE stats_date >= #{startDate} AND stats_date <= #{endDate} " +
            "GROUP BY scene")
    List<AiApiStatsDaily> selectSceneStats(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 按模型聚合统计
     */
    @Select("SELECT model, " +
            "SUM(total_calls) as total_calls, " +
            "SUM(success_calls) as success_calls, " +
            "SUM(failed_calls) as failed_calls, " +
            "AVG(success_rate) as avg_success_rate, " +
            "AVG(avg_response_time) as avg_response_time " +
            "FROM ai_api_stats_daily " +
            "WHERE stats_date >= #{startDate} AND stats_date <= #{endDate} " +
            "GROUP BY model")
    List<AiApiStatsDaily> selectModelStats(@Param("startDate") String startDate, @Param("endDate") String endDate);
}
