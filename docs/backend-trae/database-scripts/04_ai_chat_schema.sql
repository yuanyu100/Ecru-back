-- AI对话模块数据库表结构

-- 1. AI对话会话表
CREATE TABLE ai_conversations (
    -- 主键
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',

    -- 关联信息
    user_id BIGINT NOT NULL COMMENT '用户ID',

    -- 会话信息
    session_id VARCHAR(64) NOT NULL COMMENT '会话唯一标识',
    title VARCHAR(200) COMMENT '会话标题(自动生成或用户设置)',
    context VARCHAR(50) COMMENT '对话场景: outfit-搭配建议, general-一般咨询, style-风格建议',

    -- 状态
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否活跃',
    message_count INT DEFAULT 0 COMMENT '消息数量',

    -- 元数据
    metadata JSON COMMENT '扩展信息: 地理位置、场合、天气等',

    -- 时间戳
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 外键约束
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    -- 唯一约束
    UNIQUE KEY uk_session_id (session_id),

    -- 索引
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话会话表';

-- 2. AI对话消息表
CREATE TABLE ai_chat_messages (
    -- 主键
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',

    -- 关联信息
    conversation_id BIGINT NOT NULL COMMENT '所属会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',

    -- 消息内容
    role VARCHAR(20) NOT NULL COMMENT '角色: user-用户, assistant-AI助手, system-系统',
    content TEXT NOT NULL COMMENT '消息内容',

    -- 消息类型
    message_type VARCHAR(30) DEFAULT 'text' COMMENT '消息类型: text-文本, image-图片, recommendation-推荐, error-错误',

    -- 推荐信息(JSON存储,当message_type为recommendation时使用)
    recommendations JSON COMMENT '推荐衣物信息 [{"clothingId": 1, "name": "", "reason": ""}]',

    -- 上下文信息
    context_snapshot JSON COMMENT '上下文快照: 天气、场合等',

    -- 元数据
    metadata JSON COMMENT '扩展信息: 意图分析、token消耗等',

    -- 时间戳
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    -- 外键约束
    FOREIGN KEY (conversation_id) REFERENCES ai_conversations(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    -- 索引
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话消息表';

-- 3. 用户对话偏好表(可选,用于个性化对话体验)
CREATE TABLE ai_chat_preferences (
    -- 主键
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '偏好ID',

    -- 关联信息
    user_id BIGINT NOT NULL COMMENT '用户ID',

    -- 偏好设置
    preferred_context VARCHAR(50) DEFAULT 'general' COMMENT '默认对话场景',
    auto_save_conversation BOOLEAN DEFAULT TRUE COMMENT '是否自动保存对话',
    max_context_messages INT DEFAULT 10 COMMENT '上下文保留消息数',

    -- 时间戳
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 外键约束
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    -- 唯一约束
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户对话偏好表';
