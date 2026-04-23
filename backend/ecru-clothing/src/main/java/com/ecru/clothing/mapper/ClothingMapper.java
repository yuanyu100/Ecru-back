package com.ecru.clothing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecru.clothing.dto.request.AdminClothingQueryRequest;
import com.ecru.clothing.dto.request.ClothingQueryRequest;
import com.ecru.clothing.dto.response.AdminClothingListVO;
import com.ecru.clothing.entity.Clothing;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ClothingMapper extends BaseMapper<Clothing> {

    @Select("SELECT * FROM clothings WHERE id = #{id}")
    Clothing selectById(Long id);

    @Select({
            "<script>",
            "SELECT * FROM clothings",
            "WHERE user_id = #{userId} AND is_deleted = 0",
            "<if test='request.category != null and request.category != \"\"'>",
            "AND category = #{request.category}",
            "</if>",
            "<if test='request.primaryColor != null and request.primaryColor != \"\"'>",
            "AND primary_color = #{request.primaryColor}",
            "</if>",
            "<if test='request.material != null and request.material != \"\"'>",
            "AND material = #{request.material}",
            "</if>",
            "<if test='request.styleTag != null and request.styleTag != \"\"'>",
            "AND style_tags LIKE CONCAT('%', #{request.styleTag}, '%')",
            "</if>",
            "<if test='request.occasionTag != null and request.occasionTag != \"\"'>",
            "AND occasion_tags LIKE CONCAT('%', #{request.occasionTag}, '%')",
            "</if>",
            "<if test='request.seasonTag != null and request.seasonTag != \"\"'>",
            "AND season_tags LIKE CONCAT('%', #{request.seasonTag}, '%')",
            "</if>",
            "<if test='request.keyword != null and request.keyword != \"\"'>",
            "AND (name LIKE CONCAT('%', #{request.keyword}, '%')",
            "OR category LIKE CONCAT('%', #{request.keyword}, '%')",
            "OR primary_color LIKE CONCAT('%', #{request.keyword}, '%')",
            "OR brand LIKE CONCAT('%', #{request.keyword}, '%'))",
            "</if>",
            "<if test='request.minFrequency != null'>",
            "AND frequency_level &gt;= #{request.minFrequency}",
            "</if>",
            "<if test='request.maxFrequency != null'>",
            "AND frequency_level &lt;= #{request.maxFrequency}",
            "</if>",
            "<choose>",
            "<when test='request.sortBy == \"wearCount\"'>ORDER BY wear_count</when>",
            "<when test='request.sortBy == \"frequencyLevel\"'>ORDER BY frequency_level</when>",
            "<otherwise>ORDER BY created_at</otherwise>",
            "</choose>",
            "<choose>",
            "<when test='request.sortOrder == \"asc\"'>ASC</when>",
            "<otherwise>DESC</otherwise>",
            "</choose>",
            "LIMIT #{size} OFFSET #{offset}",
            "</script>"
    })
    List<Clothing> selectClothingList(@Param("userId") Long userId,
                                      @Param("request") ClothingQueryRequest request,
                                      @Param("offset") long offset,
                                      @Param("size") long size);

    @Select({
            "<script>",
            "SELECT COUNT(*) FROM clothings",
            "WHERE user_id = #{userId} AND is_deleted = 0",
            "<if test='request.category != null and request.category != \"\"'>",
            "AND category = #{request.category}",
            "</if>",
            "<if test='request.primaryColor != null and request.primaryColor != \"\"'>",
            "AND primary_color = #{request.primaryColor}",
            "</if>",
            "<if test='request.material != null and request.material != \"\"'>",
            "AND material = #{request.material}",
            "</if>",
            "<if test='request.styleTag != null and request.styleTag != \"\"'>",
            "AND style_tags LIKE CONCAT('%', #{request.styleTag}, '%')",
            "</if>",
            "<if test='request.occasionTag != null and request.occasionTag != \"\"'>",
            "AND occasion_tags LIKE CONCAT('%', #{request.occasionTag}, '%')",
            "</if>",
            "<if test='request.seasonTag != null and request.seasonTag != \"\"'>",
            "AND season_tags LIKE CONCAT('%', #{request.seasonTag}, '%')",
            "</if>",
            "<if test='request.keyword != null and request.keyword != \"\"'>",
            "AND (name LIKE CONCAT('%', #{request.keyword}, '%')",
            "OR category LIKE CONCAT('%', #{request.keyword}, '%')",
            "OR primary_color LIKE CONCAT('%', #{request.keyword}, '%')",
            "OR brand LIKE CONCAT('%', #{request.keyword}, '%'))",
            "</if>",
            "<if test='request.minFrequency != null'>",
            "AND frequency_level &gt;= #{request.minFrequency}",
            "</if>",
            "<if test='request.maxFrequency != null'>",
            "AND frequency_level &lt;= #{request.maxFrequency}",
            "</if>",
            "</script>"
    })
    long countClothingList(@Param("userId") Long userId,
                           @Param("request") ClothingQueryRequest request);

    @Select({
            "<script>",
            "SELECT",
            "c.id, c.user_id AS userId, c.name, c.category, c.sub_category AS subCategory,",
            "c.primary_color AS primaryColor, c.material, c.style_tags AS styleTags,",
            "c.image_url AS imageUrl, c.source_type AS sourceType, c.wear_count AS wearCount,",
            "c.frequency_level AS frequencyLevel, c.created_at AS createdAt,",
            "u.username AS ownerUsername, u.nickname AS ownerNickname, u.email AS ownerEmail",
            "FROM clothings c",
            "LEFT JOIN users u ON u.id = c.user_id",
            "WHERE c.is_deleted = 0",
            "<if test='request.userId != null'>",
            "AND c.user_id = #{request.userId}",
            "</if>",
            "<if test='request.category != null and request.category != \"\"'>",
            "AND c.category = #{request.category}",
            "</if>",
            "<if test='request.primaryColor != null and request.primaryColor != \"\"'>",
            "AND c.primary_color = #{request.primaryColor}",
            "</if>",
            "<if test='request.sourceType != null and request.sourceType != \"\"'>",
            "AND c.source_type = #{request.sourceType}",
            "</if>",
            "<if test='request.keyword != null and request.keyword != \"\"'>",
            "AND (c.name LIKE CONCAT('%', #{request.keyword}, '%')",
            "OR c.category LIKE CONCAT('%', #{request.keyword}, '%')",
            "OR c.primary_color LIKE CONCAT('%', #{request.keyword}, '%')",
            "OR c.brand LIKE CONCAT('%', #{request.keyword}, '%'))",
            "</if>",
            "<if test='request.ownerKeyword != null and request.ownerKeyword != \"\"'>",
            "AND (u.username LIKE CONCAT('%', #{request.ownerKeyword}, '%')",
            "OR u.nickname LIKE CONCAT('%', #{request.ownerKeyword}, '%')",
            "OR u.email LIKE CONCAT('%', #{request.ownerKeyword}, '%'))",
            "</if>",
            "ORDER BY c.created_at DESC",
            "LIMIT #{size} OFFSET #{offset}",
            "</script>"
    })
    List<AdminClothingListVO> selectAdminClothingList(@Param("request") AdminClothingQueryRequest request,
                                                      @Param("offset") long offset,
                                                      @Param("size") long size);

    @Select({
            "<script>",
            "SELECT COUNT(*)",
            "FROM clothings c",
            "LEFT JOIN users u ON u.id = c.user_id",
            "WHERE c.is_deleted = 0",
            "<if test='request.userId != null'>",
            "AND c.user_id = #{request.userId}",
            "</if>",
            "<if test='request.category != null and request.category != \"\"'>",
            "AND c.category = #{request.category}",
            "</if>",
            "<if test='request.primaryColor != null and request.primaryColor != \"\"'>",
            "AND c.primary_color = #{request.primaryColor}",
            "</if>",
            "<if test='request.sourceType != null and request.sourceType != \"\"'>",
            "AND c.source_type = #{request.sourceType}",
            "</if>",
            "<if test='request.keyword != null and request.keyword != \"\"'>",
            "AND (c.name LIKE CONCAT('%', #{request.keyword}, '%')",
            "OR c.category LIKE CONCAT('%', #{request.keyword}, '%')",
            "OR c.primary_color LIKE CONCAT('%', #{request.keyword}, '%')",
            "OR c.brand LIKE CONCAT('%', #{request.keyword}, '%'))",
            "</if>",
            "<if test='request.ownerKeyword != null and request.ownerKeyword != \"\"'>",
            "AND (u.username LIKE CONCAT('%', #{request.ownerKeyword}, '%')",
            "OR u.nickname LIKE CONCAT('%', #{request.ownerKeyword}, '%')",
            "OR u.email LIKE CONCAT('%', #{request.ownerKeyword}, '%'))",
            "</if>",
            "</script>"
    })
    long countAdminClothingList(@Param("request") AdminClothingQueryRequest request);

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
