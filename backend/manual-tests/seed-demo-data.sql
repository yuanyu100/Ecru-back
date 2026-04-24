USE ecru;

-- Demo accounts:
--   admin:    zhangsan / 123456
--   ordinary: testuser / 123456
-- The password hash below is BCrypt("123456").

SET @role_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'role'
);
SET @alter_role_sql = IF(
    @role_column_exists = 0,
    'ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT ''USER'' COMMENT ''role: ADMIN/USER'' AFTER nickname',
    'SELECT 1'
);
PREPARE stmt_add_role FROM @alter_role_sql;
EXECUTE stmt_add_role;
DEALLOCATE PREPARE stmt_add_role;

CREATE TABLE IF NOT EXISTS ai_api_call_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scene VARCHAR(64) NOT NULL,
    model VARCHAR(128) NOT NULL,
    request_id VARCHAR(64) NOT NULL,
    user_id BIGINT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    http_code INT NULL,
    error_type VARCHAR(32) NULL,
    error_message VARCHAR(1024) NULL,
    response_time BIGINT NOT NULL,
    input_tokens INT NULL,
    output_tokens INT NULL,
    total_tokens INT NULL,
    prompt_length INT NULL,
    response_length INT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_date VARCHAR(10) NOT NULL,
    INDEX idx_ai_call_user_id (user_id),
    INDEX idx_ai_call_create_date (create_date),
    INDEX idx_ai_call_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @demo_password = '$2a$10$G257a2LmEdL1lT4hEkOQA.iCb6uYs3d2CW.0Pt6YS8qHpzgXZYFoa';

INSERT INTO users (
    username, password, email, phone, avatar_url, nickname, role, status, created_at, updated_at
) VALUES
    (
        'zhangsan',
        @demo_password,
        'demo-admin-ecru@example.com',
        '19988880002',
        'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="240" height="240"><rect width="100%" height="100%" fill="%231a304d"/><text x="50%" y="50%" text-anchor="middle" dominant-baseline="middle" fill="%23ffffff" font-size="72">A</text></svg>',
        'Demo Admin',
        'ADMIN',
        1,
        NOW(),
        NOW()
    ),
    (
        'testuser',
        @demo_password,
        'demo-user-ecru@example.com',
        '19988880001',
        'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="240" height="240"><rect width="100%" height="100%" fill="%236b4b1f"/><text x="50%" y="50%" text-anchor="middle" dominant-baseline="middle" fill="%23fff8ef" font-size="72">U</text></svg>',
        'Demo User',
        'USER',
        1,
        NOW(),
        NOW()
    )
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    email = VALUES(email),
    phone = VALUES(phone),
    avatar_url = VALUES(avatar_url),
    nickname = VALUES(nickname),
    role = VALUES(role),
    status = VALUES(status),
    updated_at = NOW();

SET @admin_id = (SELECT id FROM users WHERE username = 'zhangsan' LIMIT 1);
SET @user_id = (SELECT id FROM users WHERE username = 'testuser' LIMIT 1);

DELETE m
FROM ai_chat_messages m
JOIN ai_conversations c ON c.id = m.conversation_id
WHERE c.user_id IN (@admin_id, @user_id);

DELETE FROM ai_conversations
WHERE user_id IN (@admin_id, @user_id);

DELETE f
FROM outfit_feedback f
JOIN outfit_advice_records r ON r.id = f.outfit_advice_id
WHERE r.user_id IN (@admin_id, @user_id);

DELETE i
FROM outfit_items i
JOIN outfit_advice_records r ON r.id = i.outfit_advice_id
WHERE r.user_id IN (@admin_id, @user_id);

DELETE FROM outfit_advice_records
WHERE user_id IN (@admin_id, @user_id);

DELETE FROM clothings
WHERE user_id IN (@admin_id, @user_id);

DELETE FROM user_style_profiles
WHERE user_id IN (@admin_id, @user_id);

DELETE FROM user_settings
WHERE user_id IN (@admin_id, @user_id);

DELETE FROM ai_api_call_record
WHERE user_id IN (@admin_id, @user_id);

