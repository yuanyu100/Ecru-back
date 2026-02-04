# RAG检索模块 - 详细设计文档

## 1. 模块概述

RAG（Retrieval-Augmented Generation）检索模块是AI智能衣橱系统的核心功能之一，负责基于语义向量的衣物检索。通过将衣物信息向量化存储到PostgreSQL+pgvector，支持自然语言查询、相似衣物推荐等智能检索功能。

---

## 2. 数据库表设计

### 2.1 核心表结构

#### 2.1.1 衣物向量表 (clothing_embeddings)

存储衣物的向量表示，用于语义相似度检索。

| 字段名 | 类型 | 长度/精度 | 是否为空 | 默认值 | 说明 |
|--------|------|-----------|----------|--------|------|
| id | BIGINT | - | 否 | AUTO_INCREMENT | 主键，自增ID |
| clothing_id | BIGINT | - | 否 | - | 关联衣物表ID |
| user_id | BIGINT | - | 否 | - | 用户ID（用于权限隔离） |
| embedding | VECTOR | 1536 | 否 | - | 衣物向量表示（1536维） |
| embedding_model | VARCHAR | 50 | 是 | 'text-embedding-3-small' | 使用的Embedding模型 |
| embedding_text | TEXT | - | 是 | - | 生成向量的原始文本 |
| metadata | JSON | - | 是 | {} | 扩展元数据 |
| created_at | TIMESTAMP | - | 否 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | - | 否 | CURRENT_TIMESTAMP | 更新时间 |

**索引设计：**
```sql
-- 向量相似度检索索引（IVFFlat算法，适合高维向量）
CREATE INDEX idx_clothing_embedding_vector ON clothing_embeddings 
USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- 用户ID索引（用于权限过滤）
CREATE INDEX idx_clothing_embedding_user_id ON clothing_embeddings(user_id);

-- 衣物ID唯一索引（一个衣物对应一个向量）
CREATE UNIQUE INDEX idx_clothing_embedding_clothing_id ON clothing_embeddings(clothing_id);
```

---

#### 2.1.2 检索历史表 (search_history)

记录用户的检索历史，用于优化推荐和统计分析。

| 字段名 | 类型 | 长度/精度 | 是否为空 | 默认值 | 说明 |
|--------|------|-----------|----------|--------|------|
| id | BIGINT | - | 否 | AUTO_INCREMENT | 主键，自增ID |
| user_id | BIGINT | - | 否 | - | 用户ID |
| query_text | VARCHAR | 500 | 否 | - | 检索文本 |
| query_type | TINYINT | - | 否 | 1 | 检索类型：1-语义检索，2-相似检索 |
| filters | JSON | - | 是 | NULL | 筛选条件（颜色、类别等） |
| result_count | INT | - | 否 | 0 | 返回结果数量 |
| top_result_ids | JSON | - | 是 | NULL | 前N个结果ID |
| execution_time_ms | INT | - | 是 | NULL | 执行耗时（毫秒） |
| is_success | BOOLEAN | - | 否 | TRUE | 是否检索成功 |
| error_message | VARCHAR | 500 | 是 | NULL | 错误信息 |
| client_ip | VARCHAR | 50 | 是 | NULL | 客户端IP |
| created_at | TIMESTAMP | - | 否 | CURRENT_TIMESTAMP | 创建时间 |

**索引设计：**
```sql
-- 用户ID索引
CREATE INDEX idx_search_history_user_id ON search_history(user_id);

-- 创建时间索引（用于时间范围查询）
CREATE INDEX idx_search_history_created_at ON search_history(created_at);

-- 复合索引（用户+时间）
CREATE INDEX idx_search_history_user_created ON search_history(user_id, created_at);
```

---

#### 2.1.3 检索配置表 (search_config)

存储检索相关的配置参数。

