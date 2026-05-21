package com.ecru.web.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KnowledgeVectorTableInitializer {

    private final JdbcTemplate postgresJdbcTemplate;

    public KnowledgeVectorTableInitializer(@Qualifier("postgresJdbcTemplate") JdbcTemplate postgresJdbcTemplate) {
        this.postgresJdbcTemplate = postgresJdbcTemplate;
    }

    @PostConstruct
    public void init() {
        try {
            postgresJdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
            postgresJdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS knowledge_embeddings (
                        id SERIAL PRIMARY KEY,
                        knowledge_type VARCHAR(32) NOT NULL,
                        knowledge_id BIGINT NOT NULL,
                        title VARCHAR(255) NOT NULL,
                        embedding VECTOR(1024) NOT NULL,
                        embedding_model VARCHAR(255) NOT NULL,
                        embedding_text TEXT NOT NULL,
                        metadata JSONB,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            postgresJdbcTemplate.execute("""
                    CREATE UNIQUE INDEX IF NOT EXISTS uk_knowledge_embeddings_type_id
                    ON knowledge_embeddings (knowledge_type, knowledge_id)
                    """);
            postgresJdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_knowledge_embeddings_type
                    ON knowledge_embeddings (knowledge_type)
                    """);
            postgresJdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_knowledge_embeddings_embedding
                    ON knowledge_embeddings
                    USING ivfflat (embedding vector_cosine_ops)
                    WITH (lists = 100)
                    """);
            log.info("knowledge_embeddings table initialized");
        } catch (Exception e) {
            log.error("Failed to initialize knowledge_embeddings table: {}", e.getMessage(), e);
        }
    }
}
