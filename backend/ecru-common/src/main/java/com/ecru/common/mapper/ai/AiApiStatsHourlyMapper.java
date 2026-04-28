package com.ecru.common.mapper.ai;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecru.common.entity.ai.AiApiStatsHourly;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI API小时级统计Mapper
 */
@Mapper
public interface AiApiStatsHourlyMapper extends BaseMapper<AiApiStatsHourly> {

    /**
     * 查询日期范围内的统计
     */
    List<AiApiStatsHourly> selectByDateRange(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 查询今日24小时趋势
     */
    List<AiApiStatsHourly> selectTodayHourly(@Param("today") String today);

    /**
     * 按场景聚合统计
     */
    List<AiApiStatsHourly> selectSceneStats(@Param("today") String today);

    /**
     * 按模型聚合统计
     */
    List<AiApiStatsHourly> selectModelStats(@Param("today") String today);
}