| 字段名 | 类型 | 长度/精度 | 是否为空 | 默认值 | 说明 |
|--------|------|-----------|----------|--------|------|
| id | BIGINT | - | 否 | AUTO_INCREMENT | 主键，自增ID |
| config_key | VARCHAR | 100 | 否 | - | 配置键 |
| config_value | VARCHAR | 500 | 否 | - | 配置值 |
| description | VARCHAR | 255 | 是 | NULL | 配置说明 |
| is_active | BOOLEAN | - | 否 | TRUE | 是否启用 |
| created_at | TIMESTAMP | - | 否 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | - | 否 | CURRENT_TIMESTAMP | 更新时间 |

**索引设计：**
```sql
-- 配置键唯一索引
CREATE UNIQUE INDEX idx_search_config_key ON search_config(config_key);
```

**默认配置数据：**
| config_key | config_value | description |
|------------|--------------|-------------|
| default_top_k | 10 | 默认返回结果数量 |
| similarity_threshold | 0.7 | 相似度阈值 |
| max_query_length | 500 | 最大查询文本长度 |
| enable_hybrid_search | true | 是否启用混合检索 |
| vector_dimension | 1536 | 向量维度 |

---

#### 2.1.4 用户检索偏好表 (user_search_preference)

存储用户的个性化检索偏好。

| 字段名 | 类型 | 长度/精度 | 是否为空 | 默认值 | 说明 |
|--------|------|-----------|----------|--------|------|
| id | BIGINT | - | 否 | AUTO_INCREMENT | 主键，自增ID |
| user_id | BIGINT | - | 否 | - | 用户ID |
| preferred_categories | JSON | - | 是 | NULL | 偏好的衣物类别 |
| preferred_colors | JSON | - | 是 | NULL | 偏好的颜色 |
| preferred_styles | JSON | - | 是 | NULL | 偏好的风格 |
| default_top_k | INT | - | 是 | 10 | 默认返回数量 |
| min_similarity_score | DECIMAL | 3,2 | 是 | 0.6 | 最小相似度分数 |
| created_at | TIMESTAMP | - | 否 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | - | 否 | CURRENT_TIMESTAMP | 更新时间 |

**索引设计：**
```sql
-- 用户ID唯一索引（一个用户一条记录）
CREATE UNIQUE INDEX idx_user_search_pref_user_id ON user_search_preference(user_id);
```

---

## 3. SQL语句

### 3.1 建表语句

```sql
-- ============================================
-- RAG检索模块 - 数据库表结构
-- 数据库: PostgreSQL 15 + pgvector
-- ============================================

-- 启用pgvector扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- --------------------------------------------
-- 1. 衣物向量表
-- --------------------------------------------
CREATE TABLE clothing_embeddings (
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

-- 创建索引
CREATE INDEX idx_clothing_embedding_vector ON clothing_embeddings 
USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

CREATE INDEX idx_clothing_embedding_user_id ON clothing_embeddings(user_id);

CREATE UNIQUE INDEX idx_clothing_embedding_clothing_id ON clothing_embeddings(clothing_id);

-- 创建更新时间触发器
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_clothing_embeddings_updated_at 
    BEFORE UPDATE ON clothing_embeddings 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- --------------------------------------------
-- 2. 检索历史表
-- --------------------------------------------
CREATE TABLE search_history (
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
CREATE INDEX idx_search_history_user_id ON search_history(user_id);
CREATE INDEX idx_search_history_created_at ON search_history(created_at);
CREATE INDEX idx_search_history_user_created ON search_history(user_id, created_at);
CREATE INDEX idx_search_history_query_type ON search_history(query_type);

-- --------------------------------------------
-- 3. 检索配置表
-- --------------------------------------------
CREATE TABLE search_config (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value VARCHAR(500) NOT NULL,
    description VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建更新时间触发器
CREATE TRIGGER update_search_config_updated_at 
    BEFORE UPDATE ON search_config 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- 插入默认配置
INSERT INTO search_config (config_key, config_value, description) VALUES
('default_top_k', '10', '默认返回结果数量'),
('similarity_threshold', '0.7', '相似度阈值（0-1之间）'),
('max_query_length', '500', '最大查询文本长度'),
('enable_hybrid_search', 'true', '是否启用混合检索'),
('vector_dimension', '1536', '向量维度'),
('embedding_model', 'text-embedding-3-small', '默认Embedding模型'),
('max_search_results', '50', '最大返回结果数量'),
('enable_search_history', 'true', '是否记录检索历史');

-- --------------------------------------------
-- 4. 用户检索偏好表
-- --------------------------------------------
CREATE TABLE user_search_preference (
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

-- 创建索引
CREATE UNIQUE INDEX idx_user_search_pref_user_id ON user_search_preference(user_id);

-- 创建更新时间触发器
CREATE TRIGGER update_user_search_pref_updated_at 
    BEFORE UPDATE ON user_search_preference 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- --------------------------------------------
-- 5. 相似衣物关联表（用于缓存相似关系）
-- --------------------------------------------
CREATE TABLE clothing_similarities (
    id BIGSERIAL PRIMARY KEY,
    source_clothing_id BIGINT NOT NULL,
    target_clothing_id BIGINT NOT NULL,
    similarity_score DECIMAL(4,3) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(source_clothing_id, target_clothing_id)
);

-- 创建索引
CREATE INDEX idx_clothing_sim_source ON clothing_similarities(source_clothing_id);
CREATE INDEX idx_clothing_sim_target ON clothing_similarities(target_clothing_id);
CREATE INDEX idx_clothing_sim_score ON clothing_similarities(similarity_score DESC);
```

