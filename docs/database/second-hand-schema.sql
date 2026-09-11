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
-- 0 在售：买家可浏览、下单、议价。
-- 1 历史线上锁定：仅保留给 ONLINE 存量待支付订单。
-- 2 交易中：OFFLINE 已下单，或 ONLINE 买家已支付；等待卖家交付或买家确认。
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
  `condition_level` VARCHAR(30) NULL COMMENT '商品成色，如全新、九成新、八成新',
  `price` DECIMAL(10,2) NOT NULL COMMENT '商品售价，买家直接购买时的成交价',
  `pickup_address_id` BIGINT NULL COMMENT '自提点地址簿id，关联 tb_address_book.id，仅作来源记录',
  `pickup_address_snapshot` VARCHAR(500) NULL COMMENT '自提点地址快照，如南校区 宿舍楼 弘毅楼 506室',
  `pickup_only` INT NULL DEFAULT 1 COMMENT '是否仅自提：1仅自提，0买家可选自提或卖家配送',
  `negotiable` INT NULL DEFAULT 1 COMMENT '是否允许议价：0否，1是',
  `status` INT NULL DEFAULT 0 COMMENT '商品状态：0在售，1历史线上锁定，2交易中，3已售出，4已下架',
  `view_count` INT NULL DEFAULT 0 COMMENT '浏览次数，用于后续热度排序或数据统计',
  `favorite_count` INT NULL DEFAULT 0 COMMENT '当前有效收藏次数，由收藏关系增删时同步维护',
  `deleted` INT NULL DEFAULT 0 COMMENT '逻辑删除字段：0未删除，1已删除',
  `create_time` DATETIME NULL COMMENT '商品创建时间',
  `update_time` DATETIME NULL COMMENT '商品最后更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_second_hand_product_school_status` (`school_id`, `status`),
  KEY `idx_second_hand_product_seller` (`seller_id`),
  KEY `idx_second_hand_product_category` (`category_id`)
) ENGINE=InnoDB COMMENT='二手商品表';

