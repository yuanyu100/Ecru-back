# 风格偏好模块设计文档

## 1. 数据库设计

### 1.1 表结构说明

#### 1.1.0 风格标签表 (style_tags)
存储风格标签信息，是风格偏好模块的核心表

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键，自增 |
| name | VARCHAR(50) | 标签名称（如"森系"、"韩系简约"） |
| category | VARCHAR(50) | 风格大类（日系/韩系/欧美/通勤/运动/复古） |
| is_preset | BOOLEAN | 是否预设标签 |
| description | VARCHAR(255) | 标签描述 |
| usage_count | INT | 使用次数 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

#### 1.1.1 风格图片表 (style_images)
存储爬虫采集的风格图片信息

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键，自增 |
| image_url | VARCHAR(500) | 图片URL |
| title | VARCHAR(200) | 图片标题/描述 |
| source | VARCHAR(50) | 来源平台（taobao/xiaohongshu） |
| source_url | VARCHAR(500) | 原始链接 |
| price | DECIMAL(10,2) | 价格（可选） |
| style_category | VARCHAR(50) | 风格大类（日系/韩系/欧美等） |
| embedding | VECTOR(1536) | 图片向量（PostgreSQL pgvector） |
| is_active | BOOLEAN | 是否可用 |
| created_at | TIMESTAMP | 创建时间 |

#### 1.1.2 风格图片标签关联表 (style_image_tags)
图片与风格标签的多对多关联

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键，自增 |
| image_id | BIGINT | 图片ID |
| style_tag_id | BIGINT | 风格标签ID |
| confidence | DECIMAL(3,2) | AI识别置信度 |

#### 1.1.3 用户风格偏好标记表 (user_style_preference_logs)
用户对图片的 like/dislike 标记记录

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键，自增 |
| user_id | BIGINT | 用户ID |
| image_id | BIGINT | 图片ID |
| preference_type | TINYINT | 偏好类型：1-like, 2-dislike, 0-skip |
| created_at | TIMESTAMP | 创建时间 |

#### 1.1.4 用户风格画像表 (user_style_profiles)
聚合计算的用户风格偏好分数

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键，自增 |
| user_id | BIGINT | 用户ID |
| style_tag_id | BIGINT | 风格标签ID |
| preference_score | DECIMAL(5,4) | 偏好分数（-1.0 ~ 1.0） |
| interaction_count | INT | 交互次数 |
| updated_at | TIMESTAMP | 更新时间 |

---

### 1.2 SQL 语句

