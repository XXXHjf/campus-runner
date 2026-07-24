-- ============================================================
-- 校园二手交易 V1 数据库脚本
-- ------------------------------------------------------------
-- 用途：
-- 1. 新增二手交易分类、商品、议价、订单、私密留言 5 张业务表。
-- 2. 新增二手交易所需系统配置项。
-- 3. 表创建使用 IF NOT EXISTS，方便本地重复执行。
--
-- 重要约定：
-- - 所有金额字段单位均为“元”，保留 2 位小数。
-- - 微信支付、退款、转账流水继续复用既有 tb_payment_log、
--   tb_refund_info、tb_wx_transfer_log，通过 order_number 关联。
-- - deleted 统一为逻辑删除字段：0 未删除，1 已删除。
-- - 本脚本不删除旧表、不清空数据。
-- ============================================================

-- ============================================================
-- 表：tb_second_hand_category
-- 说明：二手商品分类表，例如数码电子、文具教材、衣物鞋包等。
-- 使用场景：
-- - 小程序二手首页分类筛选。
-- - 后台二手分类管理。
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_second_hand_category` (
  `id` BIGINT AUTO_INCREMENT NOT NULL COMMENT '分类主键id',
  `name` VARCHAR(50) NOT NULL COMMENT '二手分类名称，如数码电子、文具教材',
  `image` VARCHAR(255) NULL COMMENT '分类图标图片地址，可为空',
  `sort` INT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
  `deleted` INT NULL DEFAULT 0 COMMENT '逻辑删除字段：0未删除，1已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='二手交易分类表';

-- ============================================================
-- 表：tb_second_hand_product
-- 说明：二手商品主表。
-- 使用场景：
-- - 卖家发布闲置。
-- - 买家浏览、搜索、筛选商品。
-- - 下单后锁定商品，交易完成后标记售出。
--
-- 商品状态 status：
-- 0 在售：买家可浏览、购买、议价。
-- 1 待支付锁定：已有买家创建待支付订单，30分钟未支付会释放。
-- 2 交易中：买家已支付，等待卖家交付或买家确认。
-- 3 已售出：交易已完成。
-- 4 已下架：卖家或后台主动下架，前台不展示。
--
-- pickup_address_id / pickup_address_snapshot：
-- 自提点来源于用户地址簿，但商品会保存发布时的地址快照，
-- 避免卖家之后修改地址簿导致已发布商品地点变化。
--
-- pickup_only：
-- 1 仅支持买家自提。
-- 0 买家下单前可选择自提或卖家配送。
--
-- support_delivery：
-- 兼容旧字段，含义与 pickup_only 相反。
-- 0 不支持卖家配送，仅支持买家自提。
-- 1 支持卖家配送，买家下单前选择配送地址。
--
-- negotiable：
-- 0 不允许议价。
-- 1 允许议价，同一买家对同一商品最多 3 次，具体次数由系统配置控制。
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_second_hand_product` (
  `id` BIGINT AUTO_INCREMENT NOT NULL COMMENT '商品主键id',
  `seller_id` BIGINT NOT NULL COMMENT '卖家用户id，关联 tb_user.id',
  `school_id` BIGINT NOT NULL COMMENT '学校id，关联 tb_school.id，用于同校商品隔离',
  `compus_id` BIGINT NULL COMMENT '校区id，关联 tb_compus.id，可用于校区筛选',
  `category_id` BIGINT NOT NULL COMMENT '二手分类id，关联 tb_second_hand_category.id',
  `title` VARCHAR(80) NOT NULL COMMENT '商品标题，列表和详情页主标题',
  `description` TEXT NULL COMMENT '商品描述，如使用情况、配件、购买时间、瑕疵说明',
  `images` TEXT NULL COMMENT '商品图片地址集合，当前小程序以英文逗号分隔存储',
  `condition_level` VARCHAR(30) NULL COMMENT '商品成色，如全新、九成新、八成新',
  `price` DECIMAL(10,2) NOT NULL COMMENT '商品售价，买家直接购买时的成交价',
  `pickup_address_id` BIGINT NULL COMMENT '自提点地址簿id，关联 tb_address_book.id，仅作来源记录',
  `pickup_address_snapshot` VARCHAR(500) NULL COMMENT '自提点地址快照，如南校区 宿舍楼 弘毅楼 506室',
  `pickup_location` VARCHAR(255) NOT NULL COMMENT '兼容旧字段：卖家指定自提地点，当前写入自提点快照',
  `pickup_only` INT NULL DEFAULT 1 COMMENT '是否仅自提：1仅自提，0买家可选自提或卖家配送',
  `support_delivery` INT NULL DEFAULT 0 COMMENT '兼容旧字段：是否支持卖家配送，0否，1是',
  `negotiable` INT NULL DEFAULT 1 COMMENT '是否允许议价：0否，1是',
  `status` INT NULL DEFAULT 0 COMMENT '商品状态：0在售，1待支付锁定，2交易中，3已售出，4已下架',
  `view_count` INT NULL DEFAULT 0 COMMENT '浏览次数，用于后续热度排序或数据统计',
  `favorite_count` INT NULL DEFAULT 0 COMMENT '收藏次数，预留字段，当前V1未实现收藏功能',
  `deleted` INT NULL DEFAULT 0 COMMENT '逻辑删除字段：0未删除，1已删除',
  `create_time` DATETIME NULL COMMENT '商品创建时间',
  `update_time` DATETIME NULL COMMENT '商品最后更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_second_hand_product_school_status` (`school_id`, `status`),
  KEY `idx_second_hand_product_seller` (`seller_id`),
  KEY `idx_second_hand_product_category` (`category_id`)
) ENGINE=InnoDB COMMENT='二手商品表';

