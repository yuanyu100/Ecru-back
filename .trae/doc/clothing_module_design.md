# 衣物模块设计文档

## 1. 数据库表设计

### 1.1 衣物表 (clothings)

```sql
-- 衣物主表
CREATE TABLE clothings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '衣物ID',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    name VARCHAR(100) NOT NULL COMMENT '衣物名称',
    
    -- 基础分类信息
    category VARCHAR(50) NOT NULL COMMENT '类别：上装/下装/外套/连衣裙/鞋履/包袋/配饰',
    sub_category VARCHAR(50) COMMENT '子类别：T恤/衬衫/卫衣/牛仔裤/西裤等',
    
    -- 颜色信息
    primary_color VARCHAR(50) COMMENT '主色调',
    primary_color_hex VARCHAR(7) COMMENT '主色十六进制值，如#FF5733',
    secondary_color VARCHAR(50) COMMENT '辅色调',
    secondary_color_hex VARCHAR(7) COMMENT '辅色十六进制值',
    
    -- 材质与图案
    material VARCHAR(50) COMMENT '材质：棉/麻/丝/羊毛/化纤/混纺/皮革/牛仔等',
    material_details VARCHAR(255) COMMENT '材质详细描述，如"95%棉+5%氨纶"',
    pattern VARCHAR(50) COMMENT '图案：纯色/条纹/格子/印花/波点/迷彩等',
    
    -- 版型与尺寸
    fit VARCHAR(50) COMMENT '版型：修身/宽松/直筒/A字/Oversize等',
    size VARCHAR(20) COMMENT '尺码：S/M/L/XL/均码等',
    
    -- 标签信息（JSON数组存储）
    style_tags JSON COMMENT '风格标签：["日系","通勤","简约"]',
    occasion_tags JSON COMMENT '适用场合：["日常","通勤","约会"]',
    season_tags JSON COMMENT '适用季节：["春","夏","秋","冬"]',
    
    -- 搭配与使用统计
    frequency_level TINYINT DEFAULT 3 COMMENT '搭配频率：1-5，1=很少穿，5=经常穿',
    wear_count INT DEFAULT 0 COMMENT '穿着次数',
    last_worn_at TIMESTAMP NULL COMMENT '最后穿着时间',
    
    -- 图片与存储
    image_url VARCHAR(255) NOT NULL COMMENT '主图URL',
    image_urls JSON COMMENT '多图URL数组',
    thumbnail_url VARCHAR(255) COMMENT '缩略图URL',
    
    -- 购买信息
    purchase_price DECIMAL(10,2) COMMENT '购买价格',
    purchase_date DATE COMMENT '购买日期',
    purchase_link VARCHAR(500) COMMENT '购买链接',
    brand VARCHAR(100) COMMENT '品牌',
    
    -- 状态与元数据
    is_deleted BOOLEAN DEFAULT FALSE COMMENT '是否删除（软删除）',
    source_type VARCHAR(20) DEFAULT 'manual' COMMENT '来源：manual手动/ai识别/import导入',
    ai_confidence DECIMAL(3,2) COMMENT 'AI识别置信度',
    
    -- 时间戳
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 外键约束
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- 索引
    INDEX idx_user_id (user_id),
    INDEX idx_category (category),
    INDEX idx_primary_color (primary_color),
    INDEX idx_material (material),
    INDEX idx_frequency (frequency_level),
    INDEX idx_deleted (is_deleted),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='衣物表';
```

### 1.2 衣物与风格标签关联表

```sql
-- 衣物与风格标签多对多关联表
CREATE TABLE clothing_style_tags (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    clothing_id BIGINT NOT NULL COMMENT '衣物ID',
    style_tag_id BIGINT NOT NULL COMMENT '风格标签ID',
    is_ai_tagged BOOLEAN DEFAULT FALSE COMMENT '是否AI自动标记',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (clothing_id) REFERENCES clothings(id) ON DELETE CASCADE,
    FOREIGN KEY (style_tag_id) REFERENCES style_tags(id) ON DELETE CASCADE,
    UNIQUE KEY uk_clothing_style (clothing_id, style_tag_id),
    INDEX idx_clothing_id (clothing_id),
    INDEX idx_style_tag_id (style_tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='衣物风格标签关联表';
```

