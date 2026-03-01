package com.ecru.outfit.service.rag;

import com.ecru.outfit.config.PgVectorConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * pgvector 向量数据库服务
 */
@Slf4j
@Service
public class PgVectorService {

    @Autowired
    @Qualifier("postgresJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PgVectorConfig pgVectorConfig;

    /**
     * 初始化向量表结构
     */
    @PostConstruct
    public void init() {
        try {
            log.info("开始初始化pgvector表结构");
            // 检查并创建向量表
            log.info("开始创建向量表");
            createVectorTable();
            log.info("向量表创建成功");
            log.info("pgvector 表结构初始化成功");
        } catch (Exception e) {
            log.error("pgvector 表结构初始化失败: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 创建向量表
     */
    private void createVectorTable() {
        String sql = """
        CREATE TABLE IF NOT EXISTS clothing_embeddings (
            id SERIAL PRIMARY KEY,
            clothing_id BIGINT NOT NULL,
            user_id BIGINT NOT NULL,
            embedding VECTOR(?) NOT NULL,
            metadata JSONB DEFAULT '{}'::jsonb,
            embedding_model VARCHAR(50) DEFAULT 'text-embedding-3-small',
            embedding_text TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            UNIQUE (clothing_id)
        )
        """;

        jdbcTemplate.update(sql, pgVectorConfig.getDimension());
    }

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
            String metadataJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(metadata);
            
            // 日志输出，确认修改后的代码被执行
            log.info("开始存储向量，衣物ID: {}, 用户ID: {}", clothingId, userId);
            log.info("向量长度: {}", embedding.length);
            log.info("向量前10个元素: {}", java.util.Arrays.toString(java.util.Arrays.copyOfRange(embedding, 0, Math.min(embedding.length, 10))));

            // 先检查是否存在
            if (vectorExists(clothingId, userId)) {
                // 更新向量
                String updateSql = "UPDATE public.clothing_embeddings SET embedding = ?::vector, embedding_model = 'text-embedding-3-small', embedding_text = ?, metadata = ?::jsonb, updated_at = CURRENT_TIMESTAMP WHERE clothing_id = ?";
                log.info("更新向量SQL: {}", updateSql);
                jdbcTemplate.update(updateSql, vectorString, embeddingText, metadataJson, clothingId);
            } else {
                // 插入向量
                String insertSql = "INSERT INTO public.clothing_embeddings (clothing_id, user_id, embedding, embedding_model, embedding_text, metadata, updated_at) VALUES (?, ?, ?::vector, 'text-embedding-3-small', ?, ?::jsonb, CURRENT_TIMESTAMP)";
                log.info("插入向量SQL: {}", insertSql);
                jdbcTemplate.update(insertSql, clothingId, userId, vectorString, embeddingText, metadataJson);
            }
            log.info("存储向量成功，衣物ID: {}", clothingId);
            return true;
        } catch (Exception e) {
            log.error("存储向量失败: {}", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 检索向量
     * @param userId 用户ID
     * @param queryEmbedding 查询向量
     * @param topK 返回结果数量
     * @return 检索结果
     */
    public List<Map<String, Object>> searchVectors(Long userId, float[] queryEmbedding, int topK) {
        try {
            // 转换查询向量为PostgreSQL向量格式
            String vectorString = arrayToVectorString(queryEmbedding);

            // 执行向量检索
            String sql = """
            SELECT 
                ce.clothing_id, 
                1 - (ce.embedding <=> ?::vector) as similarity 
            FROM 
                clothing_embeddings ce
            WHERE 
                ce.user_id = ?
            ORDER BY 
                ce.embedding <=> ?::vector
            LIMIT ?
            """;

            return jdbcTemplate.queryForList(sql, vectorString, userId, vectorString, topK);
        } catch (Exception e) {
            log.error("检索向量失败: {}", e.getMessage());
            return new ArrayList<>();
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
            log.error("删除向量失败: {}", e.getMessage());
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
            log.error("检查向量存在失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 将float数组转换为PostgreSQL向量字符串格式
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

}