-- ============================================================
-- 表：tb_second_hand_bargain
-- 说明：二手商品议价记录表。
-- 使用场景：
-- - 买家对商品发起报价。
-- - 卖家接受议价后生成待支付二手订单。
-- - 卖家拒绝后，买家仍可在次数限制内重新议价。
--
-- 议价状态 status：
-- 0 待回复：卖家尚未处理。
-- 1 已接受：卖家接受该报价，并生成待支付订单。
-- 2 已拒绝：卖家拒绝该报价。
-- 3 已失效：商品已被其他订单锁定/成交，或其他议价被接受。
--
-- attempt_no：
-- 同一买家对同一商品第几次议价，用于防止重复骚扰。
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_second_hand_bargain` (
  `id` BIGINT AUTO_INCREMENT NOT NULL COMMENT '议价记录主键id',
  `product_id` BIGINT NOT NULL COMMENT '商品id，关联 tb_second_hand_product.id',
  `buyer_id` BIGINT NOT NULL COMMENT '买家用户id，关联 tb_user.id',
  `seller_id` BIGINT NOT NULL COMMENT '卖家用户id，关联 tb_user.id',
  `offer_price` DECIMAL(10,2) NOT NULL COMMENT '买家本次议价报价金额',
  `message` VARCHAR(255) NULL COMMENT '买家议价留言，如取货时间、价格说明',
  `status` INT NULL DEFAULT 0 COMMENT '议价状态：0待回复，1已接受，2已拒绝，3已失效',
  `attempt_no` INT NULL COMMENT '同一买家对同一商品的第几次议价',
  `deleted` INT NULL DEFAULT 0 COMMENT '逻辑删除字段：0未删除，1已删除',
  `create_time` DATETIME NULL COMMENT '议价创建时间',
  `update_time` DATETIME NULL COMMENT '议价最后更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_second_hand_bargain_product` (`product_id`),
  KEY `idx_second_hand_bargain_buyer_product` (`buyer_id`, `product_id`),
  KEY `idx_second_hand_bargain_seller` (`seller_id`)
) ENGINE=InnoDB COMMENT='二手议价表';