### 3.2 常用查询语句

```sql
-- ============================================
-- 常用检索查询语句
-- ============================================

-- 1. 向量相似度检索（基础版）
-- 根据给定向量检索最相似的衣物
SELECT 
    ce.clothing_id,
    c.name,
    c.category,
    c.primary_color,
    1 - (ce.embedding <=> $1) AS similarity_score
FROM clothing_embeddings ce
JOIN clothings c ON ce.clothing_id = c.id
WHERE ce.user_id = $2
  AND c.is_deleted = FALSE
ORDER BY ce.embedding <=> $1
LIMIT $3;

-- 2. 向量相似度检索（带属性过滤）
-- 结合语义检索和属性筛选
SELECT 
    ce.clothing_id,
    c.name,
    c.category,
    c.primary_color,
    c.style_tags,
    1 - (ce.embedding <=> $1) AS similarity_score
FROM clothing_embeddings ce
JOIN clothings c ON ce.clothing_id = c.id
WHERE ce.user_id = $2
  AND c.is_deleted = FALSE
  AND ($3::varchar IS NULL OR c.category = $3)
  AND ($4::varchar IS NULL OR c.primary_color = $4)
  AND ($5::jsonb IS NULL OR c.style_tags @> $5)
ORDER BY ce.embedding <=> $1
LIMIT $6;

-- 3. 查找相似衣物
-- 根据指定衣物ID查找相似的衣物
SELECT 
    ce2.clothing_id,
    c.name,
    c.category,
    1 - (ce1.embedding <=> ce2.embedding) AS similarity_score
FROM clothing_embeddings ce1
JOIN clothing_embeddings ce2 ON ce1.clothing_id != ce2.clothing_id
JOIN clothings c ON ce2.clothing_id = c.id
WHERE ce1.clothing_id = $1
  AND ce1.user_id = ce2.user_id
  AND c.is_deleted = FALSE
  AND 1 - (ce1.embedding <=> ce2.embedding) >= $2
ORDER BY ce1.embedding <=> ce2.embedding
LIMIT $3;

-- 4. 获取用户检索历史
SELECT 
    id,
    query_text,
    query_type,
    filters,
    result_count,
    execution_time_ms,
    created_at
FROM search_history
WHERE user_id = $1
ORDER BY created_at DESC
LIMIT $2 OFFSET $3;

-- 5. 统计用户检索频率
SELECT 
    DATE(created_at) AS search_date,
    COUNT(*) AS search_count,
    AVG(execution_time_ms) AS avg_execution_time
FROM search_history
WHERE user_id = $1
  AND created_at >= $2
  AND is_success = TRUE
GROUP BY DATE(created_at)
ORDER BY search_date DESC;

-- 6. 获取热门检索词
SELECT 
    query_text,
    COUNT(*) AS search_count
FROM search_history
WHERE created_at >= $1
  AND is_success = TRUE
GROUP BY query_text
ORDER BY search_count DESC
LIMIT $2;

-- 7. 删除衣物的向量数据（软删除时调用）
DELETE FROM clothing_embeddings WHERE clothing_id = $1;

-- 8. 批量插入向量数据
INSERT INTO clothing_embeddings 
    (clothing_id, user_id, embedding, embedding_model, embedding_text, metadata)
VALUES 
    ($1, $2, $3::vector, $4, $5, $6)
ON CONFLICT (clothing_id) 
DO UPDATE SET 
    embedding = EXCLUDED.embedding,
    embedding_model = EXCLUDED.embedding_model,
    embedding_text = EXCLUDED.embedding_text,
    metadata = EXCLUDED.metadata,
    updated_at = CURRENT_TIMESTAMP;
```

