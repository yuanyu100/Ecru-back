# 搭配建议模块设计文档

## 1. 模块概述

搭配建议模块是AI智能衣橱系统的核心功能，通过Agent工作流为用户提供个性化的穿搭建议。模块整合图像分析、天气数据、用户衣橱和风格偏好，生成完整的搭配方案。

## 2. 数据库设计

### 2.1 表结构概览

| 表名 | 说明 | 用途 |
|------|------|------|
| outfit_advice_records | 搭配建议记录表 | 存储每次搭配建议的完整记录 |
| outfit_items | 搭配单品关联表 | 记录每次搭配使用的衣物 |
| outfit_feedback | 搭配反馈表 | 用户对搭配方案的反馈评分 |
| user_style_profiles | 用户风格档案表 | 存储用户气质类型测试结果 |

### 2.2 详细表结构

#### 2.2.1 搭配建议记录表 (outfit_advice_records)

```sql
CREATE TABLE outfit_advice_records (
    -- 主键
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    
    -- 关联信息
    user_id BIGINT NOT NULL COMMENT '用户ID',
    
    -- 输入信息
    input_type TINYINT NOT NULL DEFAULT 1 COMMENT '输入类型: 1-上传照片, 2-文字描述, 3-智能推荐',
    input_image_url VARCHAR(500) COMMENT '用户上传的穿搭照片URL',
    input_description TEXT COMMENT '用户文字描述',
    
    -- 环境信息(MCP获取)
    location VARCHAR(100) COMMENT '地理位置',
    temperature DECIMAL(4,1) COMMENT '温度(摄氏度)',
    weather_condition VARCHAR(50) COMMENT '天气状况: 晴/多云/雨/雪等',
    season VARCHAR(20) COMMENT '季节: 春/夏/秋/冬',
    time_of_day VARCHAR(20) COMMENT '时段: 早晨/上午/下午/晚上',
    
    -- AI分析结果
    detected_items JSON COMMENT '识别到的单品列表 [{"item": "上衣", "color": "白色", "category": "上装"}]',
    detected_style VARCHAR(100) COMMENT '识别到的风格',
    color_analysis TEXT COMMENT '色彩分析结果',
    
    -- 搭配方案
    outfit_name VARCHAR(200) COMMENT '搭配方案名称',
    outfit_description TEXT COMMENT '搭配方案描述',
    reasoning TEXT COMMENT '搭配思路说明',
    fashion_suggestions TEXT COMMENT '时尚建议',
    
    -- 推荐商品(JSON存储)
    purchase_recommendations JSON COMMENT '推荐购买商品 [{"name": "", "reason": "", "link": ""}]',
    
    -- 元数据
    occasion VARCHAR(50) COMMENT '适用场合',
    suitability_score DECIMAL(3,2) COMMENT '场合适配度评分 0.00-1.00',
    
    -- 状态
    is_favorite BOOLEAN DEFAULT FALSE COMMENT '是否收藏',
    is_deleted BOOLEAN DEFAULT FALSE COMMENT '是否删除',
    
    -- 时间戳
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 外键约束
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- 索引
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at),
    INDEX idx_input_type (input_type),
    INDEX idx_occasion (occasion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搭配建议记录表';
```

#### 2.2.2 搭配单品关联表 (outfit_items)

```sql
CREATE TABLE outfit_items (
    -- 主键
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
    
    -- 关联信息
    outfit_advice_id BIGINT NOT NULL COMMENT '搭配建议记录ID',
    clothing_id BIGINT COMMENT '衣物ID(来自用户衣橱,为空表示推荐新品)',
    
    -- 单品信息(冗余存储,防止衣物被删除后丢失信息)
    item_name VARCHAR(100) COMMENT '单品名称',
    item_category VARCHAR(50) COMMENT '单品类别',
    item_color VARCHAR(50) COMMENT '单品颜色',
    item_image_url VARCHAR(500) COMMENT '单品图片URL',
    
    -- 推荐属性
    is_recommended BOOLEAN DEFAULT FALSE COMMENT '是否为系统推荐新品',
    reason TEXT COMMENT '推荐理由',
    sort_order INT DEFAULT 0 COMMENT '排序顺序',
    
    -- 时间戳
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    -- 外键约束
    FOREIGN KEY (outfit_advice_id) REFERENCES outfit_advice_records(id) ON DELETE CASCADE,
    FOREIGN KEY (clothing_id) REFERENCES clothings(id) ON DELETE SET NULL,
    
    -- 索引
    INDEX idx_outfit_advice_id (outfit_advice_id),
    INDEX idx_clothing_id (clothing_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搭配单品关联表';
```

