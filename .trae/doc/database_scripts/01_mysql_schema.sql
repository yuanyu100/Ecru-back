-- ================================================
-- MySQL 数据库脚本
-- 适用于：智能衣橱系统
-- 时间：2026-02-04
-- ================================================

-- 切换到 ecru 数据库
USE ecru;

-- ================================================
-- 1. 用户模块
-- ================================================

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '加密密码',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    avatar_url VARCHAR(255) COMMENT '头像URL',
    nickname VARCHAR(50) COMMENT '昵称',
    gender TINYINT COMMENT '性别：0-未知，1-男，2-女',
    birthday DATE COMMENT '生日',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    last_login_at TIMESTAMP COMMENT '最后登录时间',
    last_login_ip VARCHAR(50) COMMENT '最后登录IP',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_phone (phone),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 用户偏好设置表
CREATE TABLE IF NOT EXISTS user_settings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '设置ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    setting_key VARCHAR(50) NOT NULL COMMENT '设置项键名',
    setting_value VARCHAR(500) COMMENT '设置项值',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_setting (user_id, setting_key),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户偏好设置表';

-- 用户登录日志表
CREATE TABLE IF NOT EXISTS user_login_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    login_type TINYINT NOT NULL DEFAULT 1 COMMENT '登录类型：1-密码，2-短信，3-第三方',
    login_ip VARCHAR(50) COMMENT '登录IP',
    login_device VARCHAR(200) COMMENT '登录设备信息',
    login_location VARCHAR(100) COMMENT '登录地点',
    login_status TINYINT NOT NULL DEFAULT 1 COMMENT '登录状态：0-失败，1-成功',
    fail_reason VARCHAR(200) COMMENT '失败原因',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户登录日志表';

-- ================================================
-- 2. 衣物模块
-- ================================================

-- 衣物主表
CREATE TABLE IF NOT EXISTS clothings (
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

-- 衣物与风格标签关联表
CREATE TABLE IF NOT EXISTS clothing_style_tags (
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

-- 衣物穿着记录表
CREATE TABLE IF NOT EXISTS clothing_wear_logs (
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

-- ================================================
-- 3. 搭配建议模块
-- ================================================

-- 搭配建议记录表
CREATE TABLE IF NOT EXISTS outfit_advice_records (
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

-- 搭配单品关联表
CREATE TABLE IF NOT EXISTS outfit_items (
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

-- 搭配反馈表
CREATE TABLE IF NOT EXISTS outfit_feedback (
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

-- 用户风格档案表
CREATE TABLE IF NOT EXISTS user_style_archives (
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

-- ================================================
-- 4. 风格偏好模块
-- ================================================

-- 风格标签表（核心表）
CREATE TABLE IF NOT EXISTS style_tags (
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

-- 风格图片表
CREATE TABLE IF NOT EXISTS style_images (
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

-- 风格图片标签关联表
CREATE TABLE IF NOT EXISTS style_image_tags (
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

-- 用户风格偏好标记表
CREATE TABLE IF NOT EXISTS user_style_preference_logs (
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

-- 用户风格画像表
CREATE TABLE IF NOT EXISTS user_style_profiles (
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

-- ================================================
-- 5. 初始化数据
-- ================================================

-- 预设风格标签
INSERT IGNORE INTO style_tags (name, category, is_preset) VALUES
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

-- 搜索配置表
CREATE TABLE IF NOT EXISTS search_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    config_key VARCHAR(100) NOT NULL COMMENT '配置键',
    config_value VARCHAR(500) NOT NULL COMMENT '配置值',
    description VARCHAR(255) COMMENT '配置描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_config_key (config_key),
    INDEX idx_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搜索配置表';

-- 初始化搜索配置（如果需要）
INSERT IGNORE INTO search_config (config_key, config_value, description) VALUES
('default_top_k', '10', '默认返回结果数量'),
('similarity_threshold', '0.7', '相似度阈值（0-1之间）'),
('max_query_length', '500', '最大查询文本长度'),
('enable_hybrid_search', 'true', '是否启用混合检索'),
('vector_dimension', '1536', '向量维度'),
('embedding_model', 'text-embedding-3-small', '默认Embedding模型'),
('max_search_results', '50', '最大返回结果数量'),
('enable_search_history', 'true', '是否记录检索历史');

-- ================================================
-- 6. 权限设置
-- ================================================

-- 授予用户权限
GRANT ALL PRIVILEGES ON ecru.* TO 'root'@'localhost';
FLUSH PRIVILEGES;

-- ================================================
-- 完成
-- ================================================
SELECT 'MySQL 数据库脚本执行完成！' AS message;