---

## 4. RESTful API接口文档

### 4.1 接口概览

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| POST | /api/v1/search | 语义检索衣物 | 是 |
| POST | /api/v1/search/similar | 查找相似衣物 | 是 |
| GET | /api/v1/search/history | 获取检索历史 | 是 |
| DELETE | /api/v1/search/history/{id} | 删除检索记录 | 是 |
| GET | /api/v1/search/suggestions | 获取检索建议 | 是 |
| GET | /api/v1/search/preferences | 获取检索偏好 | 是 |
| PUT | /api/v1/search/preferences | 更新检索偏好 | 是 |

---

### 4.2 接口详情

#### 4.2.1 语义检索衣物

**请求信息**
- **Method:** POST
- **Path:** /api/v1/search
- **Content-Type:** application/json
- **Authorization:** Bearer {token}

**请求参数**

| 参数名 | 类型 | 是否必填 | 默认值 | 说明 |
|--------|------|----------|--------|------|
| query | String | 是 | - | 检索文本，如"适合面试的白色衬衫" |
| topK | Integer | 否 | 10 | 返回结果数量（1-50） |
| filters | Object | 否 | null | 属性过滤条件 |
| filters.category | String | 否 | null | 类别过滤：上装/下装/外套等 |
| filters.color | String | 否 | null | 颜色过滤 |
| filters.style | String | 否 | null | 风格过滤 |
| filters.occasion | String | 否 | null | 场合过滤 |
| filters.season | String | 否 | null | 季节过滤 |
| minScore | Float | 否 | 0.6 | 最小相似度分数（0-1） |

**请求示例**
```json
{
  "query": "找一件适合面试穿的白色衬衫",
  "topK": 10,
  "filters": {
    "category": "上装",
    "color": "白色",
    "occasion": "正式"
  },
  "minScore": 0.7
}
```

**响应参数**

| 参数名 | 类型 | 说明 |
|--------|------|------|
| code | Integer | 状态码：200成功 |
| message | String | 提示信息 |
| data | Object | 响应数据 |
| data.total | Integer | 总结果数 |
| data.query | String | 查询文本 |
| data.executionTimeMs | Integer | 执行耗时（毫秒） |
| data.results | Array | 检索结果列表 |
| data.results[].clothingId | Long | 衣物ID |
| data.results[].name | String | 衣物名称 |
| data.results[].category | String | 类别 |
| data.results[].primaryColor | String | 主色调 |
| data.results[].imageUrl | String | 图片URL |
| data.results[].styleTags | Array | 风格标签 |
| data.results[].similarityScore | Float | 相似度分数（0-1） |
| data.results[].matchReason | String | 匹配原因说明 |