#### 2.2.3 搭配反馈表 (outfit_feedback)

```sql
CREATE TABLE outfit_feedback (
    -- 主键
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '反馈ID',
    
    -- 关联信息
    outfit_advice_id BIGINT NOT NULL COMMENT '搭配建议记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    
    -- 评分信息
    overall_rating TINYINT COMMENT '整体评分 1-5星',
    style_rating TINYINT COMMENT '风格匹配度评分 1-5星',
    practicality_rating TINYINT COMMENT '实用性评分 1-5星',
    weather_rating TINYINT COMMENT '天气适配度评分 1-5星',
    
    -- 反馈内容
    is_worn BOOLEAN COMMENT '是否实际穿着',
    worn_at DATE COMMENT '穿着日期',
    feedback_text TEXT COMMENT '文字反馈',
    
    -- 时间戳
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 外键约束
    FOREIGN KEY (outfit_advice_id) REFERENCES outfit_advice_records(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- 唯一约束(一个搭配只能反馈一次)
    UNIQUE KEY uk_outfit_user (outfit_advice_id, user_id),
    
    -- 索引
    INDEX idx_user_id (user_id),
    INDEX idx_overall_rating (overall_rating)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搭配反馈表';
```

#### 2.2.4 用户风格档案表 (user_style_archives)

```sql
CREATE TABLE user_style_archives (
    -- 主键
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '档案ID',
    
    -- 关联信息
    user_id BIGINT NOT NULL COMMENT '用户ID',
    
    -- 气质类型测试结果
    temperament_type VARCHAR(50) COMMENT '气质类型: 优雅型/自然型/浪漫型/古典型/前卫型/戏剧型',
    temperament_scores JSON COMMENT '各气质类型得分 {"优雅型": 85, "自然型": 60}',
    
    -- 身体特征
    height_cm INT COMMENT '身高(cm)',
    weight_kg INT COMMENT '体重(kg)',
    body_type VARCHAR(50) COMMENT '体型: 沙漏型/梨型/苹果型/矩形/倒三角',
    skin_tone VARCHAR(50) COMMENT '肤色: 冷白皮/暖黄皮/小麦色/深色',
    
    -- 风格偏好(从偏好学习模块同步)
    preferred_styles JSON COMMENT '偏好风格标签列表',
    avoided_styles JSON COMMENT '回避风格标签列表',
    preferred_colors JSON COMMENT '偏好颜色列表',
    avoided_colors JSON COMMENT '回避颜色列表',
    
    -- 职业与生活方式
    occupation VARCHAR(100) COMMENT '职业',
    lifestyle_tags JSON COMMENT '生活方式标签 ["职场精英", "运动爱好者"]',
    
    -- 元数据
    is_test_completed BOOLEAN DEFAULT FALSE COMMENT '是否完成测试',
    test_completed_at TIMESTAMP COMMENT '测试完成时间',
    
    -- 时间戳
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 外键约束
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- 唯一约束
    UNIQUE KEY uk_user_id (user_id),
    
    -- 索引
    INDEX idx_temperament_type (temperament_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户风格档案表';
```

### 2.3 枚举值定义

```sql
-- 输入类型枚举
-- 1: UPLOAD_PHOTO - 上传照片
-- 2: TEXT_DESCRIPTION - 文字描述
-- 3: SMART_RECOMMEND - 智能推荐

-- 天气状况枚举
-- SUNNY: 晴
-- CLOUDY: 多云
-- RAINY: 雨
-- SNOWY: 雪
-- FOGGY: 雾
-- WINDY: 大风

-- 时段枚举
-- MORNING: 早晨 (6:00-9:00)
-- FORENOON: 上午 (9:00-12:00)
-- AFTERNOON: 下午 (12:00-18:00)
-- EVENING: 晚上 (18:00-24:00)
-- NIGHT: 深夜 (0:00-6:00)

-- 气质类型枚举
-- ELEGANT: 优雅型
-- NATURAL: 自然型
-- ROMANTIC: 浪漫型
-- CLASSIC: 古典型
-- AVANT_GARDE: 前卫型
-- DRAMATIC: 戏剧型

-- 体型枚举
-- HOURGLASS: 沙漏型
-- PEAR: 梨型
-- APPLE: 苹果型
-- RECTANGLE: 矩形
-- INVERTED_TRIANGLE: 倒三角
```

---

## 3. RESTful API 接口文档

