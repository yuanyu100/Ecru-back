package com.ecru.outfit.service.rag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * pgvector 向量数据库服务
 */
@Service
public class PgVectorService {

    @Value("${ai.siliconflow.embedding.model}")
    private String modelName;

    @Autowired
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
            String metadataJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(metadata);
            
            // 先检查是否存在
            if (vectorExists(clothingId, userId)) {
                // 更新向量
                String updateSql = "UPDATE clothing_embeddings SET embedding = ?::vector, embedding_model = ?, embedding_text = ?, metadata = ?::jsonb, updated_at = CURRENT_TIMESTAMP WHERE clothing_id = ?";
                jdbcTemplate.update(updateSql, vectorString, modelName, embeddingText, metadataJson, clothingId);
            } else {
                // 插入向量
                String insertSql = "INSERT INTO clothing_embeddings (clothing_id, user_id, embedding, embedding_model, embedding_text, metadata, updated_at) VALUES (?, ?, ?::vector, ?, ?, ?::jsonb, CURRENT_TIMESTAMP)";
                jdbcTemplate.update(insertSql, clothingId, userId, vectorString, modelName, embeddingText, metadataJson);
            }
            return true;
        } catch (Exception e) {
            System.err.println("存储向量失败: " + e.getMessage());
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
            System.err.println("检索向量失败: " + e.getMessage());
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
            System.err.println("删除向量失败: " + e.getMessage());
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
            System.err.println("检查向量存在失败: " + e.getMessage());
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

    /**
     * 初始化表结构
     */
    public void initTable() {
        try {
            // 检查pgvector扩展是否安装
            String checkExtensionSql = "SELECT EXISTS(SELECT 1 FROM pg_extension WHERE extname = 'vector')";
            Boolean extensionExists = jdbcTemplate.queryForObject(checkExtensionSql, Boolean.class);
            if (!extensionExists) {
                // 安装pgvector扩展
                jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
                System.out.println("已安装pgvector扩展");
            } else {
                System.out.println("pgvector扩展已存在");
            }

            // 直接尝试删除并重建表，确保维度正确
            try {
                // 先删除表
                jdbcTemplate.execute("DROP TABLE IF EXISTS clothing_embeddings");
                System.out.println("已删除旧的clothing_embeddings表");
                
                // 重建clothing_embeddings表，向量维度为1024
                String createTableSql = """
                CREATE TABLE clothing_embeddings (
                    id SERIAL PRIMARY KEY,
                    clothing_id BIGINT NOT NULL,
                    user_id BIGINT NOT NULL,
                    embedding VECTOR(1024) NOT NULL,
                    embedding_model VARCHAR(255) NOT NULL,
                    embedding_text TEXT NOT NULL,
                    metadata JSONB,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE(clothing_id)
                )
                """;
                jdbcTemplate.execute(createTableSql);
                System.out.println("已重新创建clothing_embeddings表，向量维度为1024");
            } catch (Exception e) {
                System.err.println("重建表结构失败: " + e.getMessage());
                e.printStackTrace();
            }

            // 创建索引
            try {
                String createIndexSql = "CREATE INDEX IF NOT EXISTS idx_clothing_embeddings_embedding ON clothing_embeddings USING ivfflat (embedding) WITH (lists = 100)";
                jdbcTemplate.execute(createIndexSql);
                System.out.println("已创建向量索引");
            } catch (Exception e) {
                System.err.println("创建索引失败: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("初始化表结构失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

}