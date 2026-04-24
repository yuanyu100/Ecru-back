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

CREATE TABLE IF NOT EXISTS style_tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    category VARCHAR(50) NULL,
    is_preset TINYINT(1) NOT NULL DEFAULT 1,
    description VARCHAR(255) NULL,
    usage_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_style_tags_name_category (name, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS style_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    image_url VARCHAR(500) NOT NULL,
    title VARCHAR(200) NULL,
    source VARCHAR(50) NULL,
    source_url VARCHAR(500) NULL,
    price DECIMAL(10,2) NULL,
    style_category VARCHAR(50) NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS style_image_tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    image_id BIGINT NOT NULL,
    style_tag_id BIGINT NOT NULL,
    confidence DECIMAL(3,2) NOT NULL DEFAULT 1.00,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_style_image_tags_image_tag (image_id, style_tag_id),
    KEY idx_style_image_tags_image_id (image_id),
    KEY idx_style_image_tags_tag_id (style_tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_style_preference_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    image_id BIGINT NOT NULL,
    preference_type TINYINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_style_preference_logs_user_image (user_id, image_id),
    KEY idx_user_style_preference_logs_user_id (user_id),
    KEY idx_user_style_preference_logs_image_id (image_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_style_profile (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    style_tag_id BIGINT NOT NULL,
    preference_score DECIMAL(5,4) NOT NULL DEFAULT 0.0000,
    interaction_count INT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_style_profile_user_tag (user_id, style_tag_id),
    KEY idx_user_style_profile_user_id (user_id),
    KEY idx_user_style_profile_tag_id (style_tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS knowledge_fabrics (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    alias VARCHAR(255) NULL,
    fabric_type VARCHAR(50) NULL,
    warmth_score INT NOT NULL DEFAULT 0,
    breathability_score INT NOT NULL DEFAULT 0,
    comfort_score INT NOT NULL DEFAULT 0,
    durability_score INT NOT NULL DEFAULT 0,
    summary VARCHAR(255) NULL,
    properties TEXT NULL,
    care_guide TEXT NULL,
    suitable_seasons VARCHAR(100) NULL,
    suitable_occasions VARCHAR(255) NULL,
    keywords VARCHAR(255) NULL,
    source VARCHAR(100) NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_knowledge_fabrics_name (name),
    KEY idx_knowledge_fabrics_type (fabric_type),
    KEY idx_knowledge_fabrics_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS knowledge_guides (
    id BIGINT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    subtitle VARCHAR(255) NULL,
    guide_type VARCHAR(50) NULL,
    summary VARCHAR(255) NULL,
    content TEXT NULL,
    author VARCHAR(100) NULL,
    publish_date DATE NULL,
    tags VARCHAR(255) NULL,
    cover_image_url VARCHAR(500) NULL,
    cover_image_caption VARCHAR(255) NULL,
    keywords VARCHAR(255) NULL,
    source VARCHAR(100) NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_knowledge_guides_title (title),
    KEY idx_knowledge_guides_type (guide_type),
    KEY idx_knowledge_guides_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS knowledge_care_labels (
    id BIGINT PRIMARY KEY,
    symbol_code VARCHAR(50) NOT NULL,
    symbol_name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    instruction_text VARCHAR(255) NOT NULL,
    explanation TEXT NULL,
    do_text VARCHAR(255) NULL,
    dont_text VARCHAR(255) NULL,
    keywords VARCHAR(255) NULL,
    source VARCHAR(100) NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_knowledge_care_labels_code (symbol_code),
    KEY idx_knowledge_care_labels_category (category),
    KEY idx_knowledge_care_labels_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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

DELETE FROM user_style_profile
WHERE user_id IN (@admin_id, @user_id);

DELETE FROM user_style_preference_logs
WHERE user_id IN (@admin_id, @user_id);

DELETE FROM user_settings
WHERE user_id IN (@admin_id, @user_id);

DELETE FROM ai_api_call_record
WHERE user_id IN (@admin_id, @user_id);

DELETE FROM knowledge_guides
WHERE id IN (2001, 2002, 2003);

DELETE FROM knowledge_fabrics
WHERE id IN (1001, 1002, 1003, 1004);

DELETE FROM knowledge_care_labels
WHERE id IN (3001, 3002, 3003, 3004, 3005, 3006);

DELETE sit
FROM style_image_tags sit
JOIN style_images si ON si.id = sit.image_id
WHERE si.title IN (
    'Demo Commute Minimal',
    'Demo Soft Office',
    'Demo Clean Academia',
    'Demo Weekend Casual',
    'Demo Vintage Layering',
    'Demo Sharp Tailoring'
);

DELETE FROM style_images
WHERE title IN (
    'Demo Commute Minimal',
    'Demo Soft Office',
    'Demo Clean Academia',
    'Demo Weekend Casual',
    'Demo Vintage Layering',
    'Demo Sharp Tailoring'
);

INSERT INTO style_tags (name, category, is_preset, description, usage_count, created_at, updated_at) VALUES
    ('commute', 'style', 1, 'Looks suited for weekday commute and office scenes', 12, NOW(), NOW()),
    ('minimal', 'style', 1, 'Low saturation and clean silhouette direction', 9, NOW(), NOW()),
    ('soft', 'mood', 1, 'Gentle and light visual impression', 7, NOW(), NOW()),
    ('academia', 'style', 1, 'Bookish and clean campus-inspired styling', 6, NOW(), NOW()),
    ('casual', 'style', 1, 'Relaxed and everyday weekend direction', 10, NOW(), NOW()),
    ('vintage', 'style', 1, 'Layered styling with a retro tone', 5, NOW(), NOW()),
    ('sharp', 'mood', 1, 'Crisp lines and tailored visual impression', 8, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    is_preset = VALUES(is_preset),
    description = VALUES(description),
    usage_count = VALUES(usage_count),
    updated_at = NOW();

SET @style_img_commute_minimal = 'https://dummyimage.com/900x1200/e9dcc7/624720.png&text=COMMUTE';
SET @style_img_soft_office = 'https://dummyimage.com/900x1200/ecd8d5/71544f.png&text=SOFT';
SET @style_img_clean_academia = 'https://dummyimage.com/900x1200/dbe4ef/3a4c63.png&text=ACADEMIA';
SET @style_img_weekend_casual = 'https://dummyimage.com/900x1200/e7dfd4/54402b.png&text=CASUAL';
SET @style_img_vintage_layering = 'https://dummyimage.com/900x1200/d7cbbf/573e2a.png&text=VINTAGE';
SET @style_img_sharp_tailoring = 'https://dummyimage.com/900x1200/d8dde5/384557.png&text=SHARP';

INSERT INTO style_images (image_url, title, source, source_url, price, style_category, is_active, created_at, updated_at) VALUES
    (@style_img_commute_minimal, 'Demo Commute Minimal', 'demo-seed', 'https://example.com/style/commute-minimal', 299.00, 'commute', 1, NOW(), NOW()),
    (@style_img_soft_office, 'Demo Soft Office', 'demo-seed', 'https://example.com/style/soft-office', 269.00, 'commute', 1, NOW(), NOW()),
    (@style_img_clean_academia, 'Demo Clean Academia', 'demo-seed', 'https://example.com/style/clean-academia', 329.00, 'academia', 1, NOW(), NOW()),
    (@style_img_weekend_casual, 'Demo Weekend Casual', 'demo-seed', 'https://example.com/style/weekend-casual', 219.00, 'casual', 1, NOW(), NOW()),
    (@style_img_vintage_layering, 'Demo Vintage Layering', 'demo-seed', 'https://example.com/style/vintage-layering', 359.00, 'vintage', 1, NOW(), NOW()),
    (@style_img_sharp_tailoring, 'Demo Sharp Tailoring', 'demo-seed', 'https://example.com/style/sharp-tailoring', 399.00, 'commute', 1, NOW(), NOW());

SET @style_image_1 = (SELECT id FROM style_images WHERE title = 'Demo Commute Minimal' ORDER BY id DESC LIMIT 1);
SET @style_image_2 = (SELECT id FROM style_images WHERE title = 'Demo Soft Office' ORDER BY id DESC LIMIT 1);
SET @style_image_3 = (SELECT id FROM style_images WHERE title = 'Demo Clean Academia' ORDER BY id DESC LIMIT 1);
SET @style_image_4 = (SELECT id FROM style_images WHERE title = 'Demo Weekend Casual' ORDER BY id DESC LIMIT 1);
SET @style_image_5 = (SELECT id FROM style_images WHERE title = 'Demo Vintage Layering' ORDER BY id DESC LIMIT 1);
SET @style_image_6 = (SELECT id FROM style_images WHERE title = 'Demo Sharp Tailoring' ORDER BY id DESC LIMIT 1);

SET @style_tag_commute = (SELECT id FROM style_tags WHERE name = 'commute' AND category = 'style' LIMIT 1);
SET @style_tag_minimal = (SELECT id FROM style_tags WHERE name = 'minimal' AND category = 'style' LIMIT 1);
SET @style_tag_soft = (SELECT id FROM style_tags WHERE name = 'soft' AND category = 'mood' LIMIT 1);
SET @style_tag_academia = (SELECT id FROM style_tags WHERE name = 'academia' AND category = 'style' LIMIT 1);
SET @style_tag_casual = (SELECT id FROM style_tags WHERE name = 'casual' AND category = 'style' LIMIT 1);
SET @style_tag_vintage = (SELECT id FROM style_tags WHERE name = 'vintage' AND category = 'style' LIMIT 1);
SET @style_tag_sharp = (SELECT id FROM style_tags WHERE name = 'sharp' AND category = 'mood' LIMIT 1);

INSERT INTO style_image_tags (image_id, style_tag_id, confidence, created_at) VALUES
    (@style_image_1, @style_tag_commute, 0.98, NOW()),
    (@style_image_1, @style_tag_minimal, 0.94, NOW()),
    (@style_image_2, @style_tag_commute, 0.92, NOW()),
    (@style_image_2, @style_tag_soft, 0.88, NOW()),
    (@style_image_3, @style_tag_academia, 0.97, NOW()),
    (@style_image_3, @style_tag_minimal, 0.78, NOW()),
    (@style_image_4, @style_tag_casual, 0.96, NOW()),
    (@style_image_4, @style_tag_soft, 0.72, NOW()),
    (@style_image_5, @style_tag_vintage, 0.98, NOW()),
    (@style_image_5, @style_tag_soft, 0.64, NOW()),
    (@style_image_6, @style_tag_commute, 0.91, NOW()),
    (@style_image_6, @style_tag_sharp, 0.96, NOW())
ON DUPLICATE KEY UPDATE
    confidence = VALUES(confidence);

INSERT INTO user_style_preference_logs (user_id, image_id, preference_type, created_at) VALUES
    (@user_id, @style_image_1, 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),
    (@user_id, @style_image_2, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (@user_id, @style_image_4, 0, DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (@user_id, @style_image_5, 2, DATE_SUB(NOW(), INTERVAL 20 HOUR)),
    (@admin_id, @style_image_6, 1, DATE_SUB(NOW(), INTERVAL 10 HOUR))
ON DUPLICATE KEY UPDATE
    preference_type = VALUES(preference_type),
    created_at = VALUES(created_at);

INSERT INTO user_style_profile (user_id, style_tag_id, preference_score, interaction_count, updated_at) VALUES
    (@user_id, @style_tag_commute, 0.2800, 2, NOW()),
    (@user_id, @style_tag_minimal, 0.1800, 2, NOW()),
    (@user_id, @style_tag_soft, 0.0800, 2, NOW()),
    (@user_id, @style_tag_vintage, -0.1000, 1, NOW()),
    (@admin_id, @style_tag_sharp, 0.1000, 1, NOW()),
    (@admin_id, @style_tag_commute, 0.0900, 1, NOW())
ON DUPLICATE KEY UPDATE
    preference_score = VALUES(preference_score),
    interaction_count = VALUES(interaction_count),
    updated_at = NOW();

INSERT INTO knowledge_fabrics (
    id, name, alias, fabric_type, warmth_score, breathability_score, comfort_score, durability_score,
    summary, properties, care_guide, suitable_seasons, suitable_occasions, keywords, source, is_active,
    created_at, updated_at
) VALUES
    (
        1001, 'Wool', 'merino,wool blend', 'natural_fiber', 92, 70, 85, 78,
        'Warm and structured fabric that works well for cold weather tailoring.',
        'Wool keeps heat well, recovers shape after wear, and suits coats, knitwear, and formal trousers.',
        'Use gentle detergent, avoid high heat, and dry flat or hang with shoulder support.',
        'autumn,winter',
        'commute,business,formal',
        'wool,warm,winter,coat,sweater,formal',
        'demo-seed',
        1,
        NOW(),
        NOW()
    ),
    (
        1002, 'Cotton', 'cotton poplin,jersey', 'natural_fiber', 58, 92, 90, 82,
        'Balanced daily fabric with strong breathability and easy maintenance.',
        'Cotton is comfortable on skin, works across shirts and tees, and handles frequent wear well.',
        'Machine wash in mild cycle, separate dark colors first, and avoid over-drying.',
        'spring,summer,autumn',
        'daily,commute,weekend',
        'cotton,shirt,tshirt,breathable,daily,office',
        'demo-seed',
        1,
        NOW(),
        NOW()
    ),
    (
        1003, 'Linen', 'linen blend', 'natural_fiber', 42, 95, 82, 68,
        'Dry and airy fabric suited to hot weather and relaxed summer outfits.',
        'Linen releases heat quickly, has a natural texture, and creates an effortless seasonal look.',
        'Wash with cold water, do not over-spin, and steam lightly to keep texture natural.',
        'spring,summer',
        'daily,vacation,smart_casual',
        'linen,summer,cool,breathable,shirt,relaxed',
        'demo-seed',
        1,
        NOW(),
        NOW()
    ),
    (
        1004, 'Denim', 'rigid denim,washed denim', 'woven_fabric', 64, 66, 78, 90,
        'Durable fabric for casual outfits, outer layers, and structured bottoms.',
        'Denim is sturdy, resistant to abrasion, and easy to pair with shirts, knits, and sneakers.',
        'Wash inside out, reduce wash frequency, and air dry to preserve color and shape.',
        'spring,autumn,winter',
        'daily,casual,weekend',
        'denim,jeans,jacket,casual,durable,weekend',
        'demo-seed',
        1,
        NOW(),
        NOW()
    )
ON DUPLICATE KEY UPDATE
    alias = VALUES(alias),
    fabric_type = VALUES(fabric_type),
    warmth_score = VALUES(warmth_score),
    breathability_score = VALUES(breathability_score),
    comfort_score = VALUES(comfort_score),
    durability_score = VALUES(durability_score),
    summary = VALUES(summary),
    properties = VALUES(properties),
    care_guide = VALUES(care_guide),
    suitable_seasons = VALUES(suitable_seasons),
    suitable_occasions = VALUES(suitable_occasions),
    keywords = VALUES(keywords),
    source = VALUES(source),
    is_active = VALUES(is_active),
    updated_at = NOW();

SET @guide_img_layering = 'https://dummyimage.com/900x600/e6dccf/4e4030.png&text=LAYERING';
SET @guide_img_wool = 'https://dummyimage.com/900x600/dfe2ea/33445a.png&text=WOOL';
SET @guide_img_interview = 'https://dummyimage.com/900x600/e9eef3/446074.png&text=INTERVIEW';

INSERT INTO knowledge_guides (
    id, title, subtitle, guide_type, summary, content, author, publish_date, tags,
    cover_image_url, cover_image_caption, keywords, source, is_active, created_at, updated_at
) VALUES
    (
        2001,
        'Spring Commute Layering Guide',
        'Light office layering for 16C to 24C weekdays',
        'match',
        'A quick guide for building clean commute outfits with one light extra layer.',
        'Start with a breathable base such as a cotton shirt or fine knit. Add one light cardigan or unstructured blazer, keep the palette within two to three low saturation colors, and let trousers or skirts provide a stable line. This approach keeps the outfit sharp in transit and adaptable once you enter strong indoor air conditioning.',
        'Ecru Demo Editorial',
        '2026-04-18',
        'spring,commute,layering,minimal',
        @guide_img_layering,
        'Light layering reference for weekday commute looks',
        'commute,office,layering,shirt,cardigan,formal',
        'demo-seed',
        1,
        NOW(),
        NOW()
    ),
    (
        2002,
        'Winter Wool Care and Styling Notes',
        'How to keep wool pieces warm, sharp, and easy to maintain',
        'fabric',
        'Practical notes for using wool in winter outfits without making the silhouette heavy.',
        'Choose wool outerwear when you need warmth with structure. If the coat already carries visual weight, keep the inner layer lighter and cleaner, such as a fine knit or plain shirt. For care, reduce unnecessary washing, use steam between wears, and store with shoulder support so the shape stays stable through the season.',
        'Ecru Demo Editorial',
        '2026-04-12',
        'winter,wool,care,formal,coat',
        @guide_img_wool,
        'Wool coat and knit styling reference',
        'wool,winter,care,coat,warm,formal',
        'demo-seed',
        1,
        NOW(),
        NOW()
    ),
    (
        2003,
        'Interview White Shirt Pairing Notes',
        'Simple combinations for a reliable first impression',
        'style',
        'A compact checklist for pairing a white shirt in interview and formal commute scenes.',
        'A white shirt works best when the lower piece is darker and cleaner in line, such as charcoal trousers or a navy skirt. Avoid adding too many strong accessories. If the weather is cool, choose a thin neutral cardigan or blazer instead of a bulky hoodie. Shoes and bag should stay quiet so the overall message remains professional and controlled.',
        'Ecru Demo Editorial',
        '2026-04-20',
        'interview,shirt,formal,commute',
        @guide_img_interview,
        'White shirt pairing example for formal scenarios',
        'interview,shirt,white,formal,commute,office',
        'demo-seed',
        1,
        NOW(),
        NOW()
    )
ON DUPLICATE KEY UPDATE
    subtitle = VALUES(subtitle),
    guide_type = VALUES(guide_type),
    summary = VALUES(summary),
    content = VALUES(content),
    author = VALUES(author),
    publish_date = VALUES(publish_date),
    tags = VALUES(tags),
    cover_image_url = VALUES(cover_image_url),
    cover_image_caption = VALUES(cover_image_caption),
    keywords = VALUES(keywords),
    source = VALUES(source),
    is_active = VALUES(is_active),
    updated_at = NOW();

INSERT INTO knowledge_care_labels (
    id, symbol_code, symbol_name, category, instruction_text, explanation,
    do_text, dont_text, keywords, source, is_active, created_at, updated_at
) VALUES
    (
        3001,
        'wash-30',
        'Wash at or below 30C',
        'wash',
        'Use water temperature up to 30C for normal washing.',
        'Suitable for daily garments that may shrink or fade under higher temperature.',
        'Use mild detergent and short cycle when possible.',
        'Do not use hot water or long high-speed washing by default.',
        'wash,30c,machine wash,cold water,daily',
        'demo-seed',
        1,
        NOW(),
        NOW()
    ),
    (
        3002,
        'hand-wash',
        'Hand wash only',
        'wash',
        'Gently hand wash the garment in cool or lukewarm water.',
        'Usually used for delicate fabric, structured trims, or items that deform easily in machines.',
        'Soak briefly and press water out gently.',
        'Do not twist hard or machine wash.',
        'hand wash,delicate,wash,gentle',
        'demo-seed',
        1,
        NOW(),
        NOW()
    ),
    (
        3003,
        'no-bleach',
        'Do not bleach',
        'bleach',
        'Avoid chlorine and oxygen bleach on this garment.',
        'Bleach may damage dye, weaken fiber, or leave uneven color marks.',
        'Use neutral detergent for stain treatment first.',
        'Do not use bleach products directly on fabric.',
        'bleach,no bleach,stain,color care',
        'demo-seed',
        1,
        NOW(),
        NOW()
    ),
    (
        3004,
        'low-iron',
        'Iron at low temperature',
        'iron',
        'Iron the garment at low temperature, ideally with steam cloth protection.',
        'Best for blended, silk-like, printed, or heat-sensitive fabrics.',
        'Test a hidden corner first and iron from the reverse side when possible.',
        'Do not use high direct heat on the fabric surface.',
        'iron,low heat,press,wrinkle care',
        'demo-seed',
        1,
        NOW(),
        NOW()
    ),
    (
        3005,
        'no-tumble-dry',
        'Do not tumble dry',
        'dry',
        'Do not use tumble dryer; air dry the garment instead.',
        'High heat may shrink knitwear, damage elastane, or make the surface feel rough.',
        'Lay flat or hang according to garment weight and structure.',
        'Do not dry with high heat machine drying.',
        'dry,tumble dry,no tumble dry,air dry',
        'demo-seed',
        1,
        NOW(),
        NOW()
    ),
    (
        3006,
        'dry-clean',
        'Professional dry clean recommended',
        'clean',
        'Send the garment to professional dry cleaning when routine care is needed.',
        'Often used for wool coats, tailored pieces, lined garments, or items with complex structure.',
        'Store with breathable garment bag between wears.',
        'Do not repeatedly home wash unless the fabric and construction clearly allow it.',
        'dry clean,coat,wool,tailoring,formal',
        'demo-seed',
        1,
        NOW(),
        NOW()
    )
ON DUPLICATE KEY UPDATE
    symbol_code = VALUES(symbol_code),
    symbol_name = VALUES(symbol_name),
    category = VALUES(category),
    instruction_text = VALUES(instruction_text),
    explanation = VALUES(explanation),
    do_text = VALUES(do_text),
    dont_text = VALUES(dont_text),
    keywords = VALUES(keywords),
    source = VALUES(source),
    is_active = VALUES(is_active),
    updated_at = NOW();

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
