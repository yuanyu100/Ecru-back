package com.ecru.outfit.service;

import com.ecru.common.exception.BusinessException;
import com.ecru.outfit.dto.request.AdminOutfitRecordQueryRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminOutfitRecordService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final JdbcTemplate jdbcTemplate;

    public AdminOutfitRecordService(@Qualifier("mysqlDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public Map<String, Object> getOverview() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("recordTotal", queryCount("SELECT COUNT(*) FROM outfit_advice_records WHERE is_deleted = 0"));
        result.put("favoriteTotal", queryCount("SELECT COUNT(*) FROM outfit_advice_records WHERE is_deleted = 0 AND is_favorite = 1"));
        result.put("feedbackTotal", queryCount("SELECT COUNT(*) FROM outfit_feedback"));
        result.put("wornTotal", queryCount("SELECT COUNT(*) FROM outfit_feedback WHERE is_worn = 1"));
        return result;
    }

    public Map<String, Object> listRecords(AdminOutfitRecordQueryRequest request) {
        int page = safePage(request.getPage());
        int size = safeSize(request.getSize());

        List<Object> whereArgs = new ArrayList<>();
        String where = buildWhereClause(request, whereArgs);

        long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outfit_advice_records r LEFT JOIN users u ON u.id = r.user_id" + where,
                Long.class,
                whereArgs.toArray());

        List<Object> queryArgs = new ArrayList<>(whereArgs);
        queryArgs.add(size);
        queryArgs.add((page - 1L) * size);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT r.id, r.user_id, r.input_type, r.input_description, r.location, r.temperature, r.weather_condition, r.season, r.time_of_day, " +
                        "r.detected_style, r.outfit_name, r.outfit_description, r.reasoning, r.fashion_suggestions, r.occasion, r.suitability_score, " +
                        "r.is_favorite, r.created_at, r.updated_at, " +
                        "u.username, u.nickname, u.email, " +
                        "(SELECT COUNT(*) FROM outfit_items i WHERE i.outfit_advice_id = r.id) AS item_count, " +
                        "(SELECT f.overall_rating FROM outfit_feedback f WHERE f.outfit_advice_id = r.id ORDER BY f.updated_at DESC LIMIT 1) AS overall_rating, " +
                        "(SELECT f.feedback_text FROM outfit_feedback f WHERE f.outfit_advice_id = r.id ORDER BY f.updated_at DESC LIMIT 1) AS feedback_text " +
                        "FROM outfit_advice_records r " +
                        "LEFT JOIN users u ON u.id = r.user_id " +
                        where +
                        " ORDER BY r.updated_at DESC, r.id DESC LIMIT ? OFFSET ?",
                queryArgs.toArray());

        List<Map<String, Object>> list = rows.stream().map(this::toRecordListItem).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("page", page);
        result.put("size", size);
        result.put("total", total);
        result.put("list", list);
        return result;
    }

    public Map<String, Object> getRecordDetail(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT r.id, r.user_id, r.input_type, r.input_image_url, r.input_description, r.location, r.temperature, r.weather_condition, r.season, r.time_of_day, " +
                        "r.detected_items, r.detected_style, r.color_analysis, r.outfit_name, r.outfit_description, r.reasoning, r.fashion_suggestions, " +
                        "r.purchase_recommendations, r.occasion, r.suitability_score, r.is_favorite, r.created_at, r.updated_at, " +
                        "u.username, u.nickname, u.email " +
                        "FROM outfit_advice_records r " +
                        "LEFT JOIN users u ON u.id = r.user_id " +
                        "WHERE r.id = ? AND r.is_deleted = 0",
                id);
        if (rows.isEmpty()) {
            throw new BusinessException(404, "穿搭记录不存在");
        }

        Map<String, Object> record = toRecordDetailItem(rows.get(0));
        List<Map<String, Object>> items = jdbcTemplate.queryForList(
                "SELECT id, clothing_id, item_name, item_category, item_color, item_image_url, is_recommended, reason, sort_order, created_at " +
                        "FROM outfit_items WHERE outfit_advice_id = ? ORDER BY sort_order ASC, id ASC",
                id);
        List<Map<String, Object>> feedbacks = jdbcTemplate.queryForList(
                "SELECT f.id, f.user_id, f.overall_rating, f.style_rating, f.practicality_rating, f.weather_rating, f.is_worn, f.worn_at, f.feedback_text, f.created_at, f.updated_at, " +
                        "u.username, u.nickname " +
                        "FROM outfit_feedback f " +
                        "LEFT JOIN users u ON u.id = f.user_id " +
                        "WHERE f.outfit_advice_id = ? ORDER BY f.updated_at DESC, f.id DESC",
                id);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("record", record);
        result.put("items", items.stream().map(this::toItemDetail).toList());
        result.put("feedbacks", feedbacks.stream().map(this::toFeedbackDetail).toList());
        return result;
    }

    public void deleteRecord(Long id) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outfit_advice_records WHERE id = ? AND is_deleted = 0",
                Long.class,
                id);
        if (count == null || count <= 0) {
            throw new BusinessException(404, "穿搭记录不存在");
        }

        jdbcTemplate.update("DELETE FROM outfit_feedback WHERE outfit_advice_id = ?", id);
        jdbcTemplate.update("DELETE FROM outfit_items WHERE outfit_advice_id = ?", id);
        jdbcTemplate.update("DELETE FROM outfit_advice_records WHERE id = ?", id);
    }

    private String buildWhereClause(AdminOutfitRecordQueryRequest request, List<Object> args) {
        List<String> conditions = new ArrayList<>();
        conditions.add("r.is_deleted = 0");

        String keyword = StringUtils.trimToEmpty(request.getKeyword());
        if (StringUtils.isNotBlank(keyword)) {
            conditions.add("(r.outfit_name LIKE ? OR r.outfit_description LIKE ? OR r.input_description LIKE ? OR r.reasoning LIKE ? OR r.fashion_suggestions LIKE ?)");
            for (int i = 0; i < 5; i++) {
                args.add("%" + keyword + "%");
            }
        }

        String ownerKeyword = StringUtils.trimToEmpty(request.getOwnerKeyword());
        if (StringUtils.isNotBlank(ownerKeyword)) {
            conditions.add("(u.username LIKE ? OR u.nickname LIKE ? OR u.email LIKE ?)");
            args.add("%" + ownerKeyword + "%");
            args.add("%" + ownerKeyword + "%");
            args.add("%" + ownerKeyword + "%");
        }

        String occasion = StringUtils.trimToEmpty(request.getOccasion());
        if (StringUtils.isNotBlank(occasion)) {
            conditions.add("r.occasion LIKE ?");
            args.add("%" + occasion + "%");
        }

        if (request.getFavorite() != null && (request.getFavorite() == 0 || request.getFavorite() == 1)) {
            conditions.add("r.is_favorite = ?");
            args.add(request.getFavorite());
        }

        return " WHERE " + String.join(" AND ", conditions);
    }

    private Map<String, Object> toRecordListItem(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", asLong(row.get("id")));
        result.put("userId", asLong(row.get("user_id")));
        result.put("inputType", asInt(row.get("input_type")));
        result.put("inputDescription", asString(row.get("input_description")));
        result.put("location", asString(row.get("location")));
        result.put("temperature", row.get("temperature"));
        result.put("weatherCondition", asString(row.get("weather_condition")));
        result.put("season", asString(row.get("season")));
        result.put("timeOfDay", asString(row.get("time_of_day")));
        result.put("detectedStyle", asString(row.get("detected_style")));
        result.put("outfitName", asString(row.get("outfit_name")));
        result.put("outfitDescription", asString(row.get("outfit_description")));
        result.put("reasoning", asString(row.get("reasoning")));
        result.put("fashionSuggestions", asString(row.get("fashion_suggestions")));
        result.put("occasion", asString(row.get("occasion")));
        result.put("suitabilityScore", row.get("suitability_score"));
        result.put("isFavorite", asBooleanFlag(row.get("is_favorite")));
        result.put("createdAt", row.get("created_at"));
        result.put("updatedAt", row.get("updated_at"));
        result.put("username", asString(row.get("username")));
        result.put("nickname", asString(row.get("nickname")));
        result.put("email", asString(row.get("email")));
        result.put("itemCount", asInt(row.get("item_count")));
        result.put("overallRating", row.get("overall_rating"));
        result.put("feedbackText", asString(row.get("feedback_text")));
        return result;
    }

    private Map<String, Object> toRecordDetailItem(Map<String, Object> row) {
        Map<String, Object> result = toRecordListItem(row);
        result.put("inputImageUrl", asString(row.get("input_image_url")));
        result.put("detectedItems", asString(row.get("detected_items")));
        result.put("colorAnalysis", asString(row.get("color_analysis")));
        result.put("purchaseRecommendations", asString(row.get("purchase_recommendations")));
        return result;
    }

    private Map<String, Object> toItemDetail(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", asLong(row.get("id")));
        result.put("clothingId", asLong(row.get("clothing_id")));
        result.put("itemName", asString(row.get("item_name")));
        result.put("itemCategory", asString(row.get("item_category")));
        result.put("itemColor", asString(row.get("item_color")));
        result.put("itemImageUrl", asString(row.get("item_image_url")));
        result.put("isRecommended", asBooleanFlag(row.get("is_recommended")));
        result.put("reason", asString(row.get("reason")));
        result.put("sortOrder", asInt(row.get("sort_order")));
        result.put("createdAt", row.get("created_at"));
        return result;
    }

    private Map<String, Object> toFeedbackDetail(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", asLong(row.get("id")));
        result.put("userId", asLong(row.get("user_id")));
        result.put("overallRating", row.get("overall_rating"));
        result.put("styleRating", row.get("style_rating"));
        result.put("practicalityRating", row.get("practicality_rating"));
        result.put("weatherRating", row.get("weather_rating"));
        result.put("isWorn", asBooleanFlag(row.get("is_worn")));
        result.put("wornAt", row.get("worn_at"));
        result.put("feedbackText", asString(row.get("feedback_text")));
        result.put("createdAt", row.get("created_at"));
        result.put("updatedAt", row.get("updated_at"));
        result.put("username", asString(row.get("username")));
        result.put("nickname", asString(row.get("nickname")));
        return result;
    }

    private long queryCount(String sql) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count == null ? 0L : count;
    }

    private int safePage(Integer page) {
        return page == null || page < 1 ? DEFAULT_PAGE : page;
    }

    private int safeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private int asInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private boolean asBooleanFlag(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() == 1;
        }
        if (value instanceof String text) {
            return "1".equals(text) || "true".equalsIgnoreCase(text);
        }
        return false;
    }

    private Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }
}
