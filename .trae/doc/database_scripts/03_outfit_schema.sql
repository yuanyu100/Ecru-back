-- 搭配建议模块数据库表结构

-- 1. 搭配建议记录表
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
    detected_items JSON COMMENT '识别到的单品列表 ["item": "上衣", "color": "白色", "category": "上装"]',
    detected_style VARCHAR(100) COMMENT '识别到的风格',
    color_analysis TEXT COMMENT '色彩分析结果',
    
    -- 搭配方案
    outfit_name VARCHAR(200) COMMENT '搭配方案名称',
    outfit_description TEXT COMMENT '搭配方案描述',
    reasoning TEXT COMMENT '搭配思路说明',
    fashion_suggestions TEXT COMMENT '时尚建议',
    
    -- 推荐商品(JSON存储)
    purchase_recommendations JSON COMMENT '推荐购买商品 ["name": "", "reason": "", "link": ""]',
    
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

-- 2. 搭配单品关联表
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

-- 3. 搭配反馈表
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

-- 4. 用户风格档案表
CREATE TABLE user_style_profiles (
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
