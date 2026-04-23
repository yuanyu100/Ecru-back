package com.ecru.clothing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecru.clothing.entity.ClothingWearLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ClothingWearLogMapper extends BaseMapper<ClothingWearLog> {

    @Select("SELECT * FROM clothing_wear_logs WHERE clothing_id = #{clothingId} ORDER BY worn_at DESC LIMIT #{limit}")
    List<ClothingWearLog> selectRecentWearLogs(
            @Param("clothingId") Long clothingId,
            @Param("limit") Integer limit
    );

    @Select("SELECT COUNT(*) FROM clothing_wear_logs WHERE clothing_id = #{clothingId} AND worn_at BETWEEN #{startDate} AND #{endDate}")
    Integer selectWearCountByPeriod(
            @Param("clothingId") Long clothingId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );

}
