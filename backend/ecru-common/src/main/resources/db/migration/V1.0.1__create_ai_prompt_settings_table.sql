CREATE TABLE IF NOT EXISTS ai_prompt_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'primary key',
    setting_key VARCHAR(128) NOT NULL COMMENT 'setting key',
    setting_value LONGTEXT NOT NULL COMMENT 'setting value',
    description VARCHAR(255) NULL COMMENT 'setting description',
    updated_by BIGINT NULL COMMENT 'last updated user id',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
    UNIQUE KEY uk_ai_prompt_settings_key (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI prompt settings';