-- ============================================================
-- 表：tb_second_hand_favorite
-- 说明：用户收藏二手商品的关系表。
-- 使用场景：
-- - 商品详情收藏和取消收藏。
-- - “我的收藏”按最近收藏时间展示商品。
-- - 商品即使已售出或下架，收藏关系仍保留并展示当前状态。
-- 同一用户只能收藏同一商品一次；商品逻辑删除后列表自动隐藏该商品。
-- ============================================================
CREATE TABLE IF NOT EXISTS `tb_second_hand_favorite` (
  `id` BIGINT AUTO_INCREMENT NOT NULL COMMENT '收藏关系主键id',
  `user_id` BIGINT NOT NULL COMMENT '收藏用户id，关联 tb_user.id',
  `product_id` BIGINT NOT NULL COMMENT '商品id，关联 tb_second_hand_product.id',
  `create_time` DATETIME NOT NULL COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_second_hand_favorite_user_product` (`user_id`, `product_id`),
  KEY `idx_second_hand_favorite_user_time` (`user_id`, `create_time`),
  KEY `idx_second_hand_favorite_product` (`product_id`)
) ENGINE=InnoDB COMMENT='二手商品收藏关系表';

-- ============================================================
-- 表：tb_second_hand_bargain
-- 说明：二手商品议价记录表。
-- 使用场景：
-- - 买家对商品发起报价。
-- - 卖家接受议价后按报价生成二手订单；当前为线下交易订单。
-- - 卖家拒绝后，买家仍可在次数限制内重新议价。
--
-- 议价状态 status：
-- 0 待回复：卖家尚未处理。
-- 1 已接受：卖家接受该报价，并生成订单。
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
-- 当前使用场景：
-- - 买家直接下单，或卖家接受议价后生成 OFFLINE 订单。
-- - 商品立即进入交易中，双方自行协商付款和交付。
-- - 卖家标记交付、买家确认完成后留下交易记录。
-- - 存量 ONLINE 订单保留旧支付、退款和转账字段，仅用于历史订单善后。
--
-- 金额字段：
-- product_amount：商品成交价。
-- pay_amount：兼容金额字段；OFFLINE 模式等于双方约定价，不表示平台已收款。
-- service_fee_rate / service_fee：OFFLINE 模式固定为 0；ONLINE 历史订单保留原快照。
-- seller_income：兼容字段；OFFLINE 模式等于约定价，不表示平台已结算。
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
-- OFFLINE 订单状态：1待交付、2待买家确认完成、3已完成、4已取消、11协商中。
-- ONLINE 历史订单状态：
-- 0 待支付：商品已锁定，30分钟未支付自动关闭并释放商品。
-- 1 已支付待交付：买家已付款，等待卖家自提交付或配送。
-- 2 已交付待确认：卖家已标记交付，等待买家确认收货。
-- 3 已完成：旧业务完成态。
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
  `order_number` VARCHAR(64) NOT NULL COMMENT '二手交易订单号；ONLINE历史订单同时用于支付关联',
  `product_id` BIGINT NOT NULL COMMENT '商品id，关联 tb_second_hand_product.id',
  `bargain_id` BIGINT NULL COMMENT '议价记录id；直接下单时为空，议价成交时关联 tb_second_hand_bargain.id',
  `buyer_id` BIGINT NOT NULL COMMENT '买家用户id，关联 tb_user.id',
  `seller_id` BIGINT NOT NULL COMMENT '卖家用户id，关联 tb_user.id',
  `trade_mode` VARCHAR(16) NOT NULL DEFAULT 'OFFLINE' COMMENT '交易模式：ONLINE历史线上交易，OFFLINE线下自行交易',
  `product_amount` DECIMAL(10,2) NOT NULL COMMENT '商品成交价，直接购买为商品售价，议价成交为卖家接受的报价',
  `pay_amount` DECIMAL(10,2) NOT NULL COMMENT '兼容金额字段；OFFLINE模式表示双方约定价',
  `service_fee_rate` DECIMAL(10,4) NULL COMMENT '服务费率快照；OFFLINE模式固定为0',
  `service_fee` DECIMAL(10,2) NULL COMMENT '平台服务费；OFFLINE模式固定为0',
  `seller_income` DECIMAL(10,2) NULL COMMENT '兼容金额字段；OFFLINE模式等于双方约定价',
  `delivery_mode` INT NULL DEFAULT 0 COMMENT '交付方式：0买家自提，1卖家配送',
  `pickup_address_snapshot` VARCHAR(500) NULL COMMENT '订单自提点快照，来源于商品发布时的自提点',
  `buyer_delivery_address_id` BIGINT NULL COMMENT '买家配送地址簿id，关联 tb_address_book.id，仅配送订单使用',
  `buyer_delivery_address_snapshot` VARCHAR(500) NULL COMMENT '买家配送地址快照，仅配送订单使用',
  `delivery_remark` VARCHAR(255) NULL COMMENT '交付备注，如双方协商的配送地点或自提补充说明',
  `status` INT NULL DEFAULT 0 COMMENT 'OFFLINE：1待交付，2待确认完成，3已完成，4已取消，11协商中；ONLINE保留旧支付状态',
  `pay_time` DATETIME NULL COMMENT '买家支付成功时间，以微信支付回调或查询确认为准',
  `cancel_time` DATETIME NULL COMMENT '订单取消时间',
  `cancel_reason` VARCHAR(255) NULL COMMENT '订单取消原因或退款原因',
  `delivered_time` DATETIME NULL COMMENT '卖家标记已交付时间',
  `confirm_deadline` DATETIME NULL COMMENT '自动确认收货截止时间，默认卖家交付后24小时',
  `finish_time` DATETIME NULL COMMENT '交易完成时间，即确认收货或自动确认时间',
  `transfer_time` DATETIME NULL COMMENT '卖家微信零钱转账成功时间',
  `transfer_out_bill_no` VARCHAR(32) NULL COMMENT '当前微信商家转账商户单号；每次明确失败后重试必须生成新单号',
  `transfer_attempt` INT NOT NULL DEFAULT 0 COMMENT '卖家收款发起次数',
  `transfer_bill_no` VARCHAR(64) NULL COMMENT '微信转账单号',
  `transfer_state` VARCHAR(32) NULL COMMENT '微信转账状态，如WAIT_USER_CONFIRM、SUCCESS、FAIL',
  `transfer_package_info` VARCHAR(1024) NULL COMMENT '卖家拉起微信确认收款页面所需package_info',
  `transfer_fail_reason` VARCHAR(500) NULL COMMENT '卖家微信零钱转账失败或待核对原因，仅供后台处理',
  `deleted` INT NULL DEFAULT 0 COMMENT '逻辑删除字段：0未删除，1已删除',
  `create_time` DATETIME NULL COMMENT '订单创建时间',
  `update_time` DATETIME NULL COMMENT '订单最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_second_hand_order_number` (`order_number`),
  UNIQUE KEY `uk_second_hand_transfer_out_bill_no` (`transfer_out_bill_no`),
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
  `read_time` DATETIME NULL COMMENT '接收人阅读时间；为空表示未读',
  `deleted` INT NULL DEFAULT 0 COMMENT '逻辑删除字段：0未删除，1已删除',
  `create_time` DATETIME NULL COMMENT '留言创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_second_hand_message_product` (`product_id`),
  KEY `idx_second_hand_message_user` (`sender_id`, `receiver_id`),
  KEY `idx_second_hand_message_conversation` (`product_id`, `sender_id`, `receiver_id`, `deleted`, `create_time`),
  KEY `idx_second_hand_message_unread` (`receiver_id`, `read_time`, `deleted`, `create_time`)
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
  'ALTER TABLE tb_second_hand_product ADD COLUMN pickup_only INT NULL DEFAULT 1 COMMENT ''是否仅自提：1仅自提，0买家可选自提或卖家配送'' AFTER pickup_address_snapshot',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_second_hand_order' AND COLUMN_NAME = 'trade_mode') = 0,
  'ALTER TABLE tb_second_hand_order ADD COLUMN trade_mode VARCHAR(16) NOT NULL DEFAULT ''ONLINE'' COMMENT ''交易模式：ONLINE历史线上交易，OFFLINE线下自行交易'' AFTER seller_id',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- 系统配置：当前二手交易模式
-- 新安装和当前生产均使用 OFFLINE；已有订单通过 trade_mode 保留为 ONLINE。
-- ============================================================
INSERT INTO tb_system_config(config_key, config_value, description)
SELECT 'second_hand_trade_mode', 'OFFLINE', '二手交易模式：当前仅提供信息撮合和线下自行交易'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM tb_system_config WHERE config_key = 'second_hand_trade_mode');

-- ============================================================
-- 系统配置：二手交易服务费率（仅供 ONLINE 历史流程兼容）
-- config_key：second_hand_service_fee_rate
-- 默认值：0.03
-- 说明：OFFLINE 模式不读取该值，服务费固定为 0。
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
-- 说明：仅用于 ONLINE 历史订单的待支付锁定。
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
-- 说明：仅用于 ONLINE 历史订单；OFFLINE 订单不会自动确认。
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
