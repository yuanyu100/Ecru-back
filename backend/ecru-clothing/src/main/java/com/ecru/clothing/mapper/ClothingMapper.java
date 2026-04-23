package com.ecru.clothing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecru.clothing.entity.Clothing;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ClothingMapper extends BaseMapper<Clothing> {

    @Select("SELECT * FROM clothings WHERE id = #{id}")
    Clothing selectById(Long id);

    @Select("SELECT * FROM clothings WHERE (user_id = #{userId} OR #{userId} IS NULL) AND is_deleted = 0")
    List<Clothing> selectClothingList(Long userId, String category, String primaryColor, String material, 
                                     String styleTag, String occasionTag, String seasonTag, 
                                     String keyword, String sortBy, String sortOrder, 
                                     Integer minFrequency, Integer maxFrequency);

    @Select("SELECT COUNT(*) as totalClothings, COUNT(DISTINCT wear_log.clothing_id) as totalWorn, " +
            "MAX(wear_log.clothing_id) as mostWornClothingId, MAX(wear_log.wear_count) as mostWornCount " +
            "FROM clothings " +
            "LEFT JOIN (SELECT clothing_id, COUNT(*) as wear_count FROM clothing_wear_log " +
                      "WHERE user_id = #{userId} GROUP BY clothing_id) as wear_log " +
            "ON clothings.id = wear_log.clothing_id " +
            "WHERE clothings.user_id = #{userId} AND clothings.is_deleted = 0")
    Map<String, Object> selectClothingStatistics(Long userId, String period);

    @Select("SELECT category, COUNT(*) as count, " +
            "ROUND((COUNT(*) * 100.0) / (SELECT COUNT(*) FROM clothings WHERE user_id = #{userId} AND is_deleted = 0), 2) as percentage " +
            "FROM clothings " +
            "WHERE user_id = #{userId} AND is_deleted = 0 " +
            "GROUP BY category")
    List<Map<String, Object>> selectClothingCountByCategory(Long userId);

    @Select("SELECT primary_color as color, COUNT(*) as count, " +
            "ROUND((COUNT(*) * 100.0) / (SELECT COUNT(*) FROM clothings WHERE user_id = #{userId} AND is_deleted = 0), 2) as percentage " +
            "FROM clothings " +
            "WHERE user_id = #{userId} AND is_deleted = 0 " +
            "GROUP BY primary_color")
    List<Map<String, Object>> selectClothingCountByColor(Long userId);

    @Select("SELECT frequency_level as level, " +
            "CASE frequency_level " +
            "WHEN 1 THEN '很少' " +
            "WHEN 2 THEN '较少' " +
            "WHEN 3 THEN '一般' " +
            "WHEN 4 THEN '较多' " +
            "WHEN 5 THEN '经常' " +
            "ELSE '未知' " +
            "END as label, " +
            "COUNT(*) as count " +
            "FROM clothings " +
            "WHERE user_id = #{userId} AND is_deleted = 0 " +
            "GROUP BY frequency_level")
    List<Map<String, Object>> selectClothingCountByFrequency(Long userId);

    @Select("SELECT DATE_FORMAT(worn_at, '%Y-%m-%d') as date, COUNT(*) as count " +
            "FROM clothing_wear_log " +
            "WHERE user_id = #{userId} " +
            "GROUP BY DATE_FORMAT(worn_at, '%Y-%m-%d') " +
            "ORDER BY date")
    List<Map<String, Object>> selectWearTrend(Long userId, String period);

}