```sql
-- ================================================
-- 风格偏好模块 - 数据库表结构
-- ================================================

-- 0. 风格标签表（核心表）
CREATE TABLE style_tags (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '标签ID',
    name VARCHAR(50) NOT NULL COMMENT '标签名称',
    category VARCHAR(50) COMMENT '风格大类：日系/韩系/欧美/通勤/运动/复古',
    is_preset BOOLEAN DEFAULT TRUE COMMENT '是否预设标签',
    description VARCHAR(255) COMMENT '标签描述',
    usage_count INT DEFAULT 0 COMMENT '使用次数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_name_category (name, category),
    INDEX idx_category (category),
    INDEX idx_is_preset (is_preset),
    INDEX idx_usage_count (usage_count DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风格标签表';

-- 1. 风格图片表
CREATE TABLE style_images (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    image_url VARCHAR(500) NOT NULL COMMENT '图片URL',
    title VARCHAR(200) COMMENT '图片标题/描述',
    source VARCHAR(50) COMMENT '来源平台：taobao/xiaohongshu',
    source_url VARCHAR(500) COMMENT '原始链接',
    price DECIMAL(10,2) COMMENT '价格',
    style_category VARCHAR(50) COMMENT '风格大类：日系/韩系/欧美/通勤/运动/复古',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否可用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_style_category (style_category),
    INDEX idx_source (source),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风格图片表';

-- 2. 风格图片标签关联表
CREATE TABLE style_image_tags (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    image_id BIGINT NOT NULL COMMENT '图片ID',
    style_tag_id BIGINT NOT NULL COMMENT '风格标签ID',
    confidence DECIMAL(3,2) DEFAULT 1.00 COMMENT 'AI识别置信度',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (image_id) REFERENCES style_images(id) ON DELETE CASCADE,
    FOREIGN KEY (style_tag_id) REFERENCES style_tags(id) ON DELETE CASCADE,
    UNIQUE KEY uk_image_tag (image_id, style_tag_id),
    INDEX idx_image_id (image_id),
    INDEX idx_style_tag_id (style_tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风格图片标签关联表';

-- 3. 用户风格偏好标记表
CREATE TABLE user_style_preference_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    image_id BIGINT NOT NULL COMMENT '图片ID',
    preference_type TINYINT NOT NULL COMMENT '偏好类型：1-like, 2-dislike, 0-skip',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (image_id) REFERENCES style_images(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_image (user_id, image_id),
    INDEX idx_user_id (user_id),
    INDEX idx_image_id (image_id),
    INDEX idx_preference_type (preference_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户风格偏好标记表';

-- 4. 用户风格画像表
CREATE TABLE user_style_profiles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    style_tag_id BIGINT NOT NULL COMMENT '风格标签ID',
    preference_score DECIMAL(5,4) DEFAULT 0.0000 COMMENT '偏好分数（-1.0 ~ 1.0）',
    interaction_count INT DEFAULT 0 COMMENT '交互次数',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (style_tag_id) REFERENCES style_tags(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_style (user_id, style_tag_id),
    INDEX idx_user_id (user_id),
    INDEX idx_style_tag_id (style_tag_id),
    INDEX idx_preference_score (preference_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户风格画像表';

-- ============================================
-- PostgreSQL pgvector 扩展表（图片向量存储）
-- ============================================

-- 启用 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 风格图片向量表
CREATE TABLE style_image_embeddings (
    id BIGSERIAL PRIMARY KEY,
    image_id BIGINT NOT NULL UNIQUE,
    embedding VECTOR(1536),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (image_id) REFERENCES style_images(id) ON DELETE CASCADE
);

-- 创建向量索引（IVFFlat）
CREATE INDEX ON style_image_embeddings USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- ============================================
-- 初始化数据 - 风格标签（扩展现有 style_tags 表）
-- ============================================

-- 预设风格标签（如果尚未添加）
INSERT INTO style_tags (name, category, is_preset) VALUES
-- 日系
('森系', '日系', TRUE),
('原宿', '日系', TRUE),
('通勤日系', '日系', TRUE),
('日系简约', '日系', TRUE),
-- 韩系
('韩系简约', '韩系', TRUE),
('韩系甜美', '韩系', TRUE),
('韩系街头', '韩系', TRUE),
('韩系通勤', '韩系', TRUE),
-- 欧美
('欧美极简', '欧美', TRUE),
('欧美复古', '欧美', TRUE),
('欧美街头', '欧美', TRUE),
('欧美商务', '欧美', TRUE),
-- 通勤
('正式通勤', '通勤', TRUE),
('轻商务', '通勤', TRUE),
('休闲通勤', '通勤', TRUE),
-- 运动
('运动休闲', '运动', TRUE),
('户外运动', '运动', TRUE),
('瑜伽健身', '运动', TRUE),
-- 复古
('港风复古', '复古', TRUE),
('法式复古', '复古', TRUE),
('Vintage', '复古', TRUE),
('美式复古', '复古', TRUE);

```

---

## 2. RESTful 接口文档

### 2.1 接口概览

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/style/images | 获取风格图片列表 |
| POST | /api/style/preferences | 标记图片偏好 |
| GET | /api/style/preferences | 获取用户偏好历史 |
| GET | /api/style/profile | 获取用户风格画像 |
| GET | /api/style/recommendations | 获取个性化推荐 |
| GET | /api/style/tags | 获取风格标签列表 |
| GET | /api/style/tags/{id}/images | 获取指定标签下的图片 |

