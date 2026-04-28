package com.ecru.clothing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecru.clothing.dto.request.AdminClothingQueryRequest;
import com.ecru.clothing.dto.request.ClothingQueryRequest;
import com.ecru.clothing.dto.response.AdminClothingListVO;
import com.ecru.clothing.entity.Clothing;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ClothingMapper extends BaseMapper<Clothing> {

    List<Clothing> selectClothingList(@Param("userId") Long userId,
                                      @Param("request") ClothingQueryRequest request,
                                      @Param("offset") long offset,
                                      @Param("size") long size);

    long countClothingList(@Param("userId") Long userId,
                           @Param("request") ClothingQueryRequest request);

    List<AdminClothingListVO> selectAdminClothingList(@Param("request") AdminClothingQueryRequest request,
                                                      @Param("offset") long offset,
                                                      @Param("size") long size);

    long countAdminClothingList(@Param("request") AdminClothingQueryRequest request);

    Clothing selectPossibleDuplicate(@Param("userId") Long userId,
                                     @Param("purchaseLink") String purchaseLink,
                                     @Param("sourceOrderId") String sourceOrderId,
                                     @Param("imageUrl") String imageUrl);

    Map<String, Object> selectClothingStatistics(@Param("userId") Long userId, @Param("period") String period);

    List<Map<String, Object>> selectClothingCountByCategory(@Param("userId") Long userId);

    List<Map<String, Object>> selectClothingCountByColor(@Param("userId") Long userId);

    List<Map<String, Object>> selectClothingCountByFrequency(@Param("userId") Long userId);

    List<Map<String, Object>> selectWearTrend(@Param("userId") Long userId, @Param("period") String period);
}