INSERT INTO user_settings (user_id, setting_key, setting_value, created_at, updated_at) VALUES
    (@user_id, 'stylePreferences', '["commute","minimal","casual"]', NOW(), NOW()),
    (@user_id, 'usualSize', 'M', NOW(), NOW()),
    (@user_id, 'region', 'Shanghai', NOW(), NOW()),
    (@admin_id, 'stylePreferences', '["commute","academy","clean"]', NOW(), NOW()),
    (@admin_id, 'usualSize', 'L', NOW(), NOW()),
    (@admin_id, 'region', 'Hangzhou', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    setting_value = VALUES(setting_value),
    updated_at = NOW();

INSERT INTO user_style_profiles (
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
) VALUES
    (
        @user_id,
        'Natural',
        '{"Natural":88,"Elegant":72,"Classic":61}',
        168,
        56,
        'Rectangle',
        'Warm',
        'commute,minimal,casual',
        'street',
        'ivory,khaki,navy',
        'neon',
        'Product Manager',
        'commute,gym,cafe',
        1,
        NOW(),
        NOW(),
        NOW()
    ),
    (
        @admin_id,
        'Classic',
        '{"Classic":90,"Elegant":84,"Natural":60}',
        180,
        72,
        'Inverted Triangle',
        'Cool',
        'commute,academy,clean',
        'street',
        'black,gray,navy',
        'neon',
        'Lecturer',
        'teaching,meeting,business-trip',
        1,
        NOW(),
        NOW(),
        NOW()
    )
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
    updated_at = NOW();

SET @img_cardigan = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 10 12"><rect width="10" height="12" fill="%23eadfc9"/></svg>';
SET @img_shirt = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 10 12"><rect width="10" height="12" fill="%23dfe7f2"/></svg>';
SET @img_pants = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 10 12"><rect width="10" height="12" fill="%23d8d2ca"/></svg>';
SET @img_skirt = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 10 12"><rect width="10" height="12" fill="%23e6d6d6"/></svg>';
SET @img_coat = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 10 12"><rect width="10" height="12" fill="%23d6deea"/></svg>';

INSERT INTO clothings (
    user_id, name, category, sub_category, primary_color, secondary_color, material, pattern, fit, size,
    style_tags, occasion_tags, season_tags, frequency_level, wear_count, image_url, image_urls, thumbnail_url,
    brand, is_deleted, source_type, ai_confidence, created_at, updated_at
) VALUES
    (
        @user_id, 'Ivory Knit Cardigan', 'Top', 'Cardigan', 'Ivory', 'Beige', 'Knit', 'Solid', 'Relaxed', 'M',
        JSON_ARRAY('commute', 'minimal', 'soft'),
        JSON_ARRAY('commute', 'daily'),
        JSON_ARRAY('spring', 'autumn'),
        4, 12, @img_cardigan, JSON_ARRAY(@img_cardigan), @img_cardigan,
        'Ecru Basic', 0, 'manual', 0.98, DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)
    );
SET @user_clothing_1 = LAST_INSERT_ID();

INSERT INTO clothings (
    user_id, name, category, sub_category, primary_color, secondary_color, material, pattern, fit, size,
    style_tags, occasion_tags, season_tags, frequency_level, wear_count, image_url, image_urls, thumbnail_url,
    brand, is_deleted, source_type, ai_confidence, created_at, updated_at
) VALUES
    (
        @user_id, 'Light Blue Office Shirt', 'Top', 'Shirt', 'Light Blue', 'White', 'Cotton', 'Solid', 'Regular', 'M',
        JSON_ARRAY('commute', 'clean'),
        JSON_ARRAY('commute', 'meeting'),
        JSON_ARRAY('spring', 'summer', 'autumn'),
        5, 18, @img_shirt, JSON_ARRAY(@img_shirt), @img_shirt,
        'Office Line', 0, 'manual', 0.99, DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)
    );
SET @user_clothing_2 = LAST_INSERT_ID();

INSERT INTO clothings (
    user_id, name, category, sub_category, primary_color, secondary_color, material, pattern, fit, size,
    style_tags, occasion_tags, season_tags, frequency_level, wear_count, image_url, image_urls, thumbnail_url,
    brand, is_deleted, source_type, ai_confidence, created_at, updated_at
) VALUES
    (
        @user_id, 'Charcoal Straight Pants', 'Bottom', 'Trousers', 'Charcoal', 'Black', 'Blend', 'Solid', 'Straight', 'M',
        JSON_ARRAY('commute', 'minimal'),
        JSON_ARRAY('commute', 'daily'),
        JSON_ARRAY('spring', 'autumn', 'winter'),
        5, 20, @img_pants, JSON_ARRAY(@img_pants), @img_pants,
        'Urban Tailor', 0, 'manual', 0.97, DATE_SUB(NOW(), INTERVAL 25 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)
    );
SET @user_clothing_3 = LAST_INSERT_ID();

