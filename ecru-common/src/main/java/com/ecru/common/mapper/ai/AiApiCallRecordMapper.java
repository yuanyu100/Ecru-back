package com.ecru.common.mapper.ai;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecru.common.entity.ai.AiApiCallRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
    @Select("SELECT * FROM ai_api_call_record ORDER BY created_at DESC LIMIT #{limit}")
    List<AiApiCallRecord> selectRecentCalls(@Param("limit") int limit);

    /**
     * 按场景统计错误分布
     */
    @Select("SELECT error_type, COUNT(*) as count FROM ai_api_call_record " +
            "WHERE status = 0 AND created_at >= #{startTime} GROUP BY error_type")
    List<Map<String, Object>> selectErrorDistribution(@Param("startTime") LocalDateTime startTime);

    /**
     * 查询今日统计
     */
    @Select("SELECT " +
            "COUNT(*) as total_calls, " +
            "SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) as success_calls, " +
            "SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) as failed_calls, " +
            "AVG(response_time) as avg_response_time, " +
            "SUM(total_tokens) as total_tokens " +
            "FROM ai_api_call_record " +
            "WHERE create_date = #{today}")
    Map<String, Object> selectTodayStats(@Param("today") String today);
}
