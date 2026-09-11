-- 二手交易切换为线下信息撮合模式
-- 执行前必须备份数据库；脚本可重复执行。

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'tb_second_hand_order'
     AND COLUMN_NAME = 'trade_mode') = 0,
  'ALTER TABLE tb_second_hand_order ADD COLUMN trade_mode VARCHAR(16) NOT NULL DEFAULT ''ONLINE'' COMMENT ''交易模式：ONLINE历史线上交易，OFFLINE线下自行交易'' AFTER seller_id',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 存量订单保留 ONLINE；新版 API 会为新订单显式写入 OFFLINE。
INSERT INTO tb_system_config(config_key, config_value, description)
SELECT 'second_hand_trade_mode', 'OFFLINE', '二手交易模式：当前仅提供信息撮合和线下自行交易'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM tb_system_config WHERE config_key = 'second_hand_trade_mode'
);

UPDATE tb_system_config
SET config_value = 'OFFLINE',
    description = '二手交易模式：当前仅提供信息撮合和线下自行交易'
WHERE config_key = 'second_hand_trade_mode';
