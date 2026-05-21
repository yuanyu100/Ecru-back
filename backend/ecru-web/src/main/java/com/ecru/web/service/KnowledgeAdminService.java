package com.ecru.web.service;

import com.ecru.common.exception.BusinessException;
import com.ecru.web.dto.request.AdminKnowledgeListRequest;
import com.ecru.web.dto.request.CareLabelKnowledgeBatchImportRequest;
import com.ecru.web.dto.request.CareLabelKnowledgeUpsertRequest;
import com.ecru.web.dto.request.FabricKnowledgeBatchImportRequest;
import com.ecru.web.dto.request.FabricKnowledgeUpsertRequest;
import com.ecru.web.dto.request.GuideKnowledgeBatchImportRequest;
import com.ecru.web.dto.request.GuideKnowledgeUpsertRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
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

    @Transactional
    public Map<String, Object> importFabrics(FabricKnowledgeBatchImportRequest request) {
        List<FabricKnowledgeUpsertRequest> items = request == null ? List.of() : defaultList(request.getItems());
        if (items.isEmpty()) {
            throw new BusinessException(400, "导入数据不能为空");
        }

        int created = 0;
        int updated = 0;
        int skipped = 0;
        boolean updateExisting = !Boolean.FALSE.equals(request.getUpdateExisting());

        for (FabricKnowledgeUpsertRequest item : items) {
            Long existingId = findExistingId("knowledge_fabrics", "name", item.getName());
            if (existingId != null) {
                if (updateExisting) {
                    updateFabric(existingId, item);
                    updated++;
                } else {
                    skipped++;
                }
                continue;
            }

            createFabric(item);
            created++;
        }

        return buildImportResult(items.size(), created, updated, skipped);
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

    public void deleteFabric(Long id) {
        requireExists("knowledge_fabrics", id, "面料知识不存在");
        jdbcTemplate.update("DELETE FROM knowledge_fabrics WHERE id = ?", id);
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

    @Transactional
    public Map<String, Object> importGuides(GuideKnowledgeBatchImportRequest request) {
        List<GuideKnowledgeUpsertRequest> items = request == null ? List.of() : defaultList(request.getItems());
        if (items.isEmpty()) {
            throw new BusinessException(400, "导入数据不能为空");
        }

        int created = 0;
        int updated = 0;
        int skipped = 0;
        boolean updateExisting = !Boolean.FALSE.equals(request.getUpdateExisting());

        for (GuideKnowledgeUpsertRequest item : items) {
            Long existingId = findExistingId("knowledge_guides", "title", item.getTitle());
            if (existingId != null) {
                if (updateExisting) {
                    updateGuide(existingId, item);
                    updated++;
                } else {
                    skipped++;
                }
                continue;
            }

            createGuide(item);
            created++;
        }

        return buildImportResult(items.size(), created, updated, skipped);
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

    public void deleteGuide(Long id) {
        requireExists("knowledge_guides", id, "搭配指南不存在");
        jdbcTemplate.update("DELETE FROM knowledge_guides WHERE id = ?", id);
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

    @Transactional
    public Map<String, Object> importCareLabels(CareLabelKnowledgeBatchImportRequest request) {
        List<CareLabelKnowledgeUpsertRequest> items = request == null ? List.of() : defaultList(request.getItems());
        if (items.isEmpty()) {
            throw new BusinessException(400, "导入数据不能为空");
        }

        int created = 0;
        int updated = 0;
        int skipped = 0;
        boolean updateExisting = !Boolean.FALSE.equals(request.getUpdateExisting());

        for (CareLabelKnowledgeUpsertRequest item : items) {
            Long existingId = findExistingId("knowledge_care_labels", "symbol_code", item.getSymbolCode());
            if (existingId != null) {
                if (updateExisting) {
                    updateCareLabel(existingId, item);
                    updated++;
                } else {
                    skipped++;
                }
                continue;
            }

            createCareLabel(item);
            created++;
        }

        return buildImportResult(items.size(), created, updated, skipped);
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

    public void deleteCareLabel(Long id) {
        requireExists("knowledge_care_labels", id, "洗护标知识不存在");
        jdbcTemplate.update("DELETE FROM knowledge_care_labels WHERE id = ?", id);
    }

    @Transactional
    public Map<String, Object> importGuidesFromPdf(MultipartFile file, boolean updateExisting) {
        String originalFilename = StringUtils.defaultIfBlank(file.getOriginalFilename(), "未命名文档");
        String title = originalFilename.replaceAll("(?i)\\.pdf$", "").trim();
        if (title.isEmpty()) {
            title = "PDF导入-" + System.currentTimeMillis();
        }

        String content;
        try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            content = stripper.getText(doc).trim();
        } catch (IOException e) {
            log.error("PDF解析失败: {}", e.getMessage(), e);
            throw new BusinessException(400, "PDF文件解析失败: " + e.getMessage());
        }

        if (content.isEmpty()) {
            throw new BusinessException(400, "PDF文件内容为空，无法导入");
        }

        String summary = content.length() > 200 ? content.substring(0, 200) + "..." : content;
        String keywords = title.replaceAll("[\\s_\\-]+", ",");

        Long existingId = findExistingId("knowledge_guides", "title", title);
        int created = 0;
        int updated = 0;

        if (existingId != null) {
            if (updateExisting) {
                GuideKnowledgeUpsertRequest req = buildPdfGuideRequest(title, summary, content, keywords);
                updateGuide(existingId, req);
                updated = 1;
            }
        } else {
            GuideKnowledgeUpsertRequest req = buildPdfGuideRequest(title, summary, content, keywords);
            createGuide(req);
            created = 1;
        }

        Map<String, Object> result = buildImportResult(1, created, updated, existingId != null && !updateExisting ? 1 : 0);
        result.put("title", title);
        result.put("contentLength", content.length());
        return result;
    }

    private GuideKnowledgeUpsertRequest buildPdfGuideRequest(String title, String summary, String content, String keywords) {
        GuideKnowledgeUpsertRequest req = new GuideKnowledgeUpsertRequest();
        req.setTitle(title);
        req.setSummary(summary);
        req.setContent(content);
        req.setGuideType("PDF导入");
        req.setKeywords(keywords);
        req.setSource("pdf-import");
        req.setPublishDate(LocalDate.now().toString());
        req.setActive(true);
        return req;
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

    private Map<String, Object> buildImportResult(int total, int created, int updated, int skipped) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("created", created);
        result.put("updated", updated);
        result.put("skipped", skipped);
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
        result.put("isActive", asBooleanFlag(row.get("is_active")));
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
        result.put("isActive", asBooleanFlag(row.get("is_active")));
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
        result.put("isActive", asBooleanFlag(row.get("is_active")));
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

    private Long findExistingId(String tableName, String columnName, String value) {
        String trimmed = StringUtils.trimToEmpty(value);
        if (StringUtils.isBlank(trimmed)) {
            return null;
        }

        List<Long> ids = jdbcTemplate.query(
                "SELECT id FROM " + tableName + " WHERE " + columnName + " = ? LIMIT 1",
                (rs, rowNum) -> rs.getLong("id"),
                trimmed);
        return ids.isEmpty() ? null : ids.get(0);
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

    private <T> List<T> defaultList(List<T> items) {
        return items == null ? List.of() : items;
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