---

### 2.2 接口详情

#### 2.2.1 获取风格图片列表

**接口信息**
- **URL**: `/api/style/images`
- **Method**: `GET`
- **认证**: 需要 JWT Token

**请求参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| category | String | 否 | 风格大类筛选（日系/韩系/欧美/通勤/运动/复古） |
| tag_id | Long | 否 | 标签ID筛选 |
| exclude_viewed | Boolean | 否 | 是否排除已标记的图片，默认true |
| limit | Integer | 否 | 返回数量，默认20，最大50 |
| offset | Integer | 否 | 偏移量，默认0 |

**响应示例**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 150,
    "list": [
      {
        "id": 1001,
        "image_url": "https://example.com/images/style/1001.jpg",
        "title": "日系森系连衣裙",
        "source": "xiaohongshu",
        "price": 299.00,
        "style_category": "日系",
        "tags": [
          {
            "id": 1,
            "name": "森系",
            "category": "日系"
          },
          {
            "id": 5,
            "name": "日系简约",
            "category": "日系"
          }
        ]
      }
    ]
  }
}
```

---

#### 2.2.2 标记图片偏好

**接口信息**
- **URL**: `/api/style/preferences`
- **Method**: `POST`
- **认证**: 需要 JWT Token
- **Content-Type**: `application/json`

**请求参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| image_id | Long | 是 | 图片ID |
| preference_type | Integer | 是 | 偏好类型：1-like, 2-dislike, 0-skip |

**请求示例**
```json
{
  "image_id": 1001,
  "preference_type": 1
}
```

**响应示例**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 5001,
    "image_id": 1001,
    "preference_type": 1,
    "created_at": "2025-02-03T10:30:00"
  }
}
```

**错误码**

| 错误码 | 说明 |
|--------|------|
| 400 | 参数错误 |
| 404 | 图片不存在 |
| 409 | 已标记过该图片 |

---

#### 2.2.3 获取用户偏好历史

**接口信息**
- **URL**: `/api/style/preferences`
- **Method**: `GET`
- **认证**: 需要 JWT Token

**请求参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| preference_type | Integer | 否 | 筛选类型：1-like, 2-dislike |
| limit | Integer | 否 | 默认20 |
| offset | Integer | 否 | 默认0 |

**响应示例**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 45,
    "list": [
      {
        "id": 5001,
        "image": {
          "id": 1001,
          "image_url": "https://example.com/images/style/1001.jpg",
          "title": "日系森系连衣裙",
          "style_category": "日系"
        },
        "preference_type": 1,
        "created_at": "2025-02-03T10:30:00"
      }
    ]
  }
}
```

---

#### 2.2.4 获取用户风格画像

**接口信息**
- **URL**: `/api/style/profile`
- **Method**: `GET`
- **认证**: 需要 JWT Token

**响应示例**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "user_id": 10001,
    "total_interactions": 86,
    "top_styles": [
      {
        "style_tag": {
          "id": 1,
          "name": "森系",
          "category": "日系"
        },
        "preference_score": 0.85,
        "interaction_count": 15
      },
      {
        "style_tag": {
          "id": 5,
          "name": "日系简约",
          "category": "日系"
        },
        "preference_score": 0.72,
        "interaction_count": 12
      }
    ],
    "style_distribution": {
      "日系": 0.45,
      "韩系": 0.20,
      "欧美": 0.15,
      "通勤": 0.10,
      "运动": 0.05,
      "复古": 0.05
    }
  }
}
```

---

#### 2.2.5 获取个性化推荐

**接口信息**
- **URL**: `/api/style/recommendations`
- **Method**: `GET`
- **认证**: 需要 JWT Token

**请求参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| limit | Integer | 否 | 推荐数量，默认20 |
| strategy | String | 否 | 推荐策略：hybrid(混合)/content(内容)/collaborative(协同)，默认hybrid |

