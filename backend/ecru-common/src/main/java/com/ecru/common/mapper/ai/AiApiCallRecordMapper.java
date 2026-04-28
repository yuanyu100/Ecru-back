package com.ecru.common.mapper.ai;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecru.common.entity.ai.AiApiCallRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI API调用记录Mapper
 */
@Mapper
public interface AiApiCallRecordMapper extends BaseMapper<AiApiCallRecord> {

    /**
     * 查询最近N条调用记录
     */
    List<AiApiCallRecord> selectRecentCalls(@Param("limit") int limit);

    /**
     * 按场景统计错误分布
     */
    List<Map<String, Object>> selectErrorDistribution(@Param("startTime") LocalDateTime startTime);

    /**
     * 查询今日统计
     */
    Map<String, Object> selectTodayStats(@Param("today") String today);

    /**
     * 查询指定时间之后的总览统计
     */
    Map<String, Object> selectSummarySince(@Param("startTime") LocalDateTime startTime);

    /**
     * 查询指定时间之后的按用户调用统计
     */
    List<Map<String, Object>> selectUserStatsSince(@Param("startTime") LocalDateTime startTime);
}