-- ============================================================
-- 表：tb_second_hand_order
-- 说明：二手交易订单表，独立于跑腿订单 tb_orders。
-- 使用场景：
-- - 买家直接购买生成待支付订单。
-- - 卖家接受议价后，按议价金额生成待支付订单。
-- - 买家付款后平台托管资金。
-- - 买家确认收货或超时自动确认后，平台扣服务费并转账给卖家。
--
-- 金额字段：
-- product_amount：商品成交价。
-- pay_amount：买家实际支付金额，V1 等于 product_amount。
-- service_fee_rate：服务费率快照，如 0.0300。
-- service_fee：卖家承担的平台服务费。
-- seller_income：卖家实收金额 = product_amount - service_fee。
--
-- delivery_mode：
-- 0 买家自提。
-- 1 卖家配送。配送费不单独建模，默认包含在商品价内。
--
-- pickup_address_snapshot：
-- 下单时从商品复制的自提点快照。即使卖家之后修改商品或地址簿，
-- 订单仍展示成交时约定的自提地址。
--
-- buyer_delivery_address_id / buyer_delivery_address_snapshot：
-- 买家选择卖家配送时保存的收货地址来源和快照。
-- 自提订单这两个字段为空。
--
-- 订单状态 status：
-- 0 待支付：商品已锁定，30分钟未支付自动关闭并释放商品。
-- 1 已支付待交付：买家已付款，等待卖家自提交付或配送。
-- 2 已交付待确认：卖家已标记交付，等待买家确认收货。
-- 3 已完成：业务完成态，当前实现完成后会进入转账中/转账结果态。
-- 4 已取消：未支付订单取消或超时关闭。
-- 5 退款中：买家付款后、卖家交付前取消，等待微信退款结果。
-- 6 退款成功：微信退款成功，商品可重新释放为在售。
-- 7 退款异常：退款失败或状态异常，需后台处理。
-- 8 转账中：交易完成后，平台正在给卖家微信零钱转账。
-- 9 转账成功：卖家收款成功。
-- 10 转账失败：微信零钱转账失败，需后台处理或重试。
-- 11 纠纷中：预留状态，用于后续争议处理。
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_second_hand_order` (
  `id` BIGINT AUTO_INCREMENT NOT NULL COMMENT '二手订单主键id',
  `order_number` VARCHAR(64) NOT NULL COMMENT '二手交易订单号，也是微信支付/退款/转账关联编号',
  `product_id` BIGINT NOT NULL COMMENT '商品id，关联 tb_second_hand_product.id',
  `bargain_id` BIGINT NULL COMMENT '议价记录id；直接购买时为空，议价成交时关联 tb_second_hand_bargain.id',
  `buyer_id` BIGINT NOT NULL COMMENT '买家用户id，关联 tb_user.id',
  `seller_id` BIGINT NOT NULL COMMENT '卖家用户id，关联 tb_user.id',
  `product_amount` DECIMAL(10,2) NOT NULL COMMENT '商品成交价，直接购买为商品售价，议价成交为卖家接受的报价',
  `pay_amount` DECIMAL(10,2) NOT NULL COMMENT '买家支付金额，V1等于商品成交价',
  `service_fee_rate` DECIMAL(10,4) NULL COMMENT '服务费率快照，默认0.0300，来源于系统配置 second_hand_service_fee_rate',
  `service_fee` DECIMAL(10,2) NULL COMMENT '卖家承担服务费，按商品成交价乘以服务费率计算',
  `seller_income` DECIMAL(10,2) NULL COMMENT '卖家实收金额，商品成交价减服务费',
  `delivery_mode` INT NULL DEFAULT 0 COMMENT '交付方式：0买家自提，1卖家配送',
  `pickup_address_snapshot` VARCHAR(500) NULL COMMENT '订单自提点快照，来源于商品发布时的自提点',
  `buyer_delivery_address_id` BIGINT NULL COMMENT '买家配送地址簿id，关联 tb_address_book.id，仅配送订单使用',
  `buyer_delivery_address_snapshot` VARCHAR(500) NULL COMMENT '买家配送地址快照，仅配送订单使用',
  `delivery_remark` VARCHAR(255) NULL COMMENT '交付备注，如双方协商的配送地点或自提补充说明',
  `status` INT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付待交付，2已交付待确认，3已完成，4已取消，5退款中，6退款成功，7退款异常，8转账中，9转账成功，10转账失败，11纠纷中',
  `pay_time` DATETIME NULL COMMENT '买家支付成功时间，以微信支付回调或查询确认为准',
  `cancel_time` DATETIME NULL COMMENT '订单取消时间',
  `cancel_reason` VARCHAR(255) NULL COMMENT '订单取消原因或退款原因',
  `delivered_time` DATETIME NULL COMMENT '卖家标记已交付时间',
  `confirm_deadline` DATETIME NULL COMMENT '自动确认收货截止时间，默认卖家交付后24小时',
  `finish_time` DATETIME NULL COMMENT '交易完成时间，即确认收货或自动确认时间',
  `transfer_time` DATETIME NULL COMMENT '卖家微信零钱转账成功时间',
  `transfer_fail_reason` VARCHAR(255) NULL COMMENT '卖家微信零钱转账失败原因',
  `deleted` INT NULL DEFAULT 0 COMMENT '逻辑删除字段：0未删除，1已删除',
  `create_time` DATETIME NULL COMMENT '订单创建时间',
  `update_time` DATETIME NULL COMMENT '订单最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_second_hand_order_number` (`order_number`),
  KEY `idx_second_hand_order_product` (`product_id`),
  KEY `idx_second_hand_order_buyer` (`buyer_id`),
  KEY `idx_second_hand_order_seller` (`seller_id`),
  KEY `idx_second_hand_order_status_time` (`status`, `create_time`)
) ENGINE=InnoDB COMMENT='二手交易订单表';