### 3.1 接口概览

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/v1/outfit/advice | 获取搭配建议(Agent工作流) |
| POST | /api/v1/outfit/analyze | 分析穿搭照片 |
| GET | /api/v1/outfit/history | 获取历史搭配记录 |
| GET | /api/v1/outfit/history/{id} | 获取搭配详情 |
| DELETE | /api/v1/outfit/history/{id} | 删除搭配记录 |
| POST | /api/v1/outfit/history/{id}/favorite | 收藏/取消收藏搭配 |
| POST | /api/v1/outfit/{id}/feedback | 提交搭配反馈 |
| GET | /api/v1/outfit/recommendations | 获取智能推荐搭配 |
| GET | /api/v1/outfit/style-profile | 获取用户风格档案 |
| PUT | /api/v1/outfit/style-profile | 更新用户风格档案 |
| POST | /api/v1/outfit/style-profile/test | 提交气质类型测试 |

### 3.2 接口详情

#### 3.2.1 获取搭配建议

**接口说明**: 核心接口，调用Agent工作流生成搭配建议

```
POST /api/v1/outfit/advice
```

**请求头**:
```
Authorization: Bearer {jwt_token}
Content-Type: multipart/form-data
```

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| image | File | 否 | 穿搭照片(与description二选一) |
| description | String | 否 | 文字描述需求(与image二选一) |
| occasion | String | 否 | 指定场合: daily/commute/date/sport/formal |
| considerWeather | Boolean | 否 | 是否考虑天气,默认true |
| useExistingClothes | Boolean | 否 | 是否优先使用已有衣物,默认true |

**请求示例**:
```http
POST /api/v1/outfit/advice
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary

------WebKitFormBoundary
Content-Disposition: form-data; name="image"; filename="outfit.jpg"
Content-Type: image/jpeg

[二进制图片数据]
------WebKitFormBoundary
Content-Disposition: form-data; name="occasion"

commute
------WebKitFormBoundary
Content-Disposition: form-data; name="considerWeather"

true
------WebKitFormBoundary--
```

**响应示例(成功)**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 10086,
    "inputType": 1,
    "inputImageUrl": "https://cdn.example.com/uploads/outfit_xxx.jpg",
    "analysis": {
      "detectedItems": [
        {"item": "白色衬衫", "category": "上装", "color": "白色"},
        {"item": "黑色西裤", "category": "下装", "color": "黑色"}
      ],
      "style": "商务通勤风",
      "colors": "经典黑白配，简洁大方"
    },
    "weather": {
      "location": "北京市朝阳区",
      "temperature": 22.5,
      "condition": "晴",
      "season": "春",
      "timeOfDay": "上午"
    },
    "recommendation": {
      "outfitName": "春日轻商务通勤搭配",
      "outfitDescription": "适合春季通勤的轻商务风格搭配",
      "items": [
        {
          "id": 101,
          "clothingId": 52,
          "name": "米色休闲西装",
          "category": "外套",
          "color": "米色",
          "imageUrl": "https://cdn.example.com/clothings/52.jpg",
          "isRecommended": false,
          "reason": "米色西装柔和不刻板，适合春季通勤"
        },
        {
          "id": 102,
          "clothingId": 38,
          "name": "白色纯棉衬衫",
          "category": "上装",
          "color": "白色",
          "imageUrl": "https://cdn.example.com/clothings/38.jpg",
          "isRecommended": false,
          "reason": "经典白衬衫，百搭单品"
        },
        {
          "id": 103,
          "clothingId": null,
          "name": "棕色乐福鞋",
          "category": "鞋履",
          "color": "棕色",
          "imageUrl": "https://cdn.example.com/recommendations/shoes_01.jpg",
          "isRecommended": true,
          "reason": "棕色乐福鞋增添复古质感，与米色西装协调"
        }
      ],
      "reasoning": "基于您上传的商务风格照片，结合今日22°C的晴朗天气，推荐这套轻商务搭配。米色西装比深色更显春季活力，内搭白衬衫保持专业感。",
      "suggestions": "可以搭配一条细腰带提升腰线，配饰选择简约的金属色项链或手表。"
    },
    "purchaseRecommendations": [
      {
        "name": "棕色真皮乐福鞋",
        "reason": "填补鞋履空缺，提升整体质感",
        "estimatedPrice": "¥399-599",
        "link": "https://example.com/product/12345"
      }
    ],
    "createdAt": "2026-02-03T10:30:00+08:00"
  }
}
```

**响应示例(失败)**:
```json
{
  "code": 400,
  "message": "请提供穿搭照片或文字描述",
  "data": null
}
```

**状态码说明**:
- 200: 成功
- 400: 请求参数错误
- 401: 未授权
- 429: 请求过于频繁
- 500: 服务器内部错误(AI服务异常)

---

#### 3.2.2 分析穿搭照片

**接口说明**: 仅分析照片，不生成搭配建议

```
POST /api/v1/outfit/analyze
```

**请求头**:
```
Authorization: Bearer {jwt_token}
Content-Type: multipart/form-data
```

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| image | File | 是 | 穿搭照片 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "detectedItems": [
      {"item": "oversize卫衣", "category": "上装", "color": "灰色", "confidence": 0.95},
      {"item": "阔腿裤", "category": "下装", "color": "黑色", "confidence": 0.92}
    ],
    "style": "韩系休闲风",
    "colors": "灰黑配色，低调简约",
    "occasion": "日常休闲",
    "season": "秋冬",
    "suggestions": "oversize版型适合梨型身材，建议将上衣前摆塞入裤腰提升比例"
  }
}
```