**响应示例**
```json
{
  "code": 200,
  "message": "检索成功",
  "data": {
    "total": 3,
    "query": "找一件适合面试穿的白色衬衫",
    "executionTimeMs": 245,
    "results": [
      {
        "clothingId": 1001,
        "name": "白色商务衬衫",
        "category": "上装",
        "primaryColor": "白色",
        "imageUrl": "https://example.com/images/1001.jpg",
        "styleTags": ["通勤", "正式", "简约"],
        "similarityScore": 0.92,
        "matchReason": "白色衬衫，适合正式场合"
      },
      {
        "clothingId": 1005,
        "name": "米白色雪纺衬衫",
        "category": "上装",
        "primaryColor": "米白色",
        "imageUrl": "https://example.com/images/1005.jpg",
        "styleTags": ["通勤", "优雅"],
        "similarityScore": 0.85,
        "matchReason": "浅色衬衫，适合职场穿搭"
      }
    ]
  }
}
```

**错误码**

| 错误码 | 说明 |
|--------|------|
| 400 | 请求参数错误 |
| 401 | 未授权，Token无效 |
| 500 | 服务器内部错误 |

---

#### 4.2.2 查找相似衣物

**请求信息**
- **Method:** POST
- **Path:** /api/v1/search/similar
- **Content-Type:** application/json
- **Authorization:** Bearer {token}

**请求参数**

| 参数名 | 类型 | 是否必填 | 默认值 | 说明 |
|--------|------|----------|--------|------|
| clothingId | Long | 是 | - | 参考衣物ID |
| topK | Integer | 否 | 5 | 返回结果数量 |
| minScore | Float | 否 | 0.7 | 最小相似度分数 |
| excludeIds | Array | 否 | [] | 排除的衣物ID列表 |

**请求示例**
```json
{
  "clothingId": 1001,
  "topK": 5,
  "minScore": 0.75,
  "excludeIds": [1002, 1003]
}
```

**响应参数**

| 参数名 | 类型 | 说明 |
|--------|------|------|
| code | Integer | 状态码 |
| message | String | 提示信息 |
| data | Object | 响应数据 |
| data.sourceClothingId | Long | 参考衣物ID |
| data.sourceClothingName | String | 参考衣物名称 |
| data.results | Array | 相似衣物列表 |
| data.results[].clothingId | Long | 衣物ID |
| data.results[].name | String | 衣物名称 |
| data.results[].similarityScore | Float | 相似度分数 |
| data.results[].similarityFactors | Object | 相似因素分析 |

**响应示例**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "sourceClothingId": 1001,
    "sourceClothingName": "白色商务衬衫",
    "results": [
      {
        "clothingId": 1005,
        "name": "米白色雪纺衬衫",
        "similarityScore": 0.88,
        "similarityFactors": {
          "color": 0.95,
          "category": 1.0,
          "style": 0.75
        }
      }
    ]
  }
}
```

---

#### 4.2.3 获取检索历史

**请求信息**
- **Method:** GET
- **Path:** /api/v1/search/history
- **Authorization:** Bearer {token}

**请求参数（Query）**

| 参数名 | 类型 | 是否必填 | 默认值 | 说明 |
|--------|------|----------|--------|------|
| page | Integer | 否 | 1 | 页码 |
| size | Integer | 否 | 20 | 每页数量 |
| startDate | String | 否 | null | 开始日期（yyyy-MM-dd） |
| endDate | String | 否 | null | 结束日期（yyyy-MM-dd） |

**响应示例**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total": 50,
    "page": 1,
    "size": 20,
    "pages": 3,
    "list": [
      {
        "id": 123,
        "queryText": "找一件适合面试穿的白色衬衫",
        "queryType": 1,
        "resultCount": 5,
        "executionTimeMs": 245,
        "createdAt": "2026-02-03T10:30:00"
      }
    ]
  }
}
```

---

#### 4.2.4 删除检索记录

**请求信息**
- **Method:** DELETE
- **Path:** /api/v1/search/history/{id}
- **Authorization:** Bearer {token}

**路径参数**

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 检索记录ID |

**响应示例**
```json
{
  "code": 200,
  "message": "删除成功"
}
```