INSERT INTO clothings (
    user_id, name, category, sub_category, primary_color, secondary_color, material, pattern, fit, size,
    style_tags, occasion_tags, season_tags, frequency_level, wear_count, image_url, image_urls, thumbnail_url,
    brand, is_deleted, source_type, ai_confidence, created_at, updated_at
) VALUES
    (
        @admin_id, 'Navy Long Coat', 'Outerwear', 'Coat', 'Navy', 'Dark Gray', 'Cotton', 'Solid', 'Regular', 'L',
        JSON_ARRAY('commute', 'academy', 'clean'),
        JSON_ARRAY('commute', 'business-trip'),
        JSON_ARRAY('spring', 'autumn'),
        4, 9, @img_coat, JSON_ARRAY(@img_coat), @img_coat,
        'Faculty Select', 0, 'manual', 0.96, DATE_SUB(NOW(), INTERVAL 28 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)
    );
SET @admin_clothing_1 = LAST_INSERT_ID();

INSERT INTO clothings (
    user_id, name, category, sub_category, primary_color, secondary_color, material, pattern, fit, size,
    style_tags, occasion_tags, season_tags, frequency_level, wear_count, image_url, image_urls, thumbnail_url,
    brand, is_deleted, source_type, ai_confidence, created_at, updated_at
) VALUES
    (
        @admin_id, 'Dusty Pink Midi Skirt', 'Bottom', 'Skirt', 'Dusty Pink', 'Ivory', 'Polyester', 'Solid', 'A-line', 'L',
        JSON_ARRAY('academy', 'elegant'),
        JSON_ARRAY('commute', 'lecture'),
        JSON_ARRAY('spring', 'autumn'),
        3, 6, @img_skirt, JSON_ARRAY(@img_skirt), @img_skirt,
        'Faculty Select', 0, 'manual', 0.94, DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)
    );
SET @admin_clothing_2 = LAST_INSERT_ID();

INSERT INTO ai_conversations (
    user_id, session_id, title, context, is_active, message_count, metadata, created_at, updated_at
) VALUES
    (
        @user_id,
        CONCAT('demo-session-', @user_id),
        'Shanghai commute look',
        'outfit',
        1,
        2,
        JSON_OBJECT('location', 'Shanghai', 'occasion', 'commute', 'weather', 'cloudy 20C'),
        DATE_SUB(NOW(), INTERVAL 2 HOUR),
        DATE_SUB(NOW(), INTERVAL 2 HOUR)
    );
SET @conversation_id = LAST_INSERT_ID();

INSERT INTO ai_chat_messages (
    conversation_id, user_id, role, content, message_type, recommendations, context_snapshot, metadata, created_at
) VALUES
    (
        @conversation_id,
        @user_id,
        'user',
        'Tomorrow in Shanghai it will be about 20C. Please suggest a commute outfit that feels light and clean.',
        'text',
        NULL,
        JSON_OBJECT('location', 'Shanghai', 'occasion', 'commute'),
        JSON_OBJECT('source', 'demo-seed'),
        DATE_SUB(NOW(), INTERVAL 2 HOUR)
    ),
    (
        @conversation_id,
        @user_id,
        'assistant',
        'Try the light blue office shirt with charcoal straight pants, then add the ivory cardigan if you expect strong office air conditioning.',
        'recommendation',
        JSON_ARRAY(
            JSON_OBJECT('clothingId', @user_clothing_2, 'name', 'Light Blue Office Shirt', 'reason', 'Brightens the face and keeps the outfit professional'),
            JSON_OBJECT('clothingId', @user_clothing_3, 'name', 'Charcoal Straight Pants', 'reason', 'Keeps the line clean and helps the silhouette look longer')
        ),
        JSON_OBJECT('location', 'Shanghai', 'occasion', 'commute', 'temperature', 20),
        JSON_OBJECT('source', 'demo-seed', 'tokens', 856),
        DATE_SUB(NOW(), INTERVAL 119 MINUTE)
    );

INSERT INTO outfit_advice_records (
    user_id, input_type, input_description, location, temperature, weather_condition, season, time_of_day,
    detected_items, detected_style, color_analysis, outfit_name, outfit_description, reasoning, fashion_suggestions,
    purchase_recommendations, occasion, suitability_score, is_favorite, is_deleted, created_at, updated_at
) VALUES
    (
        @user_id,
        2,
        'Tomorrow in Shanghai is about 20C and I need a light commute outfit.',
        'Shanghai',
        20.0,
        'Cloudy',
        'Spring',
        'Morning',
        JSON_ARRAY(
            JSON_OBJECT('item', 'Light Blue Office Shirt', 'color', 'Light Blue', 'category', 'Top'),
            JSON_OBJECT('item', 'Charcoal Straight Pants', 'color', 'Charcoal', 'category', 'Bottom')
        ),
        'Minimal Commute',
        'Low saturation colors keep the outfit clean and office friendly.',
        'Light commute layering',
        'Pair the blue shirt with charcoal straight pants, then add the ivory cardigan if the office is cold.',
        'The shirt lifts the upper body visually, the pants keep the lower line stable, and the cardigan adds a soft extra layer without bulk.',
        'Choose loafers or plain white sneakers. A dark brown or black bag will keep the outfit grounded.',
        JSON_ARRAY(
            JSON_OBJECT('name', 'Light Knit Shawl', 'reason', 'Useful for office AC and easy to layer', 'link', 'https://example.com/demo-knit')
        ),
        'Commute',
        0.93,
        1,
        0,
        DATE_SUB(NOW(), INTERVAL 115 MINUTE),
        DATE_SUB(NOW(), INTERVAL 90 MINUTE)
    );
