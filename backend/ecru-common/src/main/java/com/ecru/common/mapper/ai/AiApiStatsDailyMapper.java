package com.ecru.common.mapper.ai;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecru.common.entity.ai.AiApiStatsDaily;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI API日级统计Mapper
 */
@Mapper
public interface AiApiStatsDailyMapper extends BaseMapper<AiApiStatsDaily> {

    /**
     * 查询日期范围内的统计
     */
    List<AiApiStatsDaily> selectByDateRange(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 查询近N天统计
     */
    List<AiApiStatsDaily> selectRecentDays(@Param("startDate") String startDate);

    /**
     * 按场景聚合统计
     */
    List<AiApiStatsDaily> selectSceneStats(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 按模型聚合统计
     */
    List<AiApiStatsDaily> selectModelStats(@Param("startDate") String startDate, @Param("endDate") String endDate);
}
