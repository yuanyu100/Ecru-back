package com.ecru.clothing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecru.clothing.entity.ClothingWearLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ClothingWearLogMapper extends BaseMapper<ClothingWearLog> {

    List<ClothingWearLog> selectRecentWearLogs(@Param("clothingId") Long clothingId,
                                               @Param("limit") Integer limit);

    Integer selectWearCountByPeriod(@Param("clothingId") Long clothingId,
                                    @Param("startDate") String startDate,
                                    @Param("endDate") String endDate);

}
