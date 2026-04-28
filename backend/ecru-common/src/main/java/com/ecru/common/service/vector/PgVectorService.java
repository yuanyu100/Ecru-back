package com.ecru.common.service.vector;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * pgvector 向量数据库服务
 */
@Slf4j
@Service
public class PgVectorService {

    @Value("${ai.siliconflow.embedding.model}")
    private String modelName;

    @Autowired
    @Qualifier("postgresJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    /**
     * 存储向量
     * @param clothingId 衣物ID
     * @param userId 用户ID
     * @param embedding 向量值
     * @param metadata 元数据
     * @return 是否成功
     */
    public boolean storeVector(Long clothingId, Long userId, float[] embedding, Map<String, Object> metadata) {
        try {
            // 从元数据中获取衣物名称作为embedding_text
            String embeddingText = metadata.get("name") != null ? metadata.get("name").toString() : "";
            // 转换向量为PostgreSQL向量字符串格式
            String vectorString = arrayToVectorString(embedding);
            // 转换metadata为JSON字符串
            String metadataJson = new ObjectMapper().writeValueAsString(metadata);
            
            // 先检查是否存在
            if (vectorExists(clothingId, userId)) {
                // 更新向量
                String updateSql = "UPDATE clothing_embeddings SET embedding = ?::vector, embedding_model = ?, embedding_text = ?, metadata = ?::jsonb, updated_at = CURRENT_TIMESTAMP WHERE clothing_id = ?";
                jdbcTemplate.update(updateSql, vectorString, modelName, embeddingText, metadataJson, clothingId);
            } else {
                // 插入向量
                String insertSql = "INSERT INTO clothing_embeddings (clothing_id, user_id, embedding, embedding_model, embedding_text, metadata, created_at, updated_at) VALUES (?, ?, ?::vector, ?, ?, ?::jsonb, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
                jdbcTemplate.update(insertSql, clothingId, userId, vectorString, modelName, embeddingText, metadataJson);
            }
            return true;
        } catch (Exception e) {
            log.error("存储向量失败，clothingId={}: {}", clothingId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 删除向量
     * @param clothingId 衣物ID
     * @param userId 用户ID
     * @return 是否成功
     */
    public boolean deleteVector(Long clothingId, Long userId) {
        try {
            String sql = "DELETE FROM clothing_embeddings WHERE clothing_id = ?";
            int rows = jdbcTemplate.update(sql, clothingId);
            return rows > 0;
        } catch (Exception e) {
            log.error("删除向量失败，clothingId={}: {}", clothingId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 检查向量是否存在
     * @param clothingId 衣物ID
     * @param userId 用户ID
     * @return 是否存在
     */
    public boolean vectorExists(Long clothingId, Long userId) {
        try {
            String sql = "SELECT COUNT(*) FROM clothing_embeddings WHERE clothing_id = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, clothingId);
            return count != null && count > 0;
        } catch (Exception e) {
            log.warn("检查向量存在失败，clothingId={}: {}", clothingId, e.getMessage());
            return false;
        }
    }

    /**
     * 初始化表结构
     */
    public void initTable() {
        try {
            // 创建表结构
            String createTableSql = """
                    CREATE TABLE IF NOT EXISTS clothing_embeddings (
                        id SERIAL PRIMARY KEY,
                        clothing_id BIGINT NOT NULL,
                        user_id BIGINT NOT NULL,
                        embedding VECTOR(1024) NOT NULL,
                        embedding_model VARCHAR(255) NOT NULL,
                        embedding_text TEXT,
                        metadata JSONB,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE (clothing_id)
                    )
                    """;
            jdbcTemplate.execute(createTableSql);

            // 创建索引
            String createIndexSql = """
                    CREATE INDEX IF NOT EXISTS idx_clothing_embeddings_embedding 
                    ON clothing_embeddings 
                    USING ivfflat (embedding vector_cosine_ops) 
                    WITH (lists = 100)
                    """;
            jdbcTemplate.execute(createIndexSql);

            log.info("表结构初始化成功");
        } catch (Exception e) {
            log.error("初始化表结构失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 将float数组转换为PostgreSQL向量字符串格式
     * @param array float数组
     * @return 向量字符串
     */
    private String arrayToVectorString(float[] array) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i]);
            if (i < array.length - 1) {
                sb.append(',');
            }
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * 搜索向量
     * @param userId 用户ID
     * @param queryEmbedding 查询向量
     * @param limit 限制数量
     * @return 搜索结果
     */
    public java.util.List<java.util.Map<String, Object>> searchVectors(Long userId, float[] queryEmbedding, Integer limit) {
        try {
            // 转换查询向量为字符串格式
            String queryVectorString = arrayToVectorString(queryEmbedding);
            log.debug("执行向量搜索，userId={}, limit={}", userId, limit);
            
            // 执行向量搜索
            String sql = """
                    SELECT clothing_id, embedding_text, metadata, 
                           1 - (embedding <=> ?::vector) as similarity 
                    FROM clothing_embeddings 
                    WHERE user_id = ? 
                    ORDER BY similarity DESC 
                    LIMIT ?
                    """;
            
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                java.util.Map<String, Object> result = new java.util.HashMap<>();
                result.put("clothing_id", rs.getLong("clothing_id"));
                result.put("embedding_text", rs.getString("embedding_text"));
                result.put("metadata", rs.getString("metadata"));
                result.put("similarity", rs.getDouble("similarity"));
                return result;
            }, queryVectorString, userId, limit);
        } catch (Exception e) {
            log.error("搜索向量失败，userId={}: {}", userId, e.getMessage(), e);
            return new java.util.ArrayList<>();
        }
    }
}