---

#### 4.2.5 获取检索建议

**请求信息**
- **Method:** GET
- **Path:** /api/v1/search/suggestions
- **Authorization:** Bearer {token}

**请求参数（Query）**

| 参数名 | 类型 | 是否必填 | 默认值 | 说明 |
|--------|------|----------|--------|------|
| keyword | String | 是 | - | 输入关键词 |
| limit | Integer | 否 | 5 | 建议数量 |

**响应示例**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "keyword": "白色",
    "suggestions": [
      "白色衬衫",
      "白色T恤",
      "白色连衣裙",
      "适合面试的白色衬衫",
      "白色外套"
    ]
  }
}
```

---

#### 4.2.6 获取检索偏好

**请求信息**
- **Method:** GET
- **Path:** /api/v1/search/preferences
- **Authorization:** Bearer {token}

**响应示例**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "userId": 10001,
    "preferredCategories": ["上装", "外套"],
    "preferredColors": ["白色", "黑色", "米色"],
    "preferredStyles": ["通勤", "简约"],
    "defaultTopK": 10,
    "minSimilarityScore": 0.6
  }
}
```

---

#### 4.2.7 更新检索偏好

**请求信息**
- **Method:** PUT
- **Path:** /api/v1/search/preferences
- **Content-Type:** application/json
- **Authorization:** Bearer {token}

**请求参数**

| 参数名 | 类型 | 是否必填 | 说明 |
|--------|------|----------|------|
| preferredCategories | Array | 否 | 偏好的衣物类别 |
| preferredColors | Array | 否 | 偏好的颜色 |
| preferredStyles | Array | 否 | 偏好的风格 |
| defaultTopK | Integer | 否 | 默认返回数量 |
| minSimilarityScore | Float | 否 | 最小相似度分数 |

**请求示例**
```json
{
  "preferredCategories": ["上装", "外套", "连衣裙"],
  "preferredColors": ["白色", "黑色"],
  "preferredStyles": ["通勤", "简约"],
  "defaultTopK": 15,
  "minSimilarityScore": 0.65
}
```

**响应示例**
```json
{
  "code": 200,
  "message": "更新成功",
  "data": {
    "userId": 10001,
    "preferredCategories": ["上装", "外套", "连衣裙"],
    "preferredColors": ["白色", "黑色"],
    "preferredStyles": ["通勤", "简约"],
    "defaultTopK": 15,
    "minSimilarityScore": 0.65,
    "updatedAt": "2026-02-03T10:30:00"
  }
}
```

---

## 5. 核心业务逻辑

### 5.1 语义检索流程

```
用户输入检索文本
       ↓
[参数校验] - 检查query长度、topK范围
       ↓
[文本向量化] - 调用Embedding模型生成向量
       ↓
[构建查询] - 组装SQL（向量相似度 + 属性过滤）
       ↓
[执行检索] - PostgreSQL向量检索
       ↓
[结果过滤] - 按minScore过滤，按相似度排序
       ↓
[记录历史] - 异步记录检索日志
       ↓
[返回结果] - 封装响应数据
```

### 5.2 向量生成策略

衣物向量的生成基于以下文本拼接：

```
衣物名称 + 类别 + 主色调 + 辅色调 + 材质 + 图案 + 
版型 + 风格标签 + 适用场合 + 适用季节 + 品牌
```

**示例：**
```
"白色商务衬衫 上装 白色 无 棉 纯色 修身 通勤,正式,简约 日常,通勤 春,夏,秋"
```

---

## 6. 性能优化建议

1. **向量索引优化**：根据数据量调整IVFFlat的lists参数
2. **查询缓存**：对热门检索词结果进行缓存
3. **异步记录**：检索历史记录使用异步方式写入
4. **分页加载**：相似衣物检索支持分页，避免一次性返回过多结果
5. **预热机制**：系统启动时预热Embedding模型

---

**文档版本**: v1.0  
**创建日期**: 2026-02-03  
**作者**: AI Assistant
