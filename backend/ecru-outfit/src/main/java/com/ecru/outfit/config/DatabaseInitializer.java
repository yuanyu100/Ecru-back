package com.ecru.outfit.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * PostgreSQL / pgvector initialization.
 * Keep this idempotent so restart does not wipe embedding data.
 */
@Slf4j
@Component
public class DatabaseInitializer {

    @Autowired
    @Qualifier("postgresJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        try {
            enablePgVectorExtension();
            initClothingEmbeddingsTable();
            log.info("数据库初始化完成");
        } catch (Exception e) {
            log.error("数据库初始化失败: {}", e.getMessage(), e);
        }
    }

    private void enablePgVectorExtension() {
        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
            log.info("pgvector 扩展已就绪");
        } catch (Exception e) {
            log.error("启用 pgvector 扩展失败: {}", e.getMessage(), e);
        }
    }

    private void initClothingEmbeddingsTable() {
        try {
            createEmbeddingsTableIfMissing();
            ensureMetadataColumn();
            ensureUniqueConstraint();
            ensureVectorIndex();
            log.info("clothing_embeddings 表初始化完成");
        } catch (Exception e) {
            log.error("初始化 clothing_embeddings 表失败: {}", e.getMessage(), e);
        }
    }

    private void createEmbeddingsTableIfMissing() {
        String createTableSql = """
            CREATE TABLE IF NOT EXISTS clothing_embeddings (
                id SERIAL PRIMARY KEY,
                clothing_id BIGINT NOT NULL,
                user_id BIGINT NOT NULL,
                embedding VECTOR(1024) NOT NULL,
                embedding_model VARCHAR(255) NOT NULL,
                embedding_text TEXT NOT NULL,
                metadata JSONB,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
        jdbcTemplate.execute(createTableSql);
    }

    private void ensureMetadataColumn() {
        jdbcTemplate.execute("""
            ALTER TABLE clothing_embeddings
            ADD COLUMN IF NOT EXISTS metadata JSONB
            """);
    }

    private void ensureUniqueConstraint() {
        Integer constraintExists = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM pg_constraint
            WHERE conname = 'uk_clothing_embeddings_clothing_id'
            """, Integer.class);

        if (constraintExists != null && constraintExists == 0) {
            jdbcTemplate.execute("""
                ALTER TABLE clothing_embeddings
                ADD CONSTRAINT uk_clothing_embeddings_clothing_id UNIQUE (clothing_id)
                """);
        }
    }

    private void ensureVectorIndex() {
        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_clothing_embeddings_embedding
            ON clothing_embeddings
            USING ivfflat (embedding)
            WITH (lists = 100)
            """);
    }
}
