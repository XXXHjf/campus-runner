-- ============================================================
-- 校园二手交易交付方式字段迁移脚本
-- 归档：旧配送字段清理后不得再次执行；当前结构以 second-hand-schema.sql 为准。
-- ------------------------------------------------------------
-- 用途：
-- - 从旧版“手填自提地点 + support_delivery”升级到：
--   1. 商品发布时保存自提点地址簿来源和地址快照。
--   2. 商品支持“仅自提”开关。
--   3. 订单创建时保存自提点快照和买家配送地址快照。
--
-- 执行时机：
-- - 如果发布闲置或创建二手订单时报 org.springframework.jdbc.BadSqlGrammarException
--   或提示 unknown column / bad SQL grammar，通常就是本地库缺少这些新字段。
--
-- 安全性：
-- - 本脚本不会删除表、不会清空数据。
-- - 使用 information_schema 判断字段是否存在，可重复执行。
-- ============================================================

-- 商品表：自提点地址簿来源 id。
SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_second_hand_product' AND COLUMN_NAME = 'pickup_address_id') = 0,
  'ALTER TABLE tb_second_hand_product ADD COLUMN pickup_address_id BIGINT NULL COMMENT ''自提点地址簿id，关联 tb_address_book.id，仅作来源记录'' AFTER price',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 商品表：发布时的自提点地址快照。
SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_second_hand_product' AND COLUMN_NAME = 'pickup_address_snapshot') = 0,
  'ALTER TABLE tb_second_hand_product ADD COLUMN pickup_address_snapshot VARCHAR(500) NULL COMMENT ''自提点地址快照，如南校区 宿舍楼 弘毅楼 506室'' AFTER pickup_address_id',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 商品表：是否仅支持自提。1 仅自提；0 买家可选自提或卖家配送。
SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_second_hand_product' AND COLUMN_NAME = 'pickup_only') = 0,
  'ALTER TABLE tb_second_hand_product ADD COLUMN pickup_only INT NULL DEFAULT 1 COMMENT ''是否仅自提：1仅自提，0买家可选自提或卖家配送'' AFTER pickup_location',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 旧数据兼容：把旧的 pickup_location 写入新的自提点快照字段。
UPDATE tb_second_hand_product
SET pickup_address_snapshot = pickup_location
WHERE pickup_address_snapshot IS NULL AND pickup_location IS NOT NULL;

-- 旧数据兼容：support_delivery=1 代表可配送，因此 pickup_only=0；否则仅自提。
UPDATE tb_second_hand_product
SET pickup_only = CASE WHEN support_delivery = 1 THEN 0 ELSE 1 END
WHERE pickup_only IS NULL;

-- 订单表：下单时复制的商品自提点快照。
SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_second_hand_order' AND COLUMN_NAME = 'pickup_address_snapshot') = 0,
  'ALTER TABLE tb_second_hand_order ADD COLUMN pickup_address_snapshot VARCHAR(500) NULL COMMENT ''订单自提点快照，来源于商品发布时的自提点'' AFTER delivery_mode',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 订单表：买家配送地址簿来源 id，仅配送订单使用。
SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_second_hand_order' AND COLUMN_NAME = 'buyer_delivery_address_id') = 0,
  'ALTER TABLE tb_second_hand_order ADD COLUMN buyer_delivery_address_id BIGINT NULL COMMENT ''买家配送地址簿id，关联 tb_address_book.id，仅配送订单使用'' AFTER pickup_address_snapshot',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 订单表：买家配送地址快照，仅配送订单使用。
SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_second_hand_order' AND COLUMN_NAME = 'buyer_delivery_address_snapshot') = 0,
  'ALTER TABLE tb_second_hand_order ADD COLUMN buyer_delivery_address_snapshot VARCHAR(500) NULL COMMENT ''买家配送地址快照，仅配送订单使用'' AFTER buyer_delivery_address_id',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
