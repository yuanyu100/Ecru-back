package com.ecru.web.service;

import com.ecru.common.exception.BusinessException;
import com.ecru.web.dto.request.AdminKnowledgeListRequest;
import com.ecru.web.dto.request.CareLabelKnowledgeUpsertRequest;
import com.ecru.web.dto.request.FabricKnowledgeUpsertRequest;
import com.ecru.web.dto.request.GuideKnowledgeUpsertRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class KnowledgeAdminService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final JdbcTemplate jdbcTemplate;

    public KnowledgeAdminService(@Qualifier("mysqlDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public Map<String, Object> getOverview() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fabricTotal", queryCount("SELECT COUNT(*) FROM knowledge_fabrics"));
        result.put("fabricActive", queryCount("SELECT COUNT(*) FROM knowledge_fabrics WHERE is_active = 1"));
        result.put("guideTotal", queryCount("SELECT COUNT(*) FROM knowledge_guides"));
        result.put("guideActive", queryCount("SELECT COUNT(*) FROM knowledge_guides WHERE is_active = 1"));
        result.put("careLabelTotal", queryCount("SELECT COUNT(*) FROM knowledge_care_labels"));
        result.put("careLabelActive", queryCount("SELECT COUNT(*) FROM knowledge_care_labels WHERE is_active = 1"));
        return result;
    }

    public Map<String, Object> listFabrics(AdminKnowledgeListRequest request) {
        List<Object> whereArgs = new ArrayList<>();
        String where = buildWhereClause(
                request,
                whereArgs,
                List.of("name", "alias", "fabric_type", "summary", "properties", "keywords"));

        long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM knowledge_fabrics" + where,
                Long.class,
                whereArgs.toArray());

        int page = safePage(request.getPage());
        int size = safeSize(request.getSize());
        List<Object> queryArgs = new ArrayList<>(whereArgs);
        queryArgs.add(size);
        queryArgs.add((page - 1L) * size);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, name, alias, fabric_type, warmth_score, breathability_score, comfort_score, durability_score, " +
                        "summary, properties, care_guide, suitable_seasons, suitable_occasions, keywords, source, is_active, created_at, updated_at " +
                        "FROM knowledge_fabrics" + where + " ORDER BY updated_at DESC, id DESC LIMIT ? OFFSET ?",
                queryArgs.toArray());

        List<Map<String, Object>> list = rows.stream().map(this::toAdminFabricItem).toList();
        return buildPageResult(page, size, total, list);
    }

    public Map<String, Object> listGuides(AdminKnowledgeListRequest request) {
        List<Object> whereArgs = new ArrayList<>();
        String where = buildWhereClause(
                request,
                whereArgs,
                List.of("title", "subtitle", "guide_type", "summary", "content", "tags", "keywords"));

        long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM knowledge_guides" + where,
                Long.class,
                whereArgs.toArray());

        int page = safePage(request.getPage());
        int size = safeSize(request.getSize());
        List<Object> queryArgs = new ArrayList<>(whereArgs);
        queryArgs.add(size);
        queryArgs.add((page - 1L) * size);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, title, subtitle, guide_type, summary, content, author, publish_date, tags, cover_image_url, cover_image_caption, " +
                        "keywords, source, is_active, created_at, updated_at " +
                        "FROM knowledge_guides" + where + " ORDER BY updated_at DESC, id DESC LIMIT ? OFFSET ?",
                queryArgs.toArray());

        List<Map<String, Object>> list = rows.stream().map(this::toAdminGuideItem).toList();
        return buildPageResult(page, size, total, list);
    }

    public Map<String, Object> listCareLabels(AdminKnowledgeListRequest request) {
        List<Object> whereArgs = new ArrayList<>();
        String where = buildWhereClause(
                request,
                whereArgs,
                List.of("symbol_code", "symbol_name", "category", "instruction_text", "explanation", "keywords"));

        long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM knowledge_care_labels" + where,
                Long.class,
                whereArgs.toArray());

        int page = safePage(request.getPage());
        int size = safeSize(request.getSize());
        List<Object> queryArgs = new ArrayList<>(whereArgs);
        queryArgs.add(size);
        queryArgs.add((page - 1L) * size);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, symbol_code, symbol_name, category, instruction_text, explanation, do_text, dont_text, keywords, source, is_active, created_at, updated_at " +
                        "FROM knowledge_care_labels" + where + " ORDER BY updated_at DESC, id DESC LIMIT ? OFFSET ?",
                queryArgs.toArray());

        List<Map<String, Object>> list = rows.stream().map(this::toAdminCareLabelItem).toList();
        return buildPageResult(page, size, total, list);
    }

    public Map<String, Object> createFabric(FabricKnowledgeUpsertRequest request) {
        ensureUnique("knowledge_fabrics", "name", request.getName(), null);

        long id = nextId("knowledge_fabrics");
        jdbcTemplate.update(
                "INSERT INTO knowledge_fabrics (id, name, alias, fabric_type, warmth_score, breathability_score, comfort_score, durability_score, " +
                        "summary, properties, care_guide, suitable_seasons, suitable_occasions, keywords, source, is_active) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id,
                trim(request.getName()),
                trim(request.getAlias()),
                trim(request.getFabricType()),
                defaultScore(request.getWarmthScore()),
                defaultScore(request.getBreathabilityScore()),
                defaultScore(request.getComfortScore()),
                defaultScore(request.getDurabilityScore()),
                trim(request.getSummary()),
                trim(request.getProperties()),
                trim(request.getCareGuide()),
                trim(request.getSuitableSeasons()),
                trim(request.getSuitableOccasions()),
                trim(request.getKeywords()),
                defaultSource(request.getSource()),
                toActiveFlag(request.getActive()));
        return getFabricById(id);
    }

    public Map<String, Object> updateFabric(Long id, FabricKnowledgeUpsertRequest request) {
        requireExists("knowledge_fabrics", id, "面料知识不存在");
        ensureUnique("knowledge_fabrics", "name", request.getName(), id);

        jdbcTemplate.update(
                "UPDATE knowledge_fabrics SET name = ?, alias = ?, fabric_type = ?, warmth_score = ?, breathability_score = ?, comfort_score = ?, durability_score = ?, " +
                        "summary = ?, properties = ?, care_guide = ?, suitable_seasons = ?, suitable_occasions = ?, keywords = ?, source = ?, is_active = ? " +
                        "WHERE id = ?",
                trim(request.getName()),
                trim(request.getAlias()),
                trim(request.getFabricType()),
                defaultScore(request.getWarmthScore()),
                defaultScore(request.getBreathabilityScore()),
                defaultScore(request.getComfortScore()),
                defaultScore(request.getDurabilityScore()),
                trim(request.getSummary()),
                trim(request.getProperties()),
                trim(request.getCareGuide()),
                trim(request.getSuitableSeasons()),
                trim(request.getSuitableOccasions()),
                trim(request.getKeywords()),
                defaultSource(request.getSource()),
                toActiveFlag(request.getActive()),
                id);
        return getFabricById(id);
    }

    public Map<String, Object> createGuide(GuideKnowledgeUpsertRequest request) {
        ensureUnique("knowledge_guides", "title", request.getTitle(), null);

        long id = nextId("knowledge_guides");
        jdbcTemplate.update(
                "INSERT INTO knowledge_guides (id, title, subtitle, guide_type, summary, content, author, publish_date, tags, cover_image_url, cover_image_caption, keywords, source, is_active) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id,
                trim(request.getTitle()),
                trim(request.getSubtitle()),
                trim(request.getGuideType()),
                trim(request.getSummary()),
                trim(request.getContent()),
                trim(request.getAuthor()),
                parseDate(request.getPublishDate()),
                trim(request.getTags()),
                trim(request.getCoverImageUrl()),
                trim(request.getCoverImageCaption()),
                trim(request.getKeywords()),
                defaultSource(request.getSource()),
                toActiveFlag(request.getActive()));
        return getGuideById(id);
    }

    public Map<String, Object> updateGuide(Long id, GuideKnowledgeUpsertRequest request) {
        requireExists("knowledge_guides", id, "搭配指南不存在");
        ensureUnique("knowledge_guides", "title", request.getTitle(), id);

        jdbcTemplate.update(
                "UPDATE knowledge_guides SET title = ?, subtitle = ?, guide_type = ?, summary = ?, content = ?, author = ?, publish_date = ?, tags = ?, cover_image_url = ?, cover_image_caption = ?, keywords = ?, source = ?, is_active = ? " +
                        "WHERE id = ?",
                trim(request.getTitle()),
                trim(request.getSubtitle()),
                trim(request.getGuideType()),
                trim(request.getSummary()),
                trim(request.getContent()),
                trim(request.getAuthor()),
                parseDate(request.getPublishDate()),
                trim(request.getTags()),
                trim(request.getCoverImageUrl()),
                trim(request.getCoverImageCaption()),
                trim(request.getKeywords()),
                defaultSource(request.getSource()),
                toActiveFlag(request.getActive()),
                id);
        return getGuideById(id);
    }

    public Map<String, Object> createCareLabel(CareLabelKnowledgeUpsertRequest request) {
        ensureUnique("knowledge_care_labels", "symbol_code", request.getSymbolCode(), null);

        long id = nextId("knowledge_care_labels");
        jdbcTemplate.update(
                "INSERT INTO knowledge_care_labels (id, symbol_code, symbol_name, category, instruction_text, explanation, do_text, dont_text, keywords, source, is_active) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id,
                trim(request.getSymbolCode()),
                trim(request.getSymbolName()),
                trim(request.getCategory()),
                trim(request.getInstruction()),
                trim(request.getExplanation()),
                trim(request.getDoText()),
                trim(request.getDontText()),
                trim(request.getKeywords()),
                defaultSource(request.getSource()),
                toActiveFlag(request.getActive()));
        return getCareLabelById(id);
    }

    public Map<String, Object> updateCareLabel(Long id, CareLabelKnowledgeUpsertRequest request) {
        requireExists("knowledge_care_labels", id, "洗护标知识不存在");
        ensureUnique("knowledge_care_labels", "symbol_code", request.getSymbolCode(), id);

        jdbcTemplate.update(
                "UPDATE knowledge_care_labels SET symbol_code = ?, symbol_name = ?, category = ?, instruction_text = ?, explanation = ?, do_text = ?, dont_text = ?, keywords = ?, source = ?, is_active = ? " +
                        "WHERE id = ?",
                trim(request.getSymbolCode()),
                trim(request.getSymbolName()),
                trim(request.getCategory()),
                trim(request.getInstruction()),
                trim(request.getExplanation()),
                trim(request.getDoText()),
                trim(request.getDontText()),
                trim(request.getKeywords()),
                defaultSource(request.getSource()),
                toActiveFlag(request.getActive()),
                id);
        return getCareLabelById(id);
    }

    private Map<String, Object> getFabricById(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, name, alias, fabric_type, warmth_score, breathability_score, comfort_score, durability_score, " +
                        "summary, properties, care_guide, suitable_seasons, suitable_occasions, keywords, source, is_active, created_at, updated_at " +
                        "FROM knowledge_fabrics WHERE id = ?",
                id);
        if (rows.isEmpty()) {
            throw new BusinessException(404, "面料知识不存在");
        }
        return toAdminFabricItem(rows.get(0));
    }

    private Map<String, Object> getGuideById(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, title, subtitle, guide_type, summary, content, author, publish_date, tags, cover_image_url, cover_image_caption, " +
                        "keywords, source, is_active, created_at, updated_at FROM knowledge_guides WHERE id = ?",
                id);
        if (rows.isEmpty()) {
            throw new BusinessException(404, "搭配指南不存在");
        }
        return toAdminGuideItem(rows.get(0));
    }

    private Map<String, Object> getCareLabelById(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, symbol_code, symbol_name, category, instruction_text, explanation, do_text, dont_text, keywords, source, is_active, created_at, updated_at " +
                        "FROM knowledge_care_labels WHERE id = ?",
                id);
        if (rows.isEmpty()) {
            throw new BusinessException(404, "洗护标知识不存在");
        }
        return toAdminCareLabelItem(rows.get(0));
    }

    private String buildWhereClause(AdminKnowledgeListRequest request, List<Object> args, List<String> keywordFields) {
        List<String> conditions = new ArrayList<>();

        String keyword = StringUtils.trimToEmpty(request.getKeyword());
        if (StringUtils.isNotBlank(keyword)) {
            List<String> keywordConditions = new ArrayList<>();
            for (String field : keywordFields) {
                keywordConditions.add(field + " LIKE ?");
                args.add("%" + keyword + "%");
            }
            conditions.add("(" + String.join(" OR ", keywordConditions) + ")");
        }

        if (request.getActive() != null && (request.getActive() == 0 || request.getActive() == 1)) {
            conditions.add("is_active = ?");
            args.add(request.getActive());
        }

        return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    }

    private Map<String, Object> buildPageResult(int page, int size, long total, List<Map<String, Object>> list) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("page", page);
        result.put("size", size);
        result.put("total", total);
        result.put("list", list);
        return result;
    }

    private Map<String, Object> toAdminFabricItem(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fabricId", asLong(row.get("id")));
        result.put("name", row.get("name"));
        result.put("alias", asString(row.get("alias")));
        result.put("fabricType", row.get("fabric_type"));
        result.put("warmthScore", asInt(row.get("warmth_score")));
        result.put("breathabilityScore", asInt(row.get("breathability_score")));
        result.put("comfortScore", asInt(row.get("comfort_score")));
        result.put("durabilityScore", asInt(row.get("durability_score")));
        result.put("summary", row.get("summary"));
        result.put("properties", row.get("properties"));
        result.put("careGuide", row.get("care_guide"));
        result.put("suitableSeasons", asString(row.get("suitable_seasons")));
        result.put("suitableOccasions", asString(row.get("suitable_occasions")));
        result.put("keywords", asString(row.get("keywords")));
        result.put("source", asString(row.get("source")));
        result.put("isActive", asInt(row.get("is_active")) == 1);
        result.put("createdAt", row.get("created_at"));
        result.put("updatedAt", row.get("updated_at"));
        return result;
    }

    private Map<String, Object> toAdminGuideItem(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("guideId", asLong(row.get("id")));
        result.put("title", row.get("title"));
        result.put("subtitle", asString(row.get("subtitle")));
        result.put("guideType", asString(row.get("guide_type")));
        result.put("summary", asString(row.get("summary")));
        result.put("content", asString(row.get("content")));
        result.put("author", asString(row.get("author")));
        result.put("publishDate", row.get("publish_date"));
        result.put("tags", asString(row.get("tags")));
        result.put("coverImageUrl", asString(row.get("cover_image_url")));
        result.put("coverImageCaption", asString(row.get("cover_image_caption")));
        result.put("keywords", asString(row.get("keywords")));
        result.put("source", asString(row.get("source")));
        result.put("isActive", asInt(row.get("is_active")) == 1);
        result.put("createdAt", row.get("created_at"));
        result.put("updatedAt", row.get("updated_at"));
        return result;
    }

    private Map<String, Object> toAdminCareLabelItem(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("careLabelId", asLong(row.get("id")));
        result.put("symbolCode", row.get("symbol_code"));
        result.put("symbolName", row.get("symbol_name"));
        result.put("category", row.get("category"));
        result.put("instruction", row.get("instruction_text"));
        result.put("explanation", asString(row.get("explanation")));
        result.put("doText", asString(row.get("do_text")));
        result.put("dontText", asString(row.get("dont_text")));
        result.put("keywords", asString(row.get("keywords")));
        result.put("source", asString(row.get("source")));
        result.put("isActive", asInt(row.get("is_active")) == 1);
        result.put("createdAt", row.get("created_at"));
        result.put("updatedAt", row.get("updated_at"));
        return result;
    }

    private long nextId(String tableName) {
        Long next = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) + 1 FROM " + tableName, Long.class);
        return next == null ? 1L : next;
    }

    private long queryCount(String sql) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count == null ? 0L : count;
    }

    private void ensureUnique(String tableName, String columnName, String value, Long excludeId) {
        String trimmed = StringUtils.trimToEmpty(value);
        if (StringUtils.isBlank(trimmed)) {
            return;
        }

        String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE " + columnName + " = ?";
        List<Object> args = new ArrayList<>();
        args.add(trimmed);
        if (excludeId != null) {
            sql += " AND id <> ?";
            args.add(excludeId);
        }

        Long count = jdbcTemplate.queryForObject(sql, Long.class, args.toArray());
        if (count != null && count > 0) {
            throw new BusinessException(400, "存在重复的知识库标识，请修改后再保存");
        }
    }

    private void requireExists(String tableName, Long id, String message) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE id = ?",
                Long.class,
                id);
        if (count == null || count <= 0) {
            throw new BusinessException(404, message);
        }
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

    private int defaultScore(Integer value) {
        if (value == null) {
            return 0;
        }
        return Math.max(0, Math.min(value, 100));
    }

    private int toActiveFlag(Boolean active) {
        return Boolean.FALSE.equals(active) ? 0 : 1;
    }

    private String defaultSource(String source) {
        return StringUtils.defaultIfBlank(trim(source), "admin-console");
    }

    private String trim(String value) {
        return StringUtils.trimToNull(value);
    }

    private Date parseDate(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return Date.valueOf(LocalDate.parse(value));
        } catch (DateTimeParseException ex) {
            throw new BusinessException(400, "发布日期格式必须为 yyyy-MM-dd");
        }
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private int asInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }
}
