-- AI API监控相关表结构
-- 创建时间: 2026-03-26
-- 变更说明: 初始化AI API监控表结构

-- 1. AI API调用记录表
CREATE TABLE IF NOT EXISTS ai_api_call_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    scene VARCHAR(64) NOT NULL COMMENT '调用场景/接口类型',
    model VARCHAR(128) NOT NULL COMMENT '模型名称',
    request_id VARCHAR(64) NOT NULL COMMENT '请求ID（用于链路追踪）',
    user_id BIGINT COMMENT '用户ID',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '调用状态：0-失败，1-成功',
    http_code INT COMMENT 'HTTP状态码',
    error_type VARCHAR(32) COMMENT '错误类型：timeout/parse_error/http_error/business_error/network_error',
    error_message VARCHAR(1024) COMMENT '错误信息',
    response_time BIGINT NOT NULL COMMENT '响应时间（毫秒）',
    input_tokens INT COMMENT '输入token数',
    output_tokens INT COMMENT '输出token数',
    total_tokens INT COMMENT '总token数',
    prompt_length INT COMMENT '提示词长度（字符数）',
    response_length INT COMMENT '响应长度（字符数）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_date VARCHAR(10) NOT NULL COMMENT '创建日期（yyyy-MM-dd，用于分区）',
    INDEX idx_scene (scene),
    INDEX idx_model (model),
    INDEX idx_request_id (request_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_create_date (create_date),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI API调用记录表';

-- 2. AI API小时级统计表
CREATE TABLE IF NOT EXISTS ai_api_stats_hourly (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    stats_date VARCHAR(10) NOT NULL COMMENT '统计日期（yyyy-MM-dd）',
    stats_hour TINYINT NOT NULL COMMENT '统计小时（0-23）',
    scene VARCHAR(64) NOT NULL COMMENT '调用场景',
    model VARCHAR(128) NOT NULL COMMENT '模型名称',
    total_calls INT NOT NULL DEFAULT 0 COMMENT '总调用次数',
    success_calls INT NOT NULL DEFAULT 0 COMMENT '成功次数',
    failed_calls INT NOT NULL DEFAULT 0 COMMENT '失败次数',
    success_rate DECIMAL(5,2) COMMENT '成功率（百分比）',
    avg_response_time DECIMAL(10,2) COMMENT '平均响应时间（毫秒）',
    min_response_time BIGINT COMMENT '最小响应时间（毫秒）',
    max_response_time BIGINT COMMENT '最大响应时间（毫秒）',
    p50_response_time DECIMAL(10,2) COMMENT 'P50响应时间（毫秒）',
    p95_response_time DECIMAL(10,2) COMMENT 'P95响应时间（毫秒）',
    p99_response_time DECIMAL(10,2) COMMENT 'P99响应时间（毫秒）',
    total_input_tokens INT NOT NULL DEFAULT 0 COMMENT '总输入token数',
    total_output_tokens INT NOT NULL DEFAULT 0 COMMENT '总输出token数',
    total_tokens INT NOT NULL DEFAULT 0 COMMENT '总token数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_date_hour_scene_model (stats_date, stats_hour, scene, model),
    INDEX idx_stats_date (stats_date),
    INDEX idx_scene (scene),
    INDEX idx_model (model)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI API小时级统计表';

-- 3. AI API日级统计表
CREATE TABLE IF NOT EXISTS ai_api_stats_daily (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    stats_date VARCHAR(10) NOT NULL COMMENT '统计日期（yyyy-MM-dd）',
    scene VARCHAR(64) NOT NULL COMMENT '调用场景',
    model VARCHAR(128) NOT NULL COMMENT '模型名称',
    total_calls INT NOT NULL DEFAULT 0 COMMENT '总调用次数',
    success_calls INT NOT NULL DEFAULT 0 COMMENT '成功次数',
    failed_calls INT NOT NULL DEFAULT 0 COMMENT '失败次数',
    success_rate DECIMAL(5,2) COMMENT '成功率（百分比）',
    avg_response_time DECIMAL(10,2) COMMENT '平均响应时间（毫秒）',
    min_response_time BIGINT COMMENT '最小响应时间（毫秒）',
    max_response_time BIGINT COMMENT '最大响应时间（毫秒）',
    p50_response_time DECIMAL(10,2) COMMENT 'P50响应时间（毫秒）',
    p95_response_time DECIMAL(10,2) COMMENT 'P95响应时间（毫秒）',
    p99_response_time DECIMAL(10,2) COMMENT 'P99响应时间（毫秒）',
    total_input_tokens INT NOT NULL DEFAULT 0 COMMENT '总输入token数',
    total_output_tokens INT NOT NULL DEFAULT 0 COMMENT '总输出token数',
    total_tokens INT NOT NULL DEFAULT 0 COMMENT '总token数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_date_scene_model (stats_date, scene, model),
    INDEX idx_stats_date (stats_date),
    INDEX idx_scene (scene),
    INDEX idx_model (model)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI API日级统计表';
