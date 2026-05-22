package com.ecru.web.service;

import com.ecru.common.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

@Service
public class KnowledgeBaseService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int DEFAULT_MATCH_LIMIT = 5;
    private static final int MAX_LIMIT = 20;

    private final JdbcTemplate jdbcTemplate;
    private final KnowledgeVectorSyncService knowledgeVectorSyncService;
    private final boolean knowledgeVectorEnabled;
    private final int vectorScoreBoost;

    public KnowledgeBaseService(@Qualifier("mysqlDataSource") DataSource dataSource,
                                KnowledgeVectorSyncService knowledgeVectorSyncService,
                                @Value("${knowledge.search.vector-enabled:true}") boolean knowledgeVectorEnabled,
                                @Value("${knowledge.search.vector-score-boost:12}") int vectorScoreBoost) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.knowledgeVectorSyncService = knowledgeVectorSyncService;
        this.knowledgeVectorEnabled = knowledgeVectorEnabled;
        this.vectorScoreBoost = vectorScoreBoost;
    }

    public Map<String, Object> search(String query, String type, Integer limit) {
        String trimmedQuery = StringUtils.trimToEmpty(query);
        if (StringUtils.isBlank(trimmedQuery)) {
            throw new BusinessException(400, "搜索关键词不能为空");
        }

        SearchType searchType = SearchType.from(type);
        int safeLimit = clampLimit(limit, DEFAULT_LIMIT);
        List<String> expandedTerms = expandTerms(trimmedQuery);
        Map<String, Map<String, Object>> mergedResults = new LinkedHashMap<>();

        // 检索结果会融合“向量召回”和“文本匹配”两条链路：
        // 一个偏语义相近，一个偏关键词精确命中。
        if (knowledgeVectorEnabled) {
            for (Map<String, Object> vectorItem : searchByVector(trimmedQuery, searchType, safeLimit)) {
                mergeSearchResult(mergedResults, vectorItem, true);
            }
        }

        for (Map<String, Object> textItem : searchByText(trimmedQuery, searchType, expandedTerms)) {
            mergeSearchResult(mergedResults, textItem, false);
        }

        List<Map<String, Object>> results = new ArrayList<>(mergedResults.values());
        results.sort(searchItemComparator());

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
        return toFabricDetail(rows.get(0));
    }

    public Map<String, Object> getGuideDetail(Long guideId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, title, subtitle, guide_type, content, author, publish_date, tags, cover_image_url, cover_image_caption " +
                        "FROM knowledge_guides WHERE id = ? AND is_active = 1",
                guideId);
        if (rows.isEmpty()) {
            throw new BusinessException(404, "指南知识不存在");
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
        result.put("tags", splitCsv(asString(row.get("tags"))));
        result.put("images", StringUtils.isBlank(asString(row.get("cover_image_url"))) ? List.of() : List.of(image));
        return result;
    }

    public Map<String, Object> getCareLabelDetail(String symbolCode) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, symbol_code, symbol_name, category, instruction_text, explanation, do_text, dont_text, keywords " +
                        "FROM knowledge_care_labels WHERE symbol_code = ? AND is_active = 1",
                symbolCode);
        if (rows.isEmpty()) {
            throw new BusinessException(404, "洗护标识不存在");
        }
        return toCareLabelDetail(rows.get(0), 0);
    }

    public List<Map<String, Object>> matchFabrics(List<String> keywords, Integer limit) {
        // 面料匹配是给材质问答服务的轻量召回接口，不走完整搜索结果结构。
        List<String> normalizedTerms = normalizeKeywords(keywords);
        if (normalizedTerms.isEmpty()) {
            return List.of();
        }

        String combinedQuery = String.join(" ", keywords);
        List<Map<String, Object>> fabrics = jdbcTemplate.queryForList(
                "SELECT id, name, alias, fabric_type, warmth_score, breathability_score, comfort_score, durability_score, " +
                        "summary, properties, care_guide, suitable_seasons, suitable_occasions, keywords " +
                        "FROM knowledge_fabrics WHERE is_active = 1 ORDER BY updated_at DESC, id ASC");

        List<Map<String, Object>> matched = new ArrayList<>();
        for (Map<String, Object> fabric : fabrics) {
            Map<String, Object> item = toMatchedFabricItem(fabric, combinedQuery, normalizedTerms);
            if (item != null) {
                matched.add(item);
            }
        }

        matched.sort(matchItemComparator("name"));
        return matched.stream().limit(clampLimit(limit, DEFAULT_MATCH_LIMIT)).toList();
    }

    public List<Map<String, Object>> matchCareLabels(List<String> keywords, Integer limit) {
        // 洗护标识单独匹配，便于后续回答时把“材质知识”和“护理动作”拆开解释。
        List<String> normalizedTerms = normalizeKeywords(keywords);
        if (normalizedTerms.isEmpty()) {
            return List.of();
        }

        String combinedQuery = String.join(" ", keywords);
        List<Map<String, Object>> careLabels = jdbcTemplate.queryForList(
                "SELECT id, symbol_code, symbol_name, category, instruction_text, explanation, do_text, dont_text, keywords " +
                        "FROM knowledge_care_labels WHERE is_active = 1 ORDER BY category ASC, id ASC");

        List<Map<String, Object>> matched = new ArrayList<>();
        for (Map<String, Object> careLabel : careLabels) {
            Map<String, Object> item = toMatchedCareLabelItem(careLabel, combinedQuery, normalizedTerms);
            if (item != null) {
                matched.add(item);
            }
        }

        matched.sort(matchItemComparator("symbolName"));
        return matched.stream().limit(clampLimit(limit, DEFAULT_MATCH_LIMIT)).toList();
    }

    private List<Map<String, Object>> searchByVector(String query, SearchType searchType, int limit) {
        // 向量检索负责找语义相关条目，真正返回前还会补充知识库业务字段。
        List<Map<String, Object>> vectorRows = knowledgeVectorSyncService.search(query, searchType.apiValue(), limit);
        List<Map<String, Object>> results = new ArrayList<>();
        for (Map<String, Object> row : vectorRows) {
            Map<String, Object> item = toVectorSearchItem(row);
            if (item != null) {
                results.add(item);
            }
        }
        return results;
    }

    private List<Map<String, Object>> searchByText(String query, SearchType searchType, List<String> expandedTerms) {
        // 文本检索直接扫业务表，更适合术语、别名、洗护代码等强关键词场景。
        List<Map<String, Object>> results = new ArrayList<>();

        if (searchType.includeFabric()) {
            List<Map<String, Object>> fabrics = jdbcTemplate.queryForList(
                    "SELECT id, name, alias, fabric_type, summary, properties, care_guide, suitable_seasons, suitable_occasions, keywords, source " +
                            "FROM knowledge_fabrics WHERE is_active = 1 ORDER BY updated_at DESC, id ASC");
            for (Map<String, Object> fabric : fabrics) {
                Map<String, Object> item = toFabricSearchItem(fabric, query, expandedTerms);
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
                Map<String, Object> item = toGuideSearchItem(guide, query, expandedTerms);
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
                Map<String, Object> item = toCareLabelSearchItem(careLabel, query, expandedTerms);
                if (item != null) {
                    results.add(item);
                }
            }
        }
        return results;
    }

    private Map<String, Object> toVectorSearchItem(Map<String, Object> vectorRow) {
        String knowledgeType = asString(vectorRow.get("knowledgeType"));
        Long knowledgeId = asLong(vectorRow.get("knowledgeId"));
        if (knowledgeId == null || StringUtils.isBlank(knowledgeType)) {
            return null;
        }

        double similarity = vectorRow.get("similarity") instanceof Number number ? number.doubleValue() : 0D;
        int relevance = (int) Math.max(1, Math.min(99, Math.round(similarity * 100)));
        Map<String, Object> item = switch (knowledgeType) {
            case KnowledgeVectorSyncService.TYPE_FABRIC -> buildVectorFabricItem(knowledgeId, relevance);
            case KnowledgeVectorSyncService.TYPE_GUIDE -> buildVectorGuideItem(knowledgeId, relevance);
            case KnowledgeVectorSyncService.TYPE_CARE_LABEL -> buildVectorCareLabelItem(knowledgeId, relevance);
            default -> null;
        };
        if (item != null) {
            item.put("similarity", similarity);
        }
        return item;
    }

    private Map<String, Object> buildVectorFabricItem(Long fabricId, int relevance) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, name, alias, fabric_type, summary, properties, suitable_seasons, suitable_occasions, source " +
                        "FROM knowledge_fabrics WHERE id = ? AND is_active = 1",
                fabricId);
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> row = rows.get(0);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("documentId", "fabric-" + row.get("id"));
        item.put("title", row.get("name"));
        item.put("type", "fabric");
        item.put("content", buildSnippet(asString(row.get("summary")), asString(row.get("properties"))));
        item.put("source", StringUtils.defaultIfBlank(asString(row.get("source")), "knowledge-fabric"));
        item.put("relevance", relevance);
        item.put("matchSource", "vector");
        item.put("tags", mergeTags(
                splitCsv(asString(row.get("suitable_seasons"))),
                splitCsv(asString(row.get("suitable_occasions")))));
        return item;
    }

    private Map<String, Object> buildVectorGuideItem(Long guideId, int relevance) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, title, summary, content, tags, source " +
                        "FROM knowledge_guides WHERE id = ? AND is_active = 1",
                guideId);
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> row = rows.get(0);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("documentId", "guide-" + row.get("id"));
        item.put("title", row.get("title"));
        item.put("type", "guide");
        item.put("content", buildSnippet(asString(row.get("summary")), asString(row.get("content"))));
        item.put("source", StringUtils.defaultIfBlank(asString(row.get("source")), "knowledge-guide"));
        item.put("relevance", relevance);
        item.put("matchSource", "vector");
        item.put("tags", splitCsv(asString(row.get("tags"))));
        return item;
    }

    private Map<String, Object> buildVectorCareLabelItem(Long careLabelId, int relevance) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, symbol_name, category, instruction_text, explanation, keywords, source " +
                        "FROM knowledge_care_labels WHERE id = ? AND is_active = 1",
                careLabelId);
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> row = rows.get(0);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("documentId", "care-label-" + row.get("id"));
        item.put("title", row.get("symbol_name"));
        item.put("type", "care-label");
        item.put("content", buildSnippet(asString(row.get("instruction_text")), asString(row.get("explanation"))));
        item.put("source", StringUtils.defaultIfBlank(asString(row.get("source")), "knowledge-care-label"));
        item.put("relevance", relevance);
        item.put("matchSource", "vector");
        item.put("tags", mergeTags(
                List.of(asString(row.get("category"))),
                splitCsv(asString(row.get("keywords")))));
        return item;
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
        item.put("matchSource", "text");
        item.put("tags", mergeTags(
                splitCsv(asString(fabric.get("suitable_seasons"))),
                splitCsv(asString(fabric.get("suitable_occasions")))));
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
        item.put("matchSource", "text");
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
        item.put("matchSource", "text");
        item.put("tags", mergeTags(
                List.of(asString(careLabel.get("category"))),
                splitCsv(asString(careLabel.get("keywords")))));
        return item;
    }

    private Map<String, Object> toMatchedFabricItem(Map<String, Object> row, String query, List<String> terms) {
        String title = asString(row.get("name"));
        String body = String.join(" ",
                asString(row.get("alias")),
                asString(row.get("fabric_type")),
                asString(row.get("summary")),
                asString(row.get("properties")),
                asString(row.get("care_guide")),
                asString(row.get("suitable_seasons")),
                asString(row.get("suitable_occasions")),
                asString(row.get("keywords")));
        int relevance = calculateRelevance(query, title, body, terms);
        if (relevance <= 0) {
            return null;
        }

        Map<String, Object> detail = toFabricDetail(row);
        detail.put("relevance", relevance);
        return detail;
    }

    private Map<String, Object> toMatchedCareLabelItem(Map<String, Object> row, String query, List<String> terms) {
        String title = asString(row.get("symbol_name"));
        String body = String.join(" ",
                asString(row.get("symbol_code")),
                asString(row.get("category")),
                asString(row.get("instruction_text")),
                asString(row.get("explanation")),
                asString(row.get("do_text")),
                asString(row.get("dont_text")),
                asString(row.get("keywords")));
        int relevance = calculateRelevance(query, title, body, terms);
        if (relevance <= 0) {
            return null;
        }

        return toCareLabelDetail(row, relevance);
    }

    private Map<String, Object> toFabricDetail(Map<String, Object> row) {
        Map<String, Object> characteristics = new LinkedHashMap<>();
        characteristics.put("warmth", asInt(row.get("warmth_score")));
        characteristics.put("breathability", asInt(row.get("breathability_score")));
        characteristics.put("comfort", asInt(row.get("comfort_score")));
        characteristics.put("durability", asInt(row.get("durability_score")));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fabricId", asLong(row.get("id")));
        result.put("name", row.get("name"));
        result.put("alias", splitCsv(asString(row.get("alias"))));
        result.put("type", row.get("fabric_type"));
        result.put("characteristics", characteristics);
        result.put("summary", row.get("summary"));
        result.put("properties", row.get("properties"));
        result.put("careGuide", row.get("care_guide"));
        result.put("suitableSeasons", splitCsv(asString(row.get("suitable_seasons"))));
        result.put("suitableOccasions", splitCsv(asString(row.get("suitable_occasions"))));
        result.put("keywords", splitCsv(asString(row.get("keywords"))));
        return result;
    }

    private Map<String, Object> toCareLabelDetail(Map<String, Object> row, int relevance) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("careLabelId", asLong(row.get("id")));
        result.put("symbolCode", row.get("symbol_code"));
        result.put("symbolName", row.get("symbol_name"));
        result.put("category", row.get("category"));
        result.put("instruction", row.get("instruction_text"));
        result.put("explanation", row.get("explanation"));
        result.put("doText", row.get("do_text"));
        result.put("dontText", row.get("dont_text"));
        result.put("keywords", splitCsv(asString(row.get("keywords"))));
        if (relevance > 0) {
            result.put("relevance", relevance);
        }
        return result;
    }

    private int calculateRelevance(String query, String title, String body, List<String> terms) {
        // 规则相关度：标题命中权重大于正文命中，多关键词同时命中会追加奖励分。
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

    private List<String> normalizeKeywords(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return List.of();
        }

        Set<String> terms = new LinkedHashSet<>();
        for (String keyword : keywords) {
            String trimmed = StringUtils.trimToEmpty(keyword);
            if (StringUtils.isBlank(trimmed)) {
                continue;
            }
            terms.addAll(expandTerms(trimmed));
            Arrays.stream(trimmed.split("[\\s,，、;/]+"))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .map(this::safeLower)
                    .forEach(terms::add);
        }
        return new ArrayList<>(terms);
    }

    private List<String> expandTerms(String query) {
        // 同义词扩展把中英文、近义词和典型场景词归并到一起，提升知识召回覆盖率。
        Set<String> terms = new LinkedHashSet<>();
        terms.add(safeLower(query));
        for (String token : query.split("[\\s,，、;/]+")) {
            if (StringUtils.isNotBlank(token)) {
                terms.add(safeLower(token));
            }
        }

        addIfMatched(terms, query, List.of("羊毛", "毛呢", "wool"), List.of("wool", "羊毛", "毛呢", "warm", "winter", "coat", "sweater"));
        addIfMatched(terms, query, List.of("棉", "纯棉", "cotton"), List.of("cotton", "棉", "纯棉", "shirt", "tshirt", "breathable", "daily"));
        addIfMatched(terms, query, List.of("亚麻", "linen"), List.of("linen", "亚麻", "summer", "cool", "breathable"));
        addIfMatched(terms, query, List.of("牛仔", "denim"), List.of("denim", "牛仔", "casual", "daily", "jacket", "pants"));
        addIfMatched(terms, query, List.of("真丝", "丝绸", "silk"), List.of("silk", "真丝", "丝绸", "elegant", "dress", "formal"));
        addIfMatched(terms, query, List.of("聚酯", "涤纶", "polyester"), List.of("polyester", "聚酯", "涤纶", "durable", "quick dry"));
        addIfMatched(terms, query, List.of("粘胶", "人造丝", "viscose", "rayon"), List.of("viscose", "rayon", "粘胶", "人造丝", "soft", "drape"));
        addIfMatched(terms, query, List.of("通勤", "办公室", "office", "commute"), List.of("commute", "office", "formal", "layering"));
        addIfMatched(terms, query, List.of("面试", "interview"), List.of("interview", "commute", "shirt", "formal"));
        addIfMatched(terms, query, List.of("冬季", "冬天", "winter"), List.of("winter", "warm", "coat", "wool"));
        addIfMatched(terms, query, List.of("夏季", "夏天", "summer"), List.of("summer", "cool", "linen", "breathable"));
        addIfMatched(terms, query, List.of("指南", "穿搭", "guide"), List.of("guide", "match", "pairing", "layering"));
        addIfMatched(terms, query, List.of("洗护", "洗标", "care label", "laundry"), List.of("care", "wash", "laundry", "label", "instruction"));
        addIfMatched(terms, query, List.of("机洗", "machine wash"), List.of("machine wash", "wash", "care"));
        addIfMatched(terms, query, List.of("手洗", "hand wash"), List.of("hand wash", "wash", "care"));
        addIfMatched(terms, query, List.of("漂白", "bleach"), List.of("bleach", "do not bleach", "care"));
        addIfMatched(terms, query, List.of("熨烫", "iron"), List.of("iron", "low heat", "care"));
        addIfMatched(terms, query, List.of("干洗", "dry clean"), List.of("dry clean", "care", "clean"));
        addIfMatched(terms, query, List.of("烘干", "tumble dry"), List.of("tumble dry", "do not tumble dry", "care"));

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

    private void mergeSearchResult(Map<String, Map<String, Object>> mergedResults, Map<String, Object> item, boolean vectorResult) {
        if (item == null) {
            return;
        }

        String documentId = asString(item.get("documentId"));
        if (StringUtils.isBlank(documentId)) {
            return;
        }

        item.put("matchSource", vectorResult ? "vector" : "text");
        item.put("vectorScore", vectorResult ? asInt(item.get("relevance")) : 0);
        item.put("textScore", vectorResult ? 0 : asInt(item.get("relevance")));
        item.put("similarity", vectorResult ? asDouble(item.get("similarity")) : 0D);

        Map<String, Object> existing = mergedResults.get(documentId);
        if (existing == null) {
            item.put("relevance", blendedRelevance(item));
            mergedResults.put(documentId, item);
            return;
        }

        if (vectorResult) {
            existing.put("vectorScore", Math.max(asInt(existing.get("vectorScore")), asInt(item.get("vectorScore"))));
            existing.put("similarity", Math.max(asDouble(existing.get("similarity")), asDouble(item.get("similarity"))));
            if (!"both".equals(existing.get("matchSource"))) {
                existing.put("matchSource", "vector");
            }
        } else {
            existing.put("textScore", Math.max(asInt(existing.get("textScore")), asInt(item.get("textScore"))));
            if (!"both".equals(existing.get("matchSource"))) {
                existing.put("matchSource", "text");
            }
        }

        if (asInt(existing.get("vectorScore")) > 0 && asInt(existing.get("textScore")) > 0) {
            existing.put("matchSource", "both");
        }

        fillBlank(existing, item, "content");
        fillBlank(existing, item, "source");
        fillBlank(existing, item, "tags");
        existing.put("relevance", blendedRelevance(existing));
    }

    private int blendedRelevance(Map<String, Object> item) {
        int vectorScore = asInt(item.get("vectorScore"));
        int textScore = asInt(item.get("textScore"));
        int score = Math.max(vectorScore, textScore);

        if (vectorScore > 0) {
            score = Math.max(score, Math.min(99, vectorScore + vectorScoreBoost));
        }
        if (vectorScore > 0 && textScore > 0) {
            score = Math.min(99, Math.max(score, ((vectorScore + textScore) / 2) + 8));
        }
        return Math.min(99, Math.max(score, textScore));
    }

    private void fillBlank(Map<String, Object> target, Map<String, Object> source, String key) {
        Object targetValue = target.get(key);
        if (targetValue == null || StringUtils.isBlank(String.valueOf(targetValue))) {
            target.put(key, source.get(key));
        }
    }

    private Comparator<Map<String, Object>> searchItemComparator() {
        return Comparator
                .comparing((Map<String, Object> item) -> asInt(item.get("relevance")), Comparator.reverseOrder())
                .thenComparing((Map<String, Object> item) -> matchSourcePriority(asString(item.get("matchSource"))), Comparator.reverseOrder())
                .thenComparing(item -> StringUtils.defaultString(asString(item.get("title"))), String.CASE_INSENSITIVE_ORDER);
    }

    private Comparator<Map<String, Object>> matchItemComparator(String nameKey) {
        return Comparator
                .comparing((Map<String, Object> item) -> asInt(item.get("relevance")), Comparator.reverseOrder())
                .thenComparing(item -> StringUtils.defaultString(asString(item.get(nameKey))), String.CASE_INSENSITIVE_ORDER);
    }

    private int clampLimit(Integer limit, int defaultLimit) {
        return Math.max(1, Math.min(limit == null ? defaultLimit : limit, MAX_LIMIT));
    }

    private List<String> splitCsv(String value) {
        if (StringUtils.isBlank(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
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

    private Double asDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0D;
    }

    private Integer matchSourcePriority(String matchSource) {
        if ("both".equalsIgnoreCase(matchSource)) {
            return 3;
        }
        if ("vector".equalsIgnoreCase(matchSource)) {
            return 2;
        }
        if ("text".equalsIgnoreCase(matchSource)) {
            return 1;
        }
        return 0;
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
                default -> throw new BusinessException(400, "不支持的知识搜索类型");
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
                case FABRIC -> KnowledgeVectorSyncService.TYPE_FABRIC;
                case GUIDE -> KnowledgeVectorSyncService.TYPE_GUIDE;
                case CARE_LABEL -> KnowledgeVectorSyncService.TYPE_CARE_LABEL;
            };
        }
    }
}
