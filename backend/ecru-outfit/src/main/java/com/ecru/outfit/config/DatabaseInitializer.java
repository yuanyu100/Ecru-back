package com.ecru.outfit.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * 数据库初始化器
 * 用于启用pgvector扩展
 */
@Slf4j
@Component
public class DatabaseInitializer {

    @Autowired
    @Qualifier("postgresJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    /**
     * 初始化数据库
     */
    @PostConstruct
    public void init() {
        try {
            // 启用pgvector扩展
            enablePgVectorExtension();
            // 初始化clothing_embeddings表
            initClothingEmbeddingsTable();
            log.info("数据库初始化成功");
        } catch (Exception e) {
            log.error("数据库初始化失败: {}", e.getMessage());
        }
    }

    /**
     * 启用pgvector扩展
     */
    private void enablePgVectorExtension() {
        try {
            // 检查pgvector扩展是否已安装
            String checkSql = "SELECT 1 FROM pg_extension WHERE extname = 'vector'";
            Integer exists = jdbcTemplate.queryForObject(checkSql, Integer.class);
            
            if (exists == null) {
                // 安装pgvector扩展
                String installSql = "CREATE EXTENSION IF NOT EXISTS vector";
                jdbcTemplate.execute(installSql);
                log.info("pgvector 扩展已安装");
            } else {
                log.info("pgvector 扩展已存在");
            }
        } catch (Exception e) {
            log.error("启用pgvector扩展失败: {}", e.getMessage());
            // 继续执行，可能是因为数据库用户没有权限安装扩展
            // 在生产环境中，应该确保数据库管理员已经安装了pgvector扩展
        }
    }

    /**
     * 初始化clothing_embeddings表
     */
    private void initClothingEmbeddingsTable() {
        try {
            // 直接尝试删除并重建表，确保维度正确
            try {
                // 先删除表
                jdbcTemplate.execute("DROP TABLE IF EXISTS clothing_embeddings");
                log.info("已删除旧的clothing_embeddings表");
                
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
                log.info("已重新创建clothing_embeddings表，向量维度为1024");
            } catch (Exception e) {
                log.error("重建表结构失败: {}", e.getMessage());
                e.printStackTrace();
            }

            // 创建索引
            try {
                String createIndexSql = "CREATE INDEX IF NOT EXISTS idx_clothing_embeddings_embedding ON clothing_embeddings USING ivfflat (embedding) WITH (lists = 100)";
                jdbcTemplate.execute(createIndexSql);
                log.info("已创建向量索引");
            } catch (Exception e) {
                log.error("创建索引失败: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("初始化clothing_embeddings表失败: {}", e.getMessage());
            e.printStackTrace();
        }
    }

}