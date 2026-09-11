-- 二手私信会话与微信卖家收款状态迁移
-- 执行前必须备份数据库；脚本可重复执行。

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_second_hand_order' AND COLUMN_NAME = 'transfer_out_bill_no') = 0,
  'ALTER TABLE tb_second_hand_order ADD COLUMN transfer_out_bill_no VARCHAR(32) NULL COMMENT ''当前微信商家转账商户单号'' AFTER transfer_time',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_second_hand_order' AND COLUMN_NAME = 'transfer_attempt') = 0,
  'ALTER TABLE tb_second_hand_order ADD COLUMN transfer_attempt INT NOT NULL DEFAULT 0 COMMENT ''卖家收款发起次数'' AFTER transfer_out_bill_no',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_second_hand_order' AND COLUMN_NAME = 'transfer_bill_no') = 0,
  'ALTER TABLE tb_second_hand_order ADD COLUMN transfer_bill_no VARCHAR(64) NULL COMMENT ''微信转账单号'' AFTER transfer_attempt',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_second_hand_order' AND COLUMN_NAME = 'transfer_state') = 0,
  'ALTER TABLE tb_second_hand_order ADD COLUMN transfer_state VARCHAR(32) NULL COMMENT ''微信转账状态'' AFTER transfer_bill_no',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_second_hand_order' AND COLUMN_NAME = 'transfer_package_info') = 0,
  'ALTER TABLE tb_second_hand_order ADD COLUMN transfer_package_info VARCHAR(1024) NULL COMMENT ''微信确认收款package_info'' AFTER transfer_state',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE tb_second_hand_order
  MODIFY COLUMN transfer_fail_reason VARCHAR(500) NULL COMMENT '卖家收款失败或待核对原因，仅供后台处理';

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_second_hand_order' AND INDEX_NAME = 'uk_second_hand_transfer_out_bill_no') = 0,
  'ALTER TABLE tb_second_hand_order ADD UNIQUE KEY uk_second_hand_transfer_out_bill_no (transfer_out_bill_no)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 兼容旧版已经进入收款流程的订单；旧实现直接使用订单号作为商户转账单号。
UPDATE tb_second_hand_order
SET transfer_out_bill_no = order_number,
    transfer_attempt = CASE WHEN transfer_attempt = 0 THEN 1 ELSE transfer_attempt END
WHERE status IN (8, 10)
  AND transfer_out_bill_no IS NULL;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_second_hand_message' AND COLUMN_NAME = 'read_time') = 0,
  'ALTER TABLE tb_second_hand_message ADD COLUMN read_time DATETIME NULL COMMENT ''接收人阅读时间；为空表示未读'' AFTER content',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_second_hand_message' AND INDEX_NAME = 'idx_second_hand_message_conversation') = 0,
  'ALTER TABLE tb_second_hand_message ADD KEY idx_second_hand_message_conversation (product_id, sender_id, receiver_id, deleted, create_time)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_second_hand_message' AND INDEX_NAME = 'idx_second_hand_message_unread') = 0,
  'ALTER TABLE tb_second_hand_message ADD KEY idx_second_hand_message_unread (receiver_id, read_time, deleted, create_time)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO tb_system_config(config_key, config_value, description)
SELECT 'second_hand_service_fee_rate', '0.03', '二手交易卖家承担服务费率，0.03表示3%'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM tb_system_config WHERE config_key = 'second_hand_service_fee_rate'
);