### 1.3 衣物穿着记录表

```sql
-- 衣物穿着记录表（用于统计穿着次数和频率）
CREATE TABLE clothing_wear_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    clothing_id BIGINT NOT NULL COMMENT '衣物ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    worn_at DATE NOT NULL COMMENT '穿着日期',
    outfit_id BIGINT COMMENT '所属搭配方案ID',
    weather_condition VARCHAR(50) COMMENT '天气状况',
    temperature DECIMAL(4,1) COMMENT '温度',
    notes VARCHAR(500) COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (clothing_id) REFERENCES clothings(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_clothing_id (clothing_id),
    INDEX idx_user_id (user_id),
    INDEX idx_worn_at (worn_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='衣物穿着记录表';
```

### 1.4 衣物向量表（使用RAG模块）

**注意**：衣物向量存储统一使用 RAG 模块中的 `clothing_embeddings` 表（PostgreSQL + pgvector），此模块不再单独定义。

---

## 2. RESTful API 接口文档

### 2.1 接口概览

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| POST | /api/v1/clothings | 创建衣物 | 是 |
| GET | /api/v1/clothings | 查询衣物列表 | 是 |
| GET | /api/v1/clothings/{id} | 获取衣物详情 | 是 |
| PUT | /api/v1/clothings/{id} | 更新衣物 | 是 |
| DELETE | /api/v1/clothings/{id} | 删除衣物 | 是 |
| POST | /api/v1/clothings/{id}/recognize | AI识别衣物 | 是 |
| PUT | /api/v1/clothings/{id}/frequency | 设置搭配频率 | 是 |
| POST | /api/v1/clothings/{id}/wear | 记录穿着 | 是 |
| GET | /api/v1/clothings/statistics | 获取衣物统计 | 是 |

---

### 2.2 接口详情

#### 2.2.1 创建衣物

**请求信息**
- **Method**: POST
- **Path**: `/api/v1/clothings`
- **Content-Type**: `multipart/form-data` 或 `application/json`

**请求参数**

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| image | File | 是 | 衣物图片文件（form-data方式）|
| imageUrl | String | 否 | 图片URL（json方式，与image二选一）|
| name | String | 否 | 衣物名称，不传则AI自动生成 |
| category | String | 否 | 类别，不传则AI识别 |
| primaryColor | String | 否 | 主色调 |
| material | String | 否 | 材质 |
| styleTags | Array | 否 | 风格标签 |
| occasionTags | Array | 否 | 适用场合标签 |
| seasonTags | Array | 否 | 适用季节标签 |
| purchasePrice | Decimal | 否 | 购买价格 |
| brand | String | 否 | 品牌 |
| notes | String | 否 | 备注 |
| autoRecognize | Boolean | 否 | 是否自动AI识别，默认true |

**请求示例（form-data）**
```http
POST /api/v1/clothings
Authorization: Bearer {token}
Content-Type: multipart/form-data

image: [文件]
name: 白色纯棉T恤
category: 上装
styleTags: ["简约", "通勤"]
```

**请求示例（json + 已有图片URL）**
```json
{
  "imageUrl": "https://example.com/image.jpg",
  "name": "白色纯棉T恤",
  "category": "上装",
  "primaryColor": "白色",
  "material": "棉",
  "styleTags": ["简约", "通勤"],
  "occasionTags": ["日常", "通勤"],
  "seasonTags": ["春", "夏", "秋"],
  "autoRecognize": true
}
```

**响应示例（成功）**
```json
{
  "code": 201,
  "message": "创建成功",
  "data": {
    "id": 10001,
    "userId": 100,
    "name": "白色纯棉T恤",
    "category": "上装",
    "subCategory": "T恤",
    "primaryColor": "白色",
    "primaryColorHex": "#FFFFFF",
    "secondaryColor": null,
    "material": "棉",
    "pattern": "纯色",
    "fit": "宽松",
    "styleTags": ["简约", "通勤"],
    "occasionTags": ["日常", "通勤"],
    "seasonTags": ["春", "夏", "秋"],
    "frequencyLevel": 3,
    "wearCount": 0,
    "imageUrl": "https://storage.example.com/clothings/10001.jpg",
    "thumbnailUrl": "https://storage.example.com/clothings/10001_thumb.jpg",
    "brand": null,
    "sourceType": "ai",
    "aiConfidence": 0.92,
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-01-15T10:30:00Z"
  }
}
```

