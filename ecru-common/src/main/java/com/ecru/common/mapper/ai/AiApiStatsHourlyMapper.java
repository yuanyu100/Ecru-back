package com.ecru.common.mapper.ai;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecru.common.entity.ai.AiApiStatsHourly;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * AI API小时级统计Mapper
 */
@Mapper
public interface AiApiStatsHourlyMapper extends BaseMapper<AiApiStatsHourly> {

    /**
     * 查询日期范围内的统计
     */
    @Select("SELECT * FROM ai_api_stats_hourly " +
            "WHERE stats_date >= #{startDate} AND stats_date <= #{endDate} " +
            "ORDER BY stats_date, stats_hour")
    List<AiApiStatsHourly> selectByDateRange(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 查询今日24小时趋势
     */
    @Select("SELECT * FROM ai_api_stats_hourly " +
            "WHERE stats_date = #{today} " +
            "ORDER BY stats_hour")
    List<AiApiStatsHourly> selectTodayHourly(@Param("today") String today);

    /**
     * 按场景聚合统计
     */
    @Select("SELECT scene, " +
            "SUM(total_calls) as total_calls, " +
            "SUM(success_calls) as success_calls, " +
            "SUM(failed_calls) as failed_calls, " +
            "AVG(success_rate) as avg_success_rate, " +
            "AVG(avg_response_time) as avg_response_time " +
            "FROM ai_api_stats_hourly " +
            "WHERE stats_date = #{today} " +
            "GROUP BY scene")
    List<AiApiStatsHourly> selectSceneStats(@Param("today") String today);

    /**
     * 按模型聚合统计
     */
    @Select("SELECT model, " +
            "SUM(total_calls) as total_calls, " +
            "SUM(success_calls) as success_calls, " +
            "SUM(failed_calls) as failed_calls, " +
            "AVG(success_rate) as avg_success_rate, " +
            "AVG(avg_response_time) as avg_response_time " +
            "FROM ai_api_stats_hourly " +
            "WHERE stats_date = #{today} " +
            "GROUP BY model")
    List<AiApiStatsHourly> selectModelStats(@Param("today") String today);
}