SET @outfit_id = LAST_INSERT_ID();

INSERT INTO outfit_items (
    outfit_advice_id, clothing_id, item_name, item_category, item_color, item_image_url, is_recommended, reason, sort_order, created_at
) VALUES
    (@outfit_id, @user_clothing_2, 'Light Blue Office Shirt', 'Top', 'Light Blue', @img_shirt, 0, 'Keeps the look clean and professional.', 1, DATE_SUB(NOW(), INTERVAL 115 MINUTE)),
    (@outfit_id, @user_clothing_3, 'Charcoal Straight Pants', 'Bottom', 'Charcoal', @img_pants, 0, 'Makes the body line look longer and less bulky.', 2, DATE_SUB(NOW(), INTERVAL 115 MINUTE)),
    (@outfit_id, NULL, 'Light Knit Shawl', 'Outerwear', 'Ivory', @img_cardigan, 1, 'Useful as a light extra layer indoors.', 3, DATE_SUB(NOW(), INTERVAL 115 MINUTE));

INSERT INTO outfit_feedback (
    outfit_advice_id, user_id, overall_rating, style_rating, practicality_rating, weather_rating, is_worn, worn_at, feedback_text, created_at, updated_at
) VALUES
    (
        @outfit_id,
        @user_id,
        5,
        5,
        4,
        5,
        1,
        CURRENT_DATE,
        'This look works well for a weekday commute. The blue shirt feels sharp and the cardigan helps inside air conditioned rooms.',
        DATE_SUB(NOW(), INTERVAL 70 MINUTE),
        DATE_SUB(NOW(), INTERVAL 45 MINUTE)
    );

INSERT INTO ai_api_call_record (
    scene, model, request_id, user_id, status, http_code, error_type, error_message,
    response_time, input_tokens, output_tokens, total_tokens, prompt_length, response_length, created_at, create_date
) VALUES
    (
        'chat', 'qwen3-vl-32b-instruct', CONCAT('req-', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 6 DAY))), @user_id,
        1, 200, NULL, NULL, 1430, 420, 280, 700, 312, 488,
        DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 6 DAY), '%Y-%m-%d')
    ),
    (
        'outfit_advice', 'qwen3-vl-32b-instruct', CONCAT('req-', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 3 DAY))), @user_id,
        1, 200, NULL, NULL, 1680, 520, 360, 880, 428, 620,
        DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 3 DAY), '%Y-%m-%d')
    ),
    (
        'image_analysis', 'qwen3-vl-32b-instruct', CONCAT('req-', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 12 HOUR))), @admin_id,
        1, 200, NULL, NULL, 1260, 380, 220, 600, 290, 360,
        DATE_SUB(NOW(), INTERVAL 12 HOUR), DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 12 HOUR), '%Y-%m-%d')
    ),
    (
        'chat', 'qwen3-vl-32b-instruct', CONCAT('req-', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 2 HOUR))), @user_id,
        1, 200, NULL, NULL, 980, 330, 250, 580, 280, 402,
        DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 2 HOUR), '%Y-%m-%d')
    ),
    (
        'outfit_advice', 'qwen3-vl-32b-instruct', CONCAT('req-', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 80 MINUTE))), @user_id,
        1, 200, NULL, NULL, 1520, 540, 340, 880, 410, 598,
        DATE_SUB(NOW(), INTERVAL 80 MINUTE), DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 80 MINUTE), '%Y-%m-%d')
    ),
    (
        'chat', 'qwen3-vl-32b-instruct', CONCAT('req-', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 20 MINUTE))), @user_id,
        0, 504, 'timeout', 'Demo timeout for monitor page', 3200, 260, 0, 260, 240, 0,
        DATE_SUB(NOW(), INTERVAL 20 MINUTE), DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 20 MINUTE), '%Y-%m-%d')
    );

SELECT 'Demo data seeded successfully.' AS message,
       'zhangsan / 123456 (ADMIN)' AS admin_account,
       'testuser / 123456 (USER)' AS user_account;