**响应示例**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "recommendations": [
      {
        "id": 2001,
        "image_url": "https://example.com/images/style/2001.jpg",
        "title": "森系棉麻长裙",
        "source": "taobao",
        "price": 189.00,
        "style_category": "日系",
        "tags": [
          {
            "id": 1,
            "name": "森系",
            "category": "日系"
          }
        ],
        "recommend_reason": "基于你对森系风格的偏好",
        "similarity_score": 0.92
      }
    ],
    "based_on": {
      "top_preferred_tags": ["森系", "日系简约"],
      "recommendation_strategy": "hybrid"
    }
  }
}
```

---

#### 2.2.6 获取风格标签列表

**接口信息**
- **URL**: `/api/style/tags`
- **Method**: `GET`
- **认证**: 需要 JWT Token

**请求参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| category | String | 否 | 按大类筛选 |

**响应示例**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "categories": [
      {
        "name": "日系",
        "tags": [
          {
            "id": 1,
            "name": "森系",
            "image_count": 156
          },
          {
            "id": 2,
            "name": "原宿",
            "image_count": 89
          }
        ]
      },
      {
        "name": "韩系",
        "tags": [
          {
            "id": 6,
            "name": "韩系简约",
            "image_count": 234
          }
        ]
      }
    ]
  }
}
```

---

#### 2.2.7 获取指定标签下的图片

**接口信息**
- **URL**: `/api/style/tags/{id}/images`
- **Method**: `GET`
- **认证**: 需要 JWT Token

**路径参数**

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 标签ID |

**请求参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| limit | Integer | 否 | 默认20 |
| offset | Integer | 否 | 默认0 |

**响应示例**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "tag": {
      "id": 1,
      "name": "森系",
      "category": "日系"
    },
    "total": 156,
    "list": [
      {
        "id": 1001,
        "image_url": "https://example.com/images/style/1001.jpg",
        "title": "日系森系连衣裙",
        "price": 299.00
      }
    ]
  }
}
```

---

## 3. 核心业务流程

### 3.1 用户偏好收集流程

```
用户浏览图片流
    ↓
对图片进行标记 (like/dislike/skip)
    ↓
保存到 user_style_preference_logs
    ↓
异步更新用户风格画像 (user_style_profiles)
    ↓
基于画像生成个性化推荐
```

### 3.2 推荐算法逻辑

**混合推荐策略 (Hybrid)**
1. **内容推荐 (60%权重)**
   - 基于用户高偏好标签
   - 使用图片向量相似度
   - 匹配风格标签

2. **协同过滤 (40%权重)**
   - 找到相似偏好的用户
   - 推荐他们喜欢的图片

**偏好分数计算**
```
preference_score = (like_count - dislike_count * 1.5) / total_interactions
范围: -1.0 (完全不喜欢) ~ 1.0 (非常喜欢)
```

---

## 4. 数据流图

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   爬虫采集系统    │────▶│   style_images  │────▶│  AI标签识别     │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                                                          │
                                                          ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   推荐系统       │◀────│  用户风格画像    │◀────│ style_image_tags│
│                 │     │ user_style_     │     └─────────────────┘
│                 │     │   profiles      │
└────────┬────────┘     └─────────────────┘
         │
         ▼
┌─────────────────┐     ┌─────────────────┐
│  用户交互记录    │────▶│  用户偏好标记    │
│ recommendations │     │ preference_logs │
└─────────────────┘     └─────────────────┘
```

---

## 5. 注意事项

1. **图片去重**: 采集时需进行图片特征去重，避免重复入库
2. **向量存储**: 图片向量存储在 PostgreSQL pgvector 中，用于相似度计算
3. **画像更新**: 用户偏好标记后异步更新画像，避免阻塞主流程
4. **冷启动**: 新用户推荐热门图片，收集足够数据后切换个性化推荐
5. **数据隐私**: 用户偏好数据仅用于本系统推荐，不对外暴露