-- ============================================================
-- 表：tb_second_hand_message
-- 说明：二手交易私密留言表。
-- 使用场景：
-- - 买家购买前咨询商品细节。
-- - 卖家和买家协商自提/配送地点。
-- - 后台查看留言作为纠纷判断依据。
--
-- 可见性：
-- - 前台仅发送人和接收人双方可见。
-- - 后台管理员可查看，用于治理和纠纷处理。
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_second_hand_message` (
  `id` BIGINT AUTO_INCREMENT NOT NULL COMMENT '私密留言主键id',
  `product_id` BIGINT NOT NULL COMMENT '商品id，关联 tb_second_hand_product.id',
  `order_id` BIGINT NULL COMMENT '订单id，关联 tb_second_hand_order.id；购买前留言可为空',
  `sender_id` BIGINT NOT NULL COMMENT '发送人用户id，关联 tb_user.id',
  `receiver_id` BIGINT NOT NULL COMMENT '接收人用户id，关联 tb_user.id',
  `content` VARCHAR(500) NOT NULL COMMENT '私密留言内容',
  `deleted` INT NULL DEFAULT 0 COMMENT '逻辑删除字段：0未删除，1已删除',
  `create_time` DATETIME NULL COMMENT '留言创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_second_hand_message_product` (`product_id`),
  KEY `idx_second_hand_message_user` (`sender_id`, `receiver_id`)
) ENGINE=InnoDB COMMENT='二手私密留言表';

-- ============================================================
-- 兼容升级：给已存在的二手商品表补充结构化自提点字段
-- 说明：
-- - CREATE TABLE IF NOT EXISTS 不会修改已存在表结构。
-- - 以下语句通过 information_schema 判断字段是否存在，再决定是否 ALTER。
-- - 适合本地重复执行，不会重复添加字段。
-- ============================================================
SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_second_hand_product' AND COLUMN_NAME = 'pickup_address_id') = 0,
  'ALTER TABLE tb_second_hand_product ADD COLUMN pickup_address_id BIGINT NULL COMMENT ''自提点地址簿id，关联 tb_address_book.id，仅作来源记录'' AFTER price',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_second_hand_product' AND COLUMN_NAME = 'pickup_address_snapshot') = 0,
  'ALTER TABLE tb_second_hand_product ADD COLUMN pickup_address_snapshot VARCHAR(500) NULL COMMENT ''自提点地址快照，如南校区 宿舍楼 弘毅楼 506室'' AFTER pickup_address_id',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_second_hand_product' AND COLUMN_NAME = 'pickup_only') = 0,
  'ALTER TABLE tb_second_hand_product ADD COLUMN pickup_only INT NULL DEFAULT 1 COMMENT ''是否仅自提：1仅自提，0买家可选自提或卖家配送'' AFTER pickup_location',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE tb_second_hand_product
SET pickup_address_snapshot = pickup_location
WHERE pickup_address_snapshot IS NULL AND pickup_location IS NOT NULL;