---

#### 3.2.3 获取历史搭配记录

**接口说明**: 分页查询用户的搭配历史

```
GET /api/v1/outfit/history
```

**请求头**:
```
Authorization: Bearer {jwt_token}
```

**请求参数(Query)**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码,默认1 |
| size | Integer | 否 | 每页数量,默认10,最大50 |
| occasion | String | 否 | 按场合筛选 |
| isFavorite | Boolean | 否 | 只查看收藏 |
| startDate | String | 否 | 开始日期(yyyy-MM-dd) |
| endDate | String | 否 | 结束日期(yyyy-MM-dd) |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 128,
    "pages": 13,
    "current": 1,
    "size": 10,
    "records": [
      {
        "id": 10086,
        "outfitName": "春日轻商务通勤搭配",
        "inputImageUrl": "https://cdn.example.com/uploads/outfit_xxx.jpg",
        "occasion": "commute",
        "itemCount": 4,
        "isFavorite": false,
        "hasFeedback": true,
        "overallRating": 5,
        "createdAt": "2026-02-03T10:30:00+08:00"
      }
    ]
  }
}
```

---

#### 3.2.4 获取搭配详情

**接口说明**: 获取单条搭配建议的完整信息

```
GET /api/v1/outfit/history/{id}
```

**路径参数**:

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 搭配记录ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 10086,
    "inputType": 1,
    "inputImageUrl": "https://cdn.example.com/uploads/outfit_xxx.jpg",
    "analysis": {
      "detectedItems": [...],
      "style": "商务通勤风",
      "colors": "经典黑白配"
    },
    "weather": {
      "location": "北京市朝阳区",
      "temperature": 22.5,
      "condition": "晴",
      "season": "春"
    },
    "recommendation": {
      "outfitName": "春日轻商务通勤搭配",
      "items": [...],
      "reasoning": "...",
      "suggestions": "..."
    },
    "isFavorite": true,
    "feedback": {
      "overallRating": 5,
      "styleRating": 5,
      "practicalityRating": 4,
      "isWorn": true,
      "wornAt": "2026-02-03",
      "feedbackText": "很满意，同事都夸好看"
    },
    "createdAt": "2026-02-03T10:30:00+08:00"
  }
}
```

---

#### 3.2.5 删除搭配记录

**接口说明**: 软删除搭配记录

```
DELETE /api/v1/outfit/history/{id}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

---

#### 3.2.6 收藏/取消收藏搭配

**接口说明**: 切换搭配记录的收藏状态

```
POST /api/v1/outfit/history/{id}/favorite
```

**请求体**:
```json
{
  "isFavorite": true
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "收藏成功",
  "data": {
    "id": 10086,
    "isFavorite": true
  }
}
```

---

#### 3.2.7 提交搭配反馈

**接口说明**: 对搭配方案进行评分和反馈

```
POST /api/v1/outfit/{id}/feedback
```

**请求体**:
```json
{
  "overallRating": 5,
  "styleRating": 5,
  "practicalityRating": 4,
  "weatherRating": 5,
  "isWorn": true,
  "wornAt": "2026-02-03",
  "feedbackText": "非常满意这套搭配，很适合今天的天气"
}
```

**字段约束**:
- overallRating: 1-5整数,必填
- styleRating: 1-5整数,可选
- practicalityRating: 1-5整数,可选
- weatherRating: 1-5整数,可选
- isWorn: Boolean,可选
- wornAt: Date格式yyyy-MM-dd,可选
- feedbackText: 最大500字符,可选

**响应示例**:
```json
{
  "code": 200,
  "message": "反馈提交成功",
  "data": {
    "id": 5001,
    "outfitAdviceId": 10086,
    "overallRating": 5,
    "createdAt": "2026-02-03T18:00:00+08:00"
  }
}
```

---

#### 3.2.8 获取智能推荐搭配

**接口说明**: 基于用户偏好和天气自动推荐搭配

```
GET /api/v1/outfit/recommendations
```

**请求参数(Query)**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| limit | Integer | 否 | 推荐数量,默认3,最大10 |
| occasion | String | 否 | 指定场合 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "weather": {
      "location": "北京市朝阳区",
      "temperature": 22.5,
      "condition": "晴"
    },
    "recommendations": [
      {
        "outfitName": "今日通勤推荐",
        "items": [...],
        "reasoning": "基于今日晴朗天气和您的商务风格偏好推荐",
        "suitabilityScore": 0.92
      }
    ]
  }
}
```

