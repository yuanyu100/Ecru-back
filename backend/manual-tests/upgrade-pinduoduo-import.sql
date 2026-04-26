ALTER TABLE clothings
    ADD COLUMN source_platform VARCHAR(30) NULL COMMENT '来源平台，如 pinduoduo' AFTER brand,
    ADD COLUMN source_order_id VARCHAR(64) NULL COMMENT '来源订单号' AFTER source_platform,
    ADD COLUMN source_shop_name VARCHAR(255) NULL COMMENT '来源店铺名' AFTER source_order_id,
    ADD COLUMN source_sku_text VARCHAR(255) NULL COMMENT '来源规格文案' AFTER source_shop_name;

CREATE INDEX idx_clothings_source_platform ON clothings(source_platform);
CREATE INDEX idx_clothings_source_order_id ON clothings(source_order_id);
