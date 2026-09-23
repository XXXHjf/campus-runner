-- 跑腿“代买”业务兼容迁移。执行前必须备份 compus_runner。
-- 新字段均保留旧版 API 可写能力；代买分类默认启用，顺风车仅停止新单。

ALTER TABLE tb_category
    ADD COLUMN category_code VARCHAR(32) NULL COMMENT '稳定业务编码' AFTER category_name,
    ADD COLUMN business_type VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL或PURCHASE' AFTER category_code,
    ADD COLUMN enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许新建并在用户分类中显示' AFTER business_type,
    ADD COLUMN sort_order INT NOT NULL DEFAULT 0 COMMENT '展示顺序' AFTER enabled;

UPDATE tb_category
SET category_code = CASE category_name
    WHEN '顺风车' THEN 'RIDESHARE'
    WHEN '外卖' THEN 'FOOD_DELIVERY'
    WHEN '快递' THEN 'EXPRESS_PICKUP'
    WHEN '大件' THEN 'BULKY_ITEM'
    WHEN '其他' THEN 'OTHER'
    ELSE CONCAT('LEGACY_', id)
END,
business_type = 'NORMAL',
enabled = CASE WHEN category_name = '顺风车' THEN 0 ELSE 1 END,
sort_order = CASE category_name
    WHEN '外卖' THEN 20
    WHEN '快递' THEN 30
    WHEN '大件' THEN 40
    WHEN '其他' THEN 50
    WHEN '顺风车' THEN 90
    ELSE 80
END
WHERE category_code IS NULL;

ALTER TABLE tb_category
    MODIFY category_code VARCHAR(32) NOT NULL COMMENT '稳定业务编码',
    ADD UNIQUE KEY uk_category_code (category_code),
    ADD KEY idx_category_enabled_sort (enabled, sort_order);

INSERT INTO tb_category(category_name, category_code, business_type, enabled, sort_order, deleted)
SELECT '代买', 'PURCHASE', 'PURCHASE', 1, 10, 0
WHERE NOT EXISTS (SELECT 1 FROM tb_category WHERE category_code = 'PURCHASE');

ALTER TABLE tb_orders
    ADD COLUMN business_type VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '订单业务类型快照' AFTER category_id,
    ADD COLUMN product_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '商品金额' AFTER price;

UPDATE tb_orders SET business_type = 'NORMAL', product_amount = 0.00
WHERE business_type IS NULL OR business_type = '' OR product_amount IS NULL;

INSERT INTO tb_system_config(config_key, config_value, description)
SELECT 'runner_transfer_single_max', '200.00', '跑腿订单接单人单笔收款上限'
WHERE NOT EXISTS (SELECT 1 FROM tb_system_config WHERE config_key = 'runner_transfer_single_max');

-- 验证：应有一条 PURCHASE，顺风车 enabled=0，商家单笔转账上限配置为 200.00。
SELECT id, category_name, category_code, business_type, enabled, sort_order
FROM tb_category ORDER BY sort_order, id;
SELECT config_key, config_value FROM tb_system_config
WHERE config_key = 'runner_transfer_single_max';
