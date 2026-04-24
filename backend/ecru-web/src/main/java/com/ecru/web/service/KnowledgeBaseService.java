package com.ecru.web.service;

import com.ecru.common.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class KnowledgeBaseService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 20;

    private final JdbcTemplate jdbcTemplate;

    public KnowledgeBaseService(@Qualifier("mysqlDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public Map<String, Object> search(String query, String type, Integer limit) {
        String trimmedQuery = StringUtils.trimToEmpty(query);
        if (StringUtils.isBlank(trimmedQuery)) {
            throw new BusinessException(400, "搜索关键词不能为空");
        }

        SearchType searchType = SearchType.from(type);
        int safeLimit = Math.max(1, Math.min(limit == null ? DEFAULT_LIMIT : limit, MAX_LIMIT));
        List<String> expandedTerms = expandTerms(trimmedQuery);
        List<Map<String, Object>> results = new ArrayList<>();

        if (searchType.includeFabric()) {
            List<Map<String, Object>> fabrics = jdbcTemplate.queryForList(
                    "SELECT id, name, alias, fabric_type, summary, properties, care_guide, suitable_seasons, suitable_occasions, keywords, source " +
                            "FROM knowledge_fabrics WHERE is_active = 1 ORDER BY updated_at DESC, id ASC");
            for (Map<String, Object> fabric : fabrics) {
                Map<String, Object> item = toFabricSearchItem(fabric, trimmedQuery, expandedTerms);
                if (item != null) {
                    results.add(item);
                }
            }
        }

        if (searchType.includeGuide()) {
            List<Map<String, Object>> guides = jdbcTemplate.queryForList(
                    "SELECT id, title, subtitle, guide_type, summary, content, tags, keywords, source " +
                            "FROM knowledge_guides WHERE is_active = 1 ORDER BY publish_date DESC, id ASC");
            for (Map<String, Object> guide : guides) {
                Map<String, Object> item = toGuideSearchItem(guide, trimmedQuery, expandedTerms);
                if (item != null) {
                    results.add(item);
                }
            }
        }

        if (searchType.includeCareLabel()) {
            List<Map<String, Object>> careLabels = jdbcTemplate.queryForList(
                    "SELECT id, symbol_code, symbol_name, category, instruction_text, explanation, do_text, dont_text, keywords, source " +
                            "FROM knowledge_care_labels WHERE is_active = 1 ORDER BY category ASC, id ASC");
            for (Map<String, Object> careLabel : careLabels) {
                Map<String, Object> item = toCareLabelSearchItem(careLabel, trimmedQuery, expandedTerms);
                if (item != null) {
                    results.add(item);
                }
            }
        }

        results.sort(Comparator
                .comparing((Map<String, Object> item) -> asInt(item.get("relevance")), Comparator.reverseOrder())
                .thenComparing(item -> StringUtils.defaultString((String) item.get("title")), String.CASE_INSENSITIVE_ORDER));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("total", results.size());
        response.put("query", trimmedQuery);
        response.put("type", searchType.apiValue());
        response.put("results", results.stream().limit(safeLimit).toList());
        return response;
    }

    public Map<String, Object> getFabricDetail(Long fabricId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, name, alias, fabric_type, warmth_score, breathability_score, comfort_score, durability_score, " +
                        "summary, properties, care_guide, suitable_seasons, suitable_occasions, keywords " +
                        "FROM knowledge_fabrics WHERE id = ? AND is_active = 1",
                fabricId);
        if (rows.isEmpty()) {
            throw new BusinessException(404, "面料知识不存在");
        }

        Map<String, Object> row = rows.get(0);
        Map<String, Object> characteristics = new LinkedHashMap<>();
        characteristics.put("warmth", asInt(row.get("warmth_score")));
        characteristics.put("breathability", asInt(row.get("breathability_score")));
        characteristics.put("comfort", asInt(row.get("comfort_score")));
        characteristics.put("durability", asInt(row.get("durability_score")));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fabricId", asLong(row.get("id")));
        result.put("name", row.get("name"));
        result.put("alias", row.get("alias"));
        result.put("type", row.get("fabric_type"));
        result.put("characteristics", characteristics);
        result.put("summary", row.get("summary"));
        result.put("properties", row.get("properties"));
        result.put("careGuide", row.get("care_guide"));
        result.put("suitableSeasons", splitCsv((String) row.get("suitable_seasons")));
        result.put("suitableOccasions", splitCsv((String) row.get("suitable_occasions")));
        result.put("keywords", splitCsv((String) row.get("keywords")));
        return result;
    }

    public Map<String, Object> getGuideDetail(Long guideId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, title, subtitle, guide_type, content, author, publish_date, tags, cover_image_url, cover_image_caption " +
                        "FROM knowledge_guides WHERE id = ? AND is_active = 1",
                guideId);
        if (rows.isEmpty()) {
            throw new BusinessException(404, "搭配指南不存在");
        }

        Map<String, Object> row = rows.get(0);
        Map<String, Object> image = new LinkedHashMap<>();
        image.put("url", row.get("cover_image_url"));
        image.put("caption", row.get("cover_image_caption"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("guideId", asLong(row.get("id")));
        result.put("title", row.get("title"));
        result.put("subtitle", row.get("subtitle"));
        result.put("guideType", row.get("guide_type"));
        result.put("content", row.get("content"));
        result.put("author", row.get("author"));
        result.put("publishDate", row.get("publish_date"));
        result.put("tags", splitCsv((String) row.get("tags")));
        result.put("images", StringUtils.isBlank((String) row.get("cover_image_url")) ? List.of() : List.of(image));
        return result;
    }

    public Map<String, Object> getCareLabelDetail(String symbolCode) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, symbol_code, symbol_name, category, instruction_text, explanation, do_text, dont_text, keywords " +
                        "FROM knowledge_care_labels WHERE symbol_code = ? AND is_active = 1",
                symbolCode);
        if (rows.isEmpty()) {
            throw new BusinessException(404, "水洗标知识不存在");
        }

        Map<String, Object> row = rows.get(0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("careLabelId", asLong(row.get("id")));
        result.put("symbolCode", row.get("symbol_code"));
        result.put("symbolName", row.get("symbol_name"));
        result.put("category", row.get("category"));
        result.put("instruction", row.get("instruction_text"));
        result.put("explanation", row.get("explanation"));
        result.put("doText", row.get("do_text"));
        result.put("dontText", row.get("dont_text"));
        result.put("keywords", splitCsv((String) row.get("keywords")));
        return result;
    }

    private Map<String, Object> toFabricSearchItem(Map<String, Object> fabric, String query, List<String> terms) {
        String title = asString(fabric.get("name"));
        String body = String.join(" ",
                asString(fabric.get("alias")),
                asString(fabric.get("fabric_type")),
                asString(fabric.get("summary")),
                asString(fabric.get("properties")),
                asString(fabric.get("care_guide")),
                asString(fabric.get("suitable_seasons")),
                asString(fabric.get("suitable_occasions")),
                asString(fabric.get("keywords")));
        int relevance = calculateRelevance(query, title, body, terms);
        if (relevance <= 0) {
            return null;
        }

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("documentId", "fabric-" + fabric.get("id"));
        item.put("title", title);
        item.put("type", "fabric");
        item.put("content", buildSnippet(asString(fabric.get("summary")), asString(fabric.get("properties"))));
        item.put("source", StringUtils.defaultIfBlank(asString(fabric.get("source")), "knowledge-fabric"));
        item.put("relevance", relevance);
        item.put("tags", mergeTags(splitCsv(asString(fabric.get("suitable_seasons"))), splitCsv(asString(fabric.get("suitable_occasions")))));
        return item;
    }

    private Map<String, Object> toGuideSearchItem(Map<String, Object> guide, String query, List<String> terms) {
        String title = asString(guide.get("title"));
        String body = String.join(" ",
                asString(guide.get("subtitle")),
                asString(guide.get("guide_type")),
                asString(guide.get("summary")),
                asString(guide.get("content")),
                asString(guide.get("tags")),
                asString(guide.get("keywords")));
        int relevance = calculateRelevance(query, title, body, terms);
        if (relevance <= 0) {
            return null;
        }

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("documentId", "guide-" + guide.get("id"));
        item.put("title", title);
        item.put("type", "guide");
        item.put("content", buildSnippet(asString(guide.get("summary")), asString(guide.get("content"))));
        item.put("source", StringUtils.defaultIfBlank(asString(guide.get("source")), "knowledge-guide"));
        item.put("relevance", relevance);
        item.put("tags", splitCsv(asString(guide.get("tags"))));
        return item;
    }

    private Map<String, Object> toCareLabelSearchItem(Map<String, Object> careLabel, String query, List<String> terms) {
        String title = asString(careLabel.get("symbol_name"));
        String body = String.join(" ",
                asString(careLabel.get("symbol_code")),
                asString(careLabel.get("category")),
                asString(careLabel.get("instruction_text")),
                asString(careLabel.get("explanation")),
                asString(careLabel.get("do_text")),
                asString(careLabel.get("dont_text")),
                asString(careLabel.get("keywords")));
        int relevance = calculateRelevance(query, title, body, terms);
        if (relevance <= 0) {
            return null;
        }

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("documentId", "care-label-" + careLabel.get("id"));
        item.put("title", title);
        item.put("type", "care-label");
        item.put("content", buildSnippet(asString(careLabel.get("instruction_text")), asString(careLabel.get("explanation"))));
        item.put("source", StringUtils.defaultIfBlank(asString(careLabel.get("source")), "knowledge-care-label"));
        item.put("relevance", relevance);
        item.put("tags", mergeTags(List.of(asString(careLabel.get("category"))), splitCsv(asString(careLabel.get("keywords")))));
        return item;
    }

    private int calculateRelevance(String query, String title, String body, List<String> terms) {
        String queryLower = safeLower(query);
        String titleLower = safeLower(title);
        String bodyLower = safeLower(body);
        int score = 0;
        int matchedTerms = 0;

        if (StringUtils.isNotBlank(queryLower)) {
            if (titleLower.contains(queryLower)) {
                score += 45;
            } else if (bodyLower.contains(queryLower)) {
                score += 20;
            }
        }

        for (String term : terms) {
            if (StringUtils.isBlank(term)) {
                continue;
            }
            if (titleLower.contains(term)) {
                score += 20;
                matchedTerms++;
            } else if (bodyLower.contains(term)) {
                score += 8;
                matchedTerms++;
            }
        }

        if (matchedTerms >= 3) {
            score += 10;
        } else if (matchedTerms == 2) {
            score += 5;
        }
        return Math.min(score, 99);
    }

    private List<String> expandTerms(String query) {
        Set<String> terms = new LinkedHashSet<>();
        terms.add(safeLower(query));
        for (String token : query.split("[\\s,，。；;/]+")) {
            if (StringUtils.isNotBlank(token)) {
                terms.add(safeLower(token));
            }
        }

        addIfMatched(terms, query, List.of("羊毛", "毛呢", "wool"), List.of("wool", "warm", "winter", "coat", "sweater"));
        addIfMatched(terms, query, List.of("棉", "纯棉", "cotton"), List.of("cotton", "shirt", "tshirt", "breathable", "daily"));
        addIfMatched(terms, query, List.of("亚麻", "linen"), List.of("linen", "summer", "cool", "breathable"));
        addIfMatched(terms, query, List.of("牛仔", "denim"), List.of("denim", "casual", "daily", "jacket", "pants"));
        addIfMatched(terms, query, List.of("真丝", "丝绸", "silk"), List.of("silk", "elegant", "dress", "formal"));
        addIfMatched(terms, query, List.of("通勤", "上班", "职场", "office", "commute"), List.of("commute", "office", "formal", "layering"));
        addIfMatched(terms, query, List.of("面试", "interview"), List.of("interview", "commute", "shirt", "formal"));
        addIfMatched(terms, query, List.of("冬天", "冬季", "保暖", "winter"), List.of("winter", "warm", "coat", "wool"));
        addIfMatched(terms, query, List.of("夏天", "夏季", "透气", "summer"), List.of("summer", "cool", "linen", "breathable"));
        addIfMatched(terms, query, List.of("搭配", "指南", "guide"), List.of("guide", "match", "pairing", "layering"));
        addIfMatched(terms, query, List.of("水洗标", "洗护", "洗标", "care label", "laundry"), List.of("care", "wash", "laundry", "label", "instruction"));
        addIfMatched(terms, query, List.of("机洗", "machine wash"), List.of("machine wash", "wash", "care"));
        addIfMatched(terms, query, List.of("手洗", "hand wash"), List.of("hand wash", "wash", "care"));
        addIfMatched(terms, query, List.of("不可漂白", "bleach"), List.of("bleach", "do not bleach", "care"));
        addIfMatched(terms, query, List.of("熨烫", "iron"), List.of("iron", "low heat", "care"));
        addIfMatched(terms, query, List.of("干洗", "dry clean"), List.of("dry clean", "care", "clean"));
        addIfMatched(terms, query, List.of("不可烘干", "tumble dry"), List.of("tumble dry", "do not tumble dry", "care"));

        return new ArrayList<>(terms);
    }

    private void addIfMatched(Set<String> terms, String query, List<String> triggers, List<String> expansions) {
        String loweredQuery = safeLower(query);
        for (String trigger : triggers) {
            if (loweredQuery.contains(safeLower(trigger))) {
                expansions.stream().map(this::safeLower).forEach(terms::add);
                return;
            }
        }
    }

    private List<String> splitCsv(String value) {
        if (StringUtils.isBlank(value)) {
            return List.of();
        }
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
    }

    private List<String> mergeTags(List<String> first, List<String> second) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        merged.addAll(first);
        merged.addAll(second);
        return new ArrayList<>(merged);
    }

    private String buildSnippet(String preferred, String fallback) {
        String text = StringUtils.defaultIfBlank(preferred, fallback);
        if (StringUtils.isBlank(text)) {
            return "";
        }
        return text.length() > 140 ? text.substring(0, 140) + "..." : text;
    }

    private String safeLower(String value) {
        return StringUtils.defaultString(value).toLowerCase(Locale.ROOT);
    }

    private String asString(Object value) {
        return value == null ? "" : value.toString();
    }

    private Integer asInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private enum SearchType {
        ALL,
        FABRIC,
        GUIDE,
        CARE_LABEL;

        static SearchType from(String type) {
            String normalized = StringUtils.trimToEmpty(type).toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "", "all" -> ALL;
                case "fabric", "material" -> FABRIC;
                case "guide", "guides", "style", "match" -> GUIDE;
                case "care", "care-label", "care_label", "label", "washing" -> CARE_LABEL;
                default -> throw new BusinessException(400, "不支持的知识库类型");
            };
        }

        boolean includeFabric() {
            return this == ALL || this == FABRIC;
        }

        boolean includeGuide() {
            return this == ALL || this == GUIDE;
        }

        boolean includeCareLabel() {
            return this == ALL || this == CARE_LABEL;
        }

        String apiValue() {
            return switch (this) {
                case ALL -> "all";
                case FABRIC -> "fabric";
                case GUIDE -> "guide";
                case CARE_LABEL -> "care-label";
            };
        }
    }
}
