package com.ecru.web.service;

import com.ecru.common.exception.BusinessException;
import com.ecru.common.service.vector.EmbeddingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class KnowledgeVectorSyncService {

    public static final String TYPE_FABRIC = "fabric";
    public static final String TYPE_GUIDE = "guide";
    public static final String TYPE_CARE_LABEL = "care-label";

    private final JdbcTemplate postgresJdbcTemplate;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;

    @Value("${ai.siliconflow.embedding.model}")
    private String modelName;

    @Value("${rag.vector.similarity-threshold:0.7}")
    private double similarityThreshold;

    public KnowledgeVectorSyncService(@Qualifier("postgresJdbcTemplate") JdbcTemplate postgresJdbcTemplate,
                                      EmbeddingService embeddingService,
                                      ObjectMapper objectMapper) {
        this.postgresJdbcTemplate = postgresJdbcTemplate;
        this.embeddingService = embeddingService;
        this.objectMapper = objectMapper;
    }

    public void upsertFabric(Map<String, Object> fabric) {
        // 这里生成向量
        String embeddingText = buildFabricEmbeddingText(fabric);
        upsert(
                TYPE_FABRIC,
                asLong(fabric.get("fabricId")),
                asString(fabric.get("name")),
                embeddingText,
                buildMetadata(TYPE_FABRIC, fabric));
    }

    public void upsertGuide(Map<String, Object> guide) {
        String embeddingText = buildGuideEmbeddingText(guide);
        upsert(
                TYPE_GUIDE,
                asLong(guide.get("guideId")),
                asString(guide.get("title")),
                embeddingText,
                buildMetadata(TYPE_GUIDE, guide));
    }

    public void upsertCareLabel(Map<String, Object> careLabel) {
        String embeddingText = buildCareLabelEmbeddingText(careLabel);
        upsert(
                TYPE_CARE_LABEL,
                asLong(careLabel.get("careLabelId")),
                asString(careLabel.get("symbolName")),
                embeddingText,
                buildMetadata(TYPE_CARE_LABEL, careLabel));
    }

    public void delete(String knowledgeType, Long knowledgeId) {
        if (knowledgeId == null || StringUtils.isBlank(knowledgeType)) {
            return;
        }

        try {
            postgresJdbcTemplate.update(
                    "DELETE FROM knowledge_embeddings WHERE knowledge_type = ? AND knowledge_id = ?",
                    knowledgeType,
                    knowledgeId);
        } catch (Exception e) {
            log.error("Failed to delete knowledge embedding, type={}, id={}: {}", knowledgeType, knowledgeId, e.getMessage(), e);
            throw new BusinessException(500, "知识向量删除失败");
        }
    }

    public List<Map<String, Object>> search(String query, String knowledgeType, Integer limit) {
        String safeQuery = StringUtils.trimToEmpty(query);
        if (StringUtils.isBlank(safeQuery)) {
            return List.of();
        }

        int safeLimit = limit == null || limit < 1 ? 10 : limit;
        float[] embedding = embeddingService.generateEmbedding(safeQuery);
        String vectorString = arrayToVectorString(embedding);

        try {
            StringBuilder sql = new StringBuilder("""
                    SELECT knowledge_type, knowledge_id, title, embedding_text, metadata,
                           1 - (embedding <=> ?::vector) AS similarity
                    FROM knowledge_embeddings
                    """);
            List<Object> args = new ArrayList<>();
            args.add(vectorString);
            if (StringUtils.isNotBlank(knowledgeType) && !"all".equalsIgnoreCase(knowledgeType)) {
                sql.append(" WHERE knowledge_type = ? AND 1 - (embedding <=> ?::vector) >= ? ");
                args.add(knowledgeType);
                args.add(vectorString);
                args.add(similarityThreshold);
            } else {
                sql.append(" WHERE 1 - (embedding <=> ?::vector) >= ? ");
                args.add(vectorString);
                args.add(similarityThreshold);
            }
            sql.append(" ORDER BY similarity DESC LIMIT ? ");
            args.add(safeLimit);

            return postgresJdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
                Map<String, Object> item = new HashMap<>();
                item.put("knowledgeType", rs.getString("knowledge_type"));
                item.put("knowledgeId", rs.getLong("knowledge_id"));
                item.put("title", rs.getString("title"));
                item.put("embeddingText", rs.getString("embedding_text"));
                item.put("metadata", rs.getString("metadata"));
                item.put("similarity", rs.getDouble("similarity"));
                return item;
            }, args.toArray());
        } catch (Exception e) {
            log.error("Failed to search knowledge embeddings, type={}, query={}: {}", knowledgeType, safeQuery, e.getMessage(), e);
            return List.of();
        }
    }

    private void upsert(String knowledgeType, Long knowledgeId, String title, String embeddingText, Map<String, Object> metadata) {
        if (knowledgeId == null) {
            throw new BusinessException(400, "知识ID不能为空");
        }

        String safeTitle = StringUtils.defaultIfBlank(StringUtils.trimToNull(title), knowledgeType + "-" + knowledgeId);
        String safeText = StringUtils.defaultIfBlank(StringUtils.trimToNull(embeddingText), safeTitle);
        float[] embedding = embeddingService.generateEmbedding(safeText);
        String vectorString = arrayToVectorString(embedding);

        try {
            String metadataJson = objectMapper.writeValueAsString(metadata);
            postgresJdbcTemplate.update(
                    """
                    INSERT INTO knowledge_embeddings
                    (knowledge_type, knowledge_id, title, embedding, embedding_model, embedding_text, metadata, created_at, updated_at)
                    VALUES (?, ?, ?, ?::vector, ?, ?, ?::jsonb, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    ON CONFLICT (knowledge_type, knowledge_id)
                    DO UPDATE SET
                        title = EXCLUDED.title,
                        embedding = EXCLUDED.embedding,
                        embedding_model = EXCLUDED.embedding_model,
                        embedding_text = EXCLUDED.embedding_text,
                        metadata = EXCLUDED.metadata,
                        updated_at = CURRENT_TIMESTAMP
                    """,
                    knowledgeType,
                    knowledgeId,
                    safeTitle,
                    vectorString,
                    modelName,
                    safeText,
                    metadataJson);
        } catch (Exception e) {
            log.error("Failed to sync knowledge embedding, type={}, id={}: {}", knowledgeType, knowledgeId, e.getMessage(), e);
            throw new BusinessException(500, "知识向量同步失败");
        }
    }

    private Map<String, Object> buildMetadata(String knowledgeType, Map<String, Object> source) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("knowledgeType", knowledgeType);
        metadata.putAll(source);
        return metadata;
    }

    private String buildFabricEmbeddingText(Map<String, Object> fabric) {
        return joinSections(
                weightedLine("面料名称", fabric.get("name"), 3),
                weightedLine("面料别名", fabric.get("alias"), 1),
                weightedLine("面料类型", fabric.get("fabricType"), 2),
                weightedLine("核心关键词", fabric.get("keywords"), 3),
                weightedLine("适用季节", fabric.get("suitableSeasons"), 2),
                weightedLine("适用场景", fabric.get("suitableOccasions"), 2),
                weightedLine("面料摘要", fabric.get("summary"), 2),
                weightedLine("面料特性", fabric.get("properties"), 2),
                weightedLine("护理说明", fabric.get("careGuide"), 1));
    }

    private String buildGuideEmbeddingText(Map<String, Object> guide) {
        return joinSections(
                weightedLine("指南标题", guide.get("title"), 3),
                weightedLine("指南副标题", guide.get("subtitle"), 1),
                weightedLine("指南类型", guide.get("guideType"), 2),
                weightedLine("核心关键词", guide.get("keywords"), 3),
                weightedLine("标签", guide.get("tags"), 2),
                weightedLine("内容摘要", guide.get("summary"), 2),
                weightedLine("正文内容", guide.get("content"), 1),
                weightedLine("作者", guide.get("author"), 1),
                weightedLine("发布日期", guide.get("publishDate"), 1));
    }

    private String buildCareLabelEmbeddingText(Map<String, Object> careLabel) {
        return joinSections(
                weightedLine("洗护符号", careLabel.get("symbolCode"), 2),
                weightedLine("符号名称", careLabel.get("symbolName"), 3),
                weightedLine("分类", careLabel.get("category"), 2),
                weightedLine("核心关键词", careLabel.get("keywords"), 3),
                weightedLine("洗护指令", careLabel.get("instruction"), 3),
                weightedLine("解释说明", careLabel.get("explanation"), 2),
                weightedLine("建议做法", careLabel.get("doText"), 1),
                weightedLine("禁忌做法", careLabel.get("dontText"), 1));
    }

    private String joinSections(String... sections) {
        StringBuilder builder = new StringBuilder();
        for (String section : sections) {
            if (StringUtils.isBlank(section)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(section.trim());
        }
        return builder.toString();
    }

    private String weightedLine(String label, Object value, int weight) {
        String text = StringUtils.trimToEmpty(asString(value));
        if (StringUtils.isBlank(text) || weight <= 0) {
            return "";
        }

        List<String> repeated = new ArrayList<>();
        for (int i = 0; i < weight; i++) {
            repeated.add(label + "：" + text);
        }
        return String.join("\n", repeated);
    }

    private String joinText(Object... values) {
        StringBuilder builder = new StringBuilder();
        for (Object value : values) {
            String text = asString(value);
            if (StringUtils.isBlank(text)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(text.trim());
        }
        return builder.toString();
    }

    private String arrayToVectorString(float[] array) {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        for (int i = 0; i < array.length; i++) {
            builder.append(array[i]);
            if (i < array.length - 1) {
                builder.append(',');
            }
        }
        builder.append(']');
        return builder.toString();
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }
}
