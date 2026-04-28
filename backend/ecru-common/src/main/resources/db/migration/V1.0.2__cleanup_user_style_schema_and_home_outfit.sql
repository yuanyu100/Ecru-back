USE ecru;

CREATE TABLE IF NOT EXISTS user_style_archives (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '档案ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    temperament_type VARCHAR(50) DEFAULT NULL COMMENT '气质类型',
    temperament_scores JSON DEFAULT NULL COMMENT '气质得分',
    height_cm INT DEFAULT NULL COMMENT '身高(cm)',
    weight_kg INT DEFAULT NULL COMMENT '体重(kg)',
    body_type VARCHAR(50) DEFAULT NULL COMMENT '体型',
    skin_tone VARCHAR(50) DEFAULT NULL COMMENT '肤色',
    preferred_styles JSON DEFAULT NULL COMMENT '偏好风格',
    avoided_styles JSON DEFAULT NULL COMMENT '回避风格',
    preferred_colors JSON DEFAULT NULL COMMENT '偏好颜色',
    avoided_colors JSON DEFAULT NULL COMMENT '回避颜色',
    occupation VARCHAR(100) DEFAULT NULL COMMENT '职业',
    lifestyle_tags JSON DEFAULT NULL COMMENT '生活方式标签',
    is_test_completed BOOLEAN DEFAULT FALSE COMMENT '是否完成测试',
    test_completed_at TIMESTAMP NULL DEFAULT NULL COMMENT '测试完成时间',
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_id (user_id),
    KEY idx_temperament_type (temperament_type),
    CONSTRAINT fk_user_style_archives_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户风格档案表';

ALTER TABLE user_login_logs
    MODIFY COLUMN user_id BIGINT NULL COMMENT '用户ID';

DELIMITER //

DROP PROCEDURE IF EXISTS migrate_user_style_schema //
CREATE PROCEDURE migrate_user_style_schema()
BEGIN
    DECLARE has_legacy_archive_table INT DEFAULT 0;
    DECLARE has_legacy_archive_column INT DEFAULT 0;
    DECLARE has_canonical_score_table INT DEFAULT 0;
    DECLARE has_old_score_table INT DEFAULT 0;

    SELECT COUNT(*)
    INTO has_legacy_archive_table
    FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'user_style_profiles';

    IF has_legacy_archive_table > 0 THEN
        SELECT COUNT(*)
        INTO has_legacy_archive_column
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'user_style_profiles'
          AND column_name = 'temperament_type';

        IF has_legacy_archive_column > 0 THEN
            INSERT INTO user_style_archives (
                user_id,
                temperament_type,
                temperament_scores,
                height_cm,
                weight_kg,
                body_type,
                skin_tone,
                preferred_styles,
                avoided_styles,
                preferred_colors,
                avoided_colors,
                occupation,
                lifestyle_tags,
                is_test_completed,
                test_completed_at,
                created_at,
                updated_at
            )
            SELECT
                legacy.user_id,
                legacy.temperament_type,
                CASE
                    WHEN legacy.temperament_scores IS NULL OR TRIM(legacy.temperament_scores) = '' THEN NULL
                    WHEN JSON_VALID(legacy.temperament_scores) THEN CAST(legacy.temperament_scores AS JSON)
                    ELSE NULL
                END,
                CAST(legacy.height_cm AS SIGNED),
                CAST(legacy.weight_kg AS SIGNED),
                legacy.body_type,
                legacy.skin_tone,
                CASE
                    WHEN legacy.preferred_styles IS NULL OR TRIM(legacy.preferred_styles) = '' THEN NULL
                    WHEN JSON_VALID(legacy.preferred_styles) THEN CAST(legacy.preferred_styles AS JSON)
                    ELSE CAST(CONCAT('["', REPLACE(REPLACE(REPLACE(legacy.preferred_styles, '"', '\\"'), '，', ','), ',', '","'), '"]') AS JSON)
                END,
                CASE
                    WHEN legacy.avoided_styles IS NULL OR TRIM(legacy.avoided_styles) = '' THEN NULL
                    WHEN JSON_VALID(legacy.avoided_styles) THEN CAST(legacy.avoided_styles AS JSON)
                    ELSE CAST(CONCAT('["', REPLACE(REPLACE(REPLACE(legacy.avoided_styles, '"', '\\"'), '，', ','), ',', '","'), '"]') AS JSON)
                END,
                CASE
                    WHEN legacy.preferred_colors IS NULL OR TRIM(legacy.preferred_colors) = '' THEN NULL
                    WHEN JSON_VALID(legacy.preferred_colors) THEN CAST(legacy.preferred_colors AS JSON)
                    ELSE CAST(CONCAT('["', REPLACE(REPLACE(REPLACE(legacy.preferred_colors, '"', '\\"'), '，', ','), ',', '","'), '"]') AS JSON)
                END,
                CASE
                    WHEN legacy.avoided_colors IS NULL OR TRIM(legacy.avoided_colors) = '' THEN NULL
                    WHEN JSON_VALID(legacy.avoided_colors) THEN CAST(legacy.avoided_colors AS JSON)
                    ELSE CAST(CONCAT('["', REPLACE(REPLACE(REPLACE(legacy.avoided_colors, '"', '\\"'), '，', ','), ',', '","'), '"]') AS JSON)
                END,
                legacy.occupation,
                CASE
                    WHEN legacy.lifestyle_tags IS NULL OR TRIM(legacy.lifestyle_tags) = '' THEN NULL
                    WHEN JSON_VALID(legacy.lifestyle_tags) THEN CAST(legacy.lifestyle_tags AS JSON)
                    ELSE CAST(CONCAT('["', REPLACE(REPLACE(REPLACE(legacy.lifestyle_tags, '"', '\\"'), '，', ','), ',', '","'), '"]') AS JSON)
                END,
                COALESCE(legacy.is_test_completed, FALSE),
                legacy.test_completed_at,
                COALESCE(legacy.created_at, CURRENT_TIMESTAMP),
                COALESCE(legacy.updated_at, CURRENT_TIMESTAMP)
            FROM user_style_profiles legacy
            WHERE legacy.user_id IS NOT NULL
            ON DUPLICATE KEY UPDATE
                temperament_type = VALUES(temperament_type),
                temperament_scores = VALUES(temperament_scores),
                height_cm = VALUES(height_cm),
                weight_kg = VALUES(weight_kg),
                body_type = VALUES(body_type),
                skin_tone = VALUES(skin_tone),
                preferred_styles = VALUES(preferred_styles),
                avoided_styles = VALUES(avoided_styles),
                preferred_colors = VALUES(preferred_colors),
                avoided_colors = VALUES(avoided_colors),
                occupation = VALUES(occupation),
                lifestyle_tags = VALUES(lifestyle_tags),
                is_test_completed = VALUES(is_test_completed),
                test_completed_at = VALUES(test_completed_at),
                updated_at = VALUES(updated_at);

            DROP TABLE user_style_profiles;
        END IF;
    END IF;

    CREATE TABLE IF NOT EXISTS user_style_profiles (
        id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
        user_id BIGINT NOT NULL COMMENT '用户ID',
        style_tag_id BIGINT NOT NULL COMMENT '风格标签ID',
        preference_score DECIMAL(5,4) DEFAULT 0.0000 COMMENT '偏好分数',
        interaction_count INT DEFAULT 0 COMMENT '交互次数',
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
        FOREIGN KEY (style_tag_id) REFERENCES style_tags(id) ON DELETE CASCADE,
        UNIQUE KEY uk_user_style (user_id, style_tag_id),
        INDEX idx_user_id (user_id),
        INDEX idx_style_tag_id (style_tag_id),
        INDEX idx_preference_score (preference_score)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户风格画像表';

    SELECT COUNT(*)
    INTO has_old_score_table
    FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'user_style_profile';

    IF has_old_score_table > 0 THEN
        INSERT INTO user_style_profiles (
            user_id,
            style_tag_id,
            preference_score,
            interaction_count,
            updated_at
        )
        SELECT
            legacy.user_id,
            legacy.style_tag_id,
            legacy.preference_score,
            legacy.interaction_count,
            legacy.updated_at
        FROM user_style_profile legacy
        ON DUPLICATE KEY UPDATE
            preference_score = VALUES(preference_score),
            interaction_count = VALUES(interaction_count),
            updated_at = VALUES(updated_at);

        DROP TABLE user_style_profile;
    END IF;
END //
DELIMITER ;

CALL migrate_user_style_schema();
DROP PROCEDURE migrate_user_style_schema;

DROP TABLE IF EXISTS clothing_style_tags;
DROP TABLE IF EXISTS search_config;