**响应示例（AI识别结果）**
```json
{
  "code": 201,
  "message": "创建成功",
  "data": {
    "id": 10001,
    "name": "白色纯棉T恤",
    "aiRecognition": {
      "detected": true,
      "confidence": 0.92,
      "results": {
        "category": "上装",
        "subCategory": "T恤",
        "primaryColor": "白色",
        "material": "棉",
        "pattern": "纯色",
        "fit": "宽松",
        "styleTags": ["简约", "基础款"],
        "occasionTags": ["日常", "通勤"],
        "seasonTags": ["春", "夏", "秋"]
      }
    }
  }
}
```

---

#### 2.2.2 查询衣物列表

**请求信息**
- **Method**: GET
- **Path**: `/api/v1/clothings`

**查询参数**

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| page | Integer | 否 | 页码，默认1 |
| size | Integer | 否 | 每页数量，默认20，最大100 |
| category | String | 否 | 类别筛选 |
| primaryColor | String | 否 | 主色调筛选 |
| material | String | 否 | 材质筛选 |
| styleTag | String | 否 | 风格标签筛选 |
| occasionTag | String | 否 | 场合标签筛选 |
| seasonTag | String | 否 | 季节标签筛选 |
| keyword | String | 否 | 关键词搜索（名称、品牌）|
| sortBy | String | 否 | 排序字段：createdAt/updatedAt/frequencyLevel/wearCount |
| sortOrder | String | 否 | 排序方向：asc/desc，默认desc |
| minFrequency | Integer | 否 | 最小搭配频率 |
| maxFrequency | Integer | 否 | 最大搭配频率 |

**请求示例**
```http
GET /api/v1/clothings?page=1&size=20&category=上装&styleTag=通勤&sortBy=frequencyLevel&sortOrder=desc
Authorization: Bearer {token}
```

**响应示例**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 156,
    "page": 1,
    "size": 20,
    "pages": 8,
    "items": [
      {
        "id": 10001,
        "name": "白色纯棉T恤",
        "category": "上装",
        "subCategory": "T恤",
        "primaryColor": "白色",
        "primaryColorHex": "#FFFFFF",
        "material": "棉",
        "pattern": "纯色",
        "styleTags": ["简约", "通勤"],
        "frequencyLevel": 5,
        "wearCount": 25,
        "imageUrl": "https://storage.example.com/clothings/10001.jpg",
        "thumbnailUrl": "https://storage.example.com/clothings/10001_thumb.jpg",
        "createdAt": "2024-01-15T10:30:00Z"
      },
      {
        "id": 10002,
        "name": "深蓝色牛仔裤",
        "category": "下装",
        "subCategory": "牛仔裤",
        "primaryColor": "深蓝色",
        "primaryColorHex": "#1a237e",
        "material": "牛仔",
        "pattern": "纯色",
        "styleTags": ["复古", "休闲"],
        "frequencyLevel": 4,
        "wearCount": 18,
        "imageUrl": "https://storage.example.com/clothings/10002.jpg",
        "thumbnailUrl": "https://storage.example.com/clothings/10002_thumb.jpg",
        "createdAt": "2024-01-10T14:20:00Z"
      }
    ]
  }
}
```

---

#### 2.2.3 获取衣物详情

**请求信息**
- **Method**: GET
- **Path**: `/api/v1/clothings/{id}`

**路径参数**

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| id | Long | 是 | 衣物ID |

**请求示例**
```http
GET /api/v1/clothings/10001
Authorization: Bearer {token}
```

**响应示例**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 10001,
    "userId": 100,
    "name": "白色纯棉T恤",
    "category": "上装",
    "subCategory": "T恤",
    "primaryColor": "白色",
    "primaryColorHex": "#FFFFFF",
    "secondaryColor": null,
    "secondaryColorHex": null,
    "material": "棉",
    "materialDetails": "100%纯棉",
    "pattern": "纯色",
    "fit": "宽松",
    "size": "L",
    "styleTags": ["简约", "通勤", "基础款"],
    "occasionTags": ["日常", "通勤", "休闲"],
    "seasonTags": ["春", "夏", "秋"],
    "frequencyLevel": 5,
    "wearCount": 25,
    "lastWornAt": "2024-02-01",
    "imageUrl": "https://storage.example.com/clothings/10001.jpg",
    "imageUrls": [
      "https://storage.example.com/clothings/10001.jpg",
      "https://storage.example.com/clothings/10001_back.jpg"
    ],
    "thumbnailUrl": "https://storage.example.com/clothings/10001_thumb.jpg",
    "purchasePrice": 129.00,
    "purchaseDate": "2023-06-15",
    "purchaseLink": null,
    "brand": "优衣库",
    "isDeleted": false,
    "sourceType": "ai",
    "aiConfidence": 0.92,
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-02-01T08:15:00Z",
    "wearHistory": {
      "total": 25,
      "recent": [
        {"date": "2024-02-01", "outfitId": 5001},
        {"date": "2024-01-28", "outfitId": 4998},
        {"date": "2024-01-25", "outfitId": 4995}
      ]
    }
  }
}
```

