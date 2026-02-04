-- ================================================
-- PostgreSQL 数据库脚本
-- 适用于：智能衣橱系统
-- 时间：2026-02-04
-- ================================================

-- 切换到 ecru-pg 数据库
\c "ecru-pg";

-- ================================================
-- 1. 启用必要的扩展
-- ================================================

-- 启用 pgvector 扩展（向量存储）
CREATE EXTENSION IF NOT EXISTS vector;

-- 启用 uuid-ossp 扩展（如果需要）
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ================================================
-- 2. RAG 模块 - 向量相关表
-- ================================================

-- 创建更新时间触发器函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- --------------------------------------------
-- 2.1 衣物向量表
-- --------------------------------------------
CREATE TABLE IF NOT EXISTS clothing_embeddings (
    id BIGSERIAL PRIMARY KEY,
    clothing_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    embedding VECTOR(1536) NOT NULL,
    embedding_model VARCHAR(50) DEFAULT 'text-embedding-3-small',
    embedding_text TEXT,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建向量索引
CREATE INDEX IF NOT EXISTS idx_clothing_embedding_vector ON clothing_embeddings 
USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- 创建用户ID索引
CREATE INDEX IF NOT EXISTS idx_clothing_embedding_user_id ON clothing_embeddings(user_id);

-- 创建衣物ID唯一索引
CREATE UNIQUE INDEX IF NOT EXISTS idx_clothing_embedding_clothing_id ON clothing_embeddings(clothing_id);

-- 创建更新时间触发器
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger 
        WHERE tgname = 'update_clothing_embeddings_updated_at'
    ) THEN
        CREATE TRIGGER update_clothing_embeddings_updated_at 
            BEFORE UPDATE ON clothing_embeddings 
            FOR EACH ROW 
            EXECUTE FUNCTION update_updated_at_column();
    END IF;
END $$;

-- --------------------------------------------
-- 2.2 检索历史表
-- --------------------------------------------
CREATE TABLE IF NOT EXISTS search_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    query_text VARCHAR(500) NOT NULL,
    query_type SMALLINT NOT NULL DEFAULT 1,
    filters JSONB,
    result_count INTEGER NOT NULL DEFAULT 0,
    top_result_ids JSONB,
    execution_time_ms INTEGER,
    is_success BOOLEAN NOT NULL DEFAULT TRUE,
    error_message VARCHAR(500),
    client_ip VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_search_history_user_id ON search_history(user_id);
CREATE INDEX IF NOT EXISTS idx_search_history_created_at ON search_history(created_at);
CREATE INDEX IF NOT EXISTS idx_search_history_user_created ON search_history(user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_search_history_query_type ON search_history(query_type);

-- --------------------------------------------
-- 2.3 检索配置表
-- --------------------------------------------
CREATE TABLE IF NOT EXISTS search_config (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value VARCHAR(500) NOT NULL,
    description VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建更新时间触发器
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger 
        WHERE tgname = 'update_search_config_updated_at'
    ) THEN
        CREATE TRIGGER update_search_config_updated_at 
            BEFORE UPDATE ON search_config 
            FOR EACH ROW 
            EXECUTE FUNCTION update_updated_at_column();
    END IF;
END $$;

-- --------------------------------------------
-- 2.4 用户检索偏好表
-- --------------------------------------------
CREATE TABLE IF NOT EXISTS user_search_preference (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    preferred_categories JSONB,
    preferred_colors JSONB,
    preferred_styles JSONB,
    default_top_k INTEGER DEFAULT 10,
    min_similarity_score DECIMAL(3,2) DEFAULT 0.6,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建用户ID唯一索引
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_search_pref_user_id ON user_search_preference(user_id);

-- 创建更新时间触发器
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger 
        WHERE tgname = 'update_user_search_pref_updated_at'
    ) THEN
        CREATE TRIGGER update_user_search_pref_updated_at 
            BEFORE UPDATE ON user_search_preference 
            FOR EACH ROW 
            EXECUTE FUNCTION update_updated_at_column();
    END IF;
END $$;

-- --------------------------------------------
-- 2.5 相似衣物关联表（用于缓存相似关系）
-- --------------------------------------------
CREATE TABLE IF NOT EXISTS clothing_similarities (
    id BIGSERIAL PRIMARY KEY,
    source_clothing_id BIGINT NOT NULL,
    target_clothing_id BIGINT NOT NULL,
    similarity_score DECIMAL(4,3) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(source_clothing_id, target_clothing_id)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_clothing_sim_source ON clothing_similarities(source_clothing_id);
CREATE INDEX IF NOT EXISTS idx_clothing_sim_target ON clothing_similarities(target_clothing_id);
CREATE INDEX IF NOT EXISTS idx_clothing_sim_score ON clothing_similarities(similarity_score DESC);

-- ================================================
-- 3. 风格偏好模块 - 向量相关表
-- ================================================

-- --------------------------------------------
-- 3.1 风格图片向量表
-- --------------------------------------------
CREATE TABLE IF NOT EXISTS style_image_embeddings (
    id BIGSERIAL PRIMARY KEY,
    image_id BIGINT NOT NULL UNIQUE,
    embedding VECTOR(1536),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建向量索引（IVFFlat）
CREATE INDEX IF NOT EXISTS idx_style_image_embedding ON style_image_embeddings 
USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- ================================================
-- 4. 初始化数据
-- ================================================

-- 插入默认搜索配置
INSERT INTO search_config (config_key, config_value, description)
VALUES
('default_top_k', '10', '默认返回结果数量'),
('similarity_threshold', '0.7', '相似度阈值（0-1之间）'),
('max_query_length', '500', '最大查询文本长度'),
('enable_hybrid_search', 'true', '是否启用混合检索'),
('vector_dimension', '1536', '向量维度'),
('embedding_model', 'text-embedding-3-small', '默认Embedding模型'),
('max_search_results', '50', '最大返回结果数量'),
('enable_search_history', 'true', '是否记录检索历史')
ON CONFLICT (config_key) DO NOTHING;

-- ================================================
-- 5. 权限设置
-- ================================================

-- 授予用户权限
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO postgres;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO postgres;
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO postgres;

-- 刷新权限
COMMIT;

-- ================================================
-- 完成
-- ================================================
SELECT 'PostgreSQL 数据库脚本执行完成！' AS message;