UPDATE tb_second_hand_product
SET pickup_only = CASE WHEN support_delivery = 1 THEN 0 ELSE 1 END
WHERE pickup_only IS NULL;

-- ============================================================
-- 兼容升级：给已存在的二手订单表补充交付地址快照字段
-- ============================================================
SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_second_hand_order' AND COLUMN_NAME = 'pickup_address_snapshot') = 0,
  'ALTER TABLE tb_second_hand_order ADD COLUMN pickup_address_snapshot VARCHAR(500) NULL COMMENT ''订单自提点快照，来源于商品发布时的自提点'' AFTER delivery_mode',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_second_hand_order' AND COLUMN_NAME = 'buyer_delivery_address_id') = 0,
  'ALTER TABLE tb_second_hand_order ADD COLUMN buyer_delivery_address_id BIGINT NULL COMMENT ''买家配送地址簿id，关联 tb_address_book.id，仅配送订单使用'' AFTER pickup_address_snapshot',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_second_hand_order' AND COLUMN_NAME = 'buyer_delivery_address_snapshot') = 0,
  'ALTER TABLE tb_second_hand_order ADD COLUMN buyer_delivery_address_snapshot VARCHAR(500) NULL COMMENT ''买家配送地址快照，仅配送订单使用'' AFTER buyer_delivery_address_id',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- 系统配置：二手交易服务费率
-- config_key：second_hand_service_fee_rate
-- 默认值：0.03
-- 说明：卖家承担服务费率，0.03 表示按成交价 3% 扣服务费。
-- 示例：商品成交价 100 元，服务费 3 元，卖家实收 97 元。
-- ============================================================
INSERT INTO tb_system_config(config_key, config_value, description)
SELECT 'second_hand_service_fee_rate', '0.03', '二手交易卖家承担服务费率，默认0.03表示3%'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM tb_system_config WHERE config_key = 'second_hand_service_fee_rate');

-- ============================================================
-- 系统配置：待支付锁货超时时间
-- config_key：second_hand_payment_timeout_minutes
-- 默认值：30
-- 说明：二手订单创建后商品会进入“待支付锁定”状态。
--      若买家在配置时间内未支付，定时任务会取消订单并释放商品。
-- ============================================================
INSERT INTO tb_system_config(config_key, config_value, description)
SELECT 'second_hand_payment_timeout_minutes', '30', '二手交易待支付锁货超时时间，单位分钟'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM tb_system_config WHERE config_key = 'second_hand_payment_timeout_minutes');

-- ============================================================
-- 系统配置：自动确认收货时间
-- config_key：second_hand_auto_confirm_hours
-- 默认值：24
-- 说明：卖家标记已交付后，买家可手动确认收货。
--      若买家超过配置时间未确认，定时任务会自动确认并进入卖家结算。
-- ============================================================
INSERT INTO tb_system_config(config_key, config_value, description)
SELECT 'second_hand_auto_confirm_hours', '24', '二手交易自动确认收货时间，单位小时'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM tb_system_config WHERE config_key = 'second_hand_auto_confirm_hours');

-- ============================================================
-- 系统配置：同一买家对同一商品最大议价次数
-- config_key：second_hand_bargain_max_count
-- 默认值：3
-- 说明：用于防止买家反复议价骚扰卖家。
-- ============================================================
INSERT INTO tb_system_config(config_key, config_value, description)
SELECT 'second_hand_bargain_max_count', '3', '同一买家对同一商品最大议价次数'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM tb_system_config WHERE config_key = 'second_hand_bargain_max_count');

-- ============================================================
-- 系统配置：是否允许自买自议价
-- config_key：second_hand_allow_self_trade
-- 默认值：false
-- 说明：生产环境建议保持 false，避免刷单、自成交和异常资金流。
--      本地开发若开启模拟支付，后端会额外允许自买自议价，便于单账号测试完整流程。
-- ============================================================
INSERT INTO tb_system_config(config_key, config_value, description)
SELECT 'second_hand_allow_self_trade', 'false', '二手交易是否允许自己购买或议价自己的商品'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM tb_system_config WHERE config_key = 'second_hand_allow_self_trade');