---

#### 2.2.4 更新衣物

**请求信息**
- **Method**: PUT
- **Path**: `/api/v1/clothings/{id}`
- **Content-Type**: `application/json`

**路径参数**

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| id | Long | 是 | 衣物ID |

**请求体参数**

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| name | String | 否 | 衣物名称 |
| category | String | 否 | 类别 |
| subCategory | String | 否 | 子类别 |
| primaryColor | String | 否 | 主色调 |
| secondaryColor | String | 否 | 辅色调 |
| material | String | 否 | 材质 |
| materialDetails | String | 否 | 材质详情 |
| pattern | String | 否 | 图案 |
| fit | String | 否 | 版型 |
| size | String | 否 | 尺码 |
| styleTags | Array | 否 | 风格标签 |
| occasionTags | Array | 否 | 适用场合标签 |
| seasonTags | Array | 否 | 适用季节标签 |
| purchasePrice | Decimal | 否 | 购买价格 |
| purchaseDate | String | 否 | 购买日期（YYYY-MM-DD）|
| brand | String | 否 | 品牌 |

**请求示例**
```http
PUT /api/v1/clothings/10001
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "白色纯棉基础T恤",
  "styleTags": ["简约", "通勤", "基础款", "百搭"],
  "frequencyLevel": 5,
  "brand": "优衣库"
}
```

**响应示例**
```json
{
  "code": 200,
  "message": "更新成功",
  "data": {
    "id": 10001,
    "name": "白色纯棉基础T恤",
    "styleTags": ["简约", "通勤", "基础款", "百搭"],
    "frequencyLevel": 5,
    "brand": "优衣库",
    "updatedAt": "2024-02-03T16:45:00Z"
  }
}
```

---

#### 2.2.5 删除衣物

**请求信息**
- **Method**: DELETE
- **Path**: `/api/v1/clothings/{id}`

**路径参数**

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| id | Long | 是 | 衣物ID |

**查询参数**

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| force | Boolean | 否 | 是否硬删除，默认false（软删除）|

**请求示例**
```http
DELETE /api/v1/clothings/10001
Authorization: Bearer {token}
```

**响应示例（软删除）**
```json
{
  "code": 200,
  "message": "删除成功",
  "data": {
    "id": 10001,
    "isDeleted": true,
    "deletedAt": "2024-02-03T16:50:00Z"
  }
}
```

---

#### 2.2.6 AI识别衣物

**请求信息**
- **Method**: POST
- **Path**: `/api/v1/clothings/{id}/recognize`

**路径参数**

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| id | Long | 是 | 衣物ID |

**请求示例**
```http
POST /api/v1/clothings/10001/recognize
Authorization: Bearer {token}
```