---

#### 3.2.9 获取用户风格档案

**接口说明**: 获取用户的风格档案信息

```
GET /api/v1/outfit/style-profile
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 1001,
    "temperamentType": "优雅型",
    "temperamentScores": {
      "优雅型": 85,
      "自然型": 60,
      "浪漫型": 45,
      "古典型": 70,
      "前卫型": 30,
      "戏剧型": 40
    },
    "heightCm": 165,
    "weightKg": 52,
    "bodyType": "沙漏型",
    "skinTone": "暖黄皮",
    "preferredStyles": ["通勤", "优雅", "简约"],
    "avoidedStyles": ["街头", "朋克"],
    "preferredColors": ["米白", "驼色", "藏蓝"],
    "avoidedColors": ["荧光色"],
    "occupation": "产品经理",
    "lifestyleTags": ["职场精英", "瑜伽爱好者"],
    "isTestCompleted": true,
    "testCompletedAt": "2026-01-15T14:00:00+08:00"
  }
}
```

---

#### 3.2.10 更新用户风格档案

**接口说明**: 更新用户风格档案(部分字段)

```
PUT /api/v1/outfit/style-profile
```

**请求体**:
```json
{
  "heightCm": 165,
  "weightKg": 52,
  "bodyType": "沙漏型",
  "skinTone": "暖黄皮",
  "occupation": "产品经理",
  "lifestyleTags": ["职场精英", "瑜伽爱好者"]
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "更新成功",
  "data": {
    "userId": 1001,
    "heightCm": 165,
    "weightKg": 52,
    "updatedAt": "2026-02-03T11:00:00+08:00"
  }
}
```

---

#### 3.2.11 提交气质类型测试

**接口说明**: 提交气质类型测试结果

```
POST /api/v1/outfit/style-profile/test
```

**请求体**:
```json
{
  "answers": [
    {"questionId": 1, "optionId": "A", "score": 5},
    {"questionId": 2, "optionId": "B", "score": 3}
  ]
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "测试完成",
  "data": {
    "temperamentType": "优雅型",
    "temperamentScores": {
      "优雅型": 85,
      "自然型": 60,
      "浪漫型": 45,
      "古典型": 70,
      "前卫型": 30,
      "戏剧型": 40
    },
    "description": "优雅型气质适合简约大方的穿搭风格，推荐选择线条流畅、质感优良的单品。",
    "recommendedStyles": ["极简", "法式", "轻熟"],
    "avoidedStyles": ["朋克", "街头"]
  }
}
```

---

## 4. 错误码定义

| 错误码 | 说明 | 场景 |
|--------|------|------|
| 200 | 成功 | - |
| 400 | 请求参数错误 | 参数缺失或格式错误 |
| 401 | 未授权 | Token无效或过期 |
| 403 | 禁止访问 | 无权限访问该资源 |
| 404 | 资源不存在 | 搭配记录不存在 |
| 409 | 资源冲突 | 重复提交反馈 |
| 429 | 请求过于频繁 | 触发限流 |
| 500 | 服务器内部错误 | AI服务异常 |
| 503 | 服务不可用 | MCP天气服务异常 |

---

## 5. 数据流说明

### 5.1 Agent工作流数据流

```
用户请求
    ↓
[1. 接收输入]
    ├── 保存上传图片
    └── 记录初始状态
    ↓
[2. 并行处理]
    ├── 图像分析(调用Qwen3-VL)
    ├── 天气数据(调用MCP)
    └── 用户档案查询
    ↓
[3. RAG检索]
    ├── 向量检索相似衣物
    ├── 属性过滤(季节/场合)
    └── 偏好排序
    ↓
[4. 搭配生成]
    ├── 生成搭配方案
    ├── 计算适配度分数
    └── 生成推荐理由
    ↓
[5. 结果存储]
    ├── 保存搭配记录
    ├── 保存单品关联
    └── 返回响应
```

---

**文档版本**: v1.0  
**创建日期**: 2026-02-03  
**最后更新**: 2026-02-03
