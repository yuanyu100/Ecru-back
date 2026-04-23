ALTER TABLE users
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色：ADMIN/USER' AFTER nickname;

UPDATE users
SET role = 'ADMIN'
WHERE id = 1;

UPDATE users
SET role = 'USER'
WHERE role IS NULL OR role = '';

CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_clothings_user_deleted_created ON clothings(user_id, is_deleted, created_at);
CREATE INDEX idx_clothings_category_color ON clothings(category, primary_color);