**响应示例**
```json
{
  "code": 200,
  "message": "识别成功",
  "data": {
    "clothingId": 10001,
    "recognition": {
      "confidence": 0.94,
      "results": {
        "category": "上装",
        "subCategory": "衬衫",
        "primaryColor": "浅蓝色",
        "primaryColorHex": "#ADD8E6",
        "secondaryColor": "白色",
        "secondaryColorHex": "#FFFFFF",
        "material": "棉",
        "pattern": "条纹",
        "fit": "修身",
        "styleTags": ["通勤", "商务", "简约"],
        "occasionTags": ["通勤", "正式", "商务会议"],
        "seasonTags": ["春", "夏", "秋"]
      },
      "suggestions": {
        "name": "浅蓝色条纹商务衬衫",
        "description": "适合职场通勤的修身条纹衬衫"
      }
    },
    "updatedFields": [
      "category",
      "subCategory", 
      "primaryColor",
      "pattern",
      "styleTags",
      "occasionTags"
    ]
  }
}
```

---

#### 2.2.7 设置搭配频率

**请求信息**
- **Method**: PUT
- **Path**: `/api/v1/clothings/{id}/frequency`
- **Content-Type**: `application/json`

**路径参数**

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| id | Long | 是 | 衣物ID |

**请求体**

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| frequencyLevel | Integer | 是 | 搭配频率：1-5，1=很少穿，5=经常穿 |

**请求示例**
```http
PUT /api/v1/clothings/10001/frequency
Authorization: Bearer {token}
Content-Type: application/json

{
  "frequencyLevel": 5
}
```

**响应示例**
```json
{
  "code": 200,
  "message": "设置成功",
  "data": {
    "id": 10001,
    "frequencyLevel": 5,
    "frequencyLabel": "经常穿",
    "updatedAt": "2024-02-03T17:00:00Z"
  }
}
```

**频率等级说明**

| 等级 | 标签 | 说明 |
|------|------|------|
| 1 | 很少穿 | 几乎不穿，考虑捐赠 |
| 2 | 偶尔穿 | 特殊场合才穿 |
| 3 | 有时穿 | 正常频率 |
| 4 | 经常穿 | 每周都会穿 |
| 5 | 经常穿 | 高频穿着，百搭单品 |

---

#### 2.2.8 记录穿着

**请求信息**
- **Method**: POST
- **Path**: `/api/v1/clothings/{id}/wear`
- **Content-Type**: `application/json`

**路径参数**

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| id | Long | 是 | 衣物ID |

**请求体**

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| wornAt | String | 否 | 穿着日期（YYYY-MM-DD），默认今天 |
| outfitId | Long | 否 | 所属搭配方案ID |
| weatherCondition | String | 否 | 天气状况 |
| temperature | Decimal | 否 | 温度 |
| notes | String | 否 | 备注 |

**请求示例**
```http
POST /api/v1/clothings/10001/wear
Authorization: Bearer {token}
Content-Type: application/json

{
  "wornAt": "2024-02-03",
  "outfitId": 5005,
  "weatherCondition": "晴",
  "temperature": 18.5,
  "notes": "参加朋友聚会"
}
```

**响应示例**
```json
{
  "code": 200,
  "message": "记录成功",
  "data": {
    "clothingId": 10001,
    "wearCount": 26,
    "lastWornAt": "2024-02-03",
    "logId": 100056
  }
}
```

---

#### 2.2.9 获取衣物统计

**请求信息**
- **Method**: GET
- **Path**: `/api/v1/clothings/statistics`

**查询参数**

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| period | String | 否 | 统计周期：week/month/year/all，默认all |

**请求示例**
```http
GET /api/v1/clothings/statistics?period=month
Authorization: Bearer {token}
```

**响应示例**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "overview": {
      "totalClothings": 156,
      "totalWornThisPeriod": 42,
      "mostWornClothingId": 10001,
      "mostWornCount": 8
    },
    "byCategory": [
      {"category": "上装", "count": 68, "percentage": 43.6},
      {"category": "下装", "count": 45, "percentage": 28.8},
      {"category": "外套", "count": 28, "percentage": 17.9},
      {"category": "鞋履", "count": 15, "percentage": 9.6}
    ],
    "byColor": [
      {"color": "白色", "count": 32, "percentage": 20.5},
      {"color": "黑色", "count": 28, "percentage": 17.9},
      {"color": "蓝色", "count": 24, "percentage": 15.4}
    ],
    "byFrequency": [
      {"level": 5, "label": "经常穿", "count": 45},
      {"level": 4, "label": "经常穿", "count": 38},
      {"level": 3, "label": "有时穿", "count": 42},
      {"level": 2, "label": "偶尔穿", "count": 21},
      {"level": 1, "label": "很少穿", "count": 10}
    ],
    "wearTrend": [
      {"date": "2024-01-28", "count": 3},
      {"date": "2024-01-29", "count": 5},
      {"date": "2024-01-30", "count": 2},
      {"date": "2024-01-31", "count": 4},
      {"date": "2024-02-01", "count": 6},
      {"date": "2024-02-02", "count": 3},
      {"date": "2024-02-03", "count": 5}
    ]
  }
}
```

---

## 3. 错误码定义

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 201 | 创建成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 禁止访问（非自己的衣物）|
| 404 | 衣物不存在 |
| 409 | 资源冲突 |
| 422 | 无法处理的实体（AI识别失败等）|
| 500 | 服务器内部错误 |

---

## 4. 通用响应格式

**成功响应**
```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

**错误响应**
```json
{
  "code": 404,
  "message": "衣物不存在",
  "data": null,
  "error": {
    "detail": "ID为10001的衣物不存在或已被删除",
    "timestamp": "2024-02-03T17:10:00Z"
  }
}
```

**分页响应**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 156,
    "page": 1,
    "size": 20,
    "pages": 8,
    "items": [ ... ]
  }
}
```

---

## 5. 数据字典

### 5.1 衣物类别 (category)

| 值 | 说明 | 子类别示例 |
|----|------|-----------|
| 上装 | 上身衣物 | T恤、衬衫、卫衣、毛衣、背心 |
| 下装 | 下身衣物 | 牛仔裤、西裤、短裤、裙子 |
| 外套 | 外层衣物 | 风衣、夹克、大衣、羽绒服、西装 |
| 连衣裙 | 连身衣物 | 长裙、短裙、吊带裙、旗袍 |
| 鞋履 |  footwear | 运动鞋、皮鞋、靴子、凉鞋 |
| 包袋 | 包包 | 手提包、双肩包、钱包、旅行包 |
| 配饰 | 装饰品 | 帽子、围巾、腰带、首饰、眼镜 |

### 5.2 颜色枚举

| 颜色名 | 推荐Hex值 |
|--------|----------|
| 白色 | #FFFFFF |
| 黑色 | #000000 |
| 灰色 | #808080 |
| 红色 | #FF0000 |
| 蓝色 | #0000FF |
| 深蓝色 | #1a237e |
| 浅蓝色 | #ADD8E6 |
| 绿色 | #008000 |
| 黄色 | #FFFF00 |
| 粉色 | #FFC0CB |
| 紫色 | #800080 |
| 棕色 | #8B4513 |
| 米色 | #F5F5DC |
| 卡其色 | #C3B091 |
| 橙色 | #FFA500 |

### 5.3 材质枚举

| 材质 | 特性 |
|------|------|
| 棉 | 透气、吸汗、舒适 |
| 麻 | 透气、凉爽、易皱 |
| 丝 | 光滑、透气、高档 |
| 羊毛 | 保暖、弹性好 |
| 化纤 | 耐用、易打理 |
| 混纺 | 综合多种材质优点 |
| 皮革 | 耐用、防水、时尚 |
| 牛仔 | 耐磨、百搭 |

### 5.4 图案枚举

- 纯色
- 条纹（横条纹、竖条纹）
- 格子
- 印花（碎花、大花）
- 波点
- 迷彩
- 几何
- 字母/Logo
- 动物纹
- 扎染

### 5.5 版型枚举

- 修身
- 宽松
- 直筒
- A字
- Oversize
- 紧身
- 高腰
- 低腰
