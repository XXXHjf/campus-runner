-- 地址簿不再区分取件地址和收件地址。
-- 执行前必须备份数据库，并确认新版 API 已准备好上线。

SET @current_schema = DATABASE();

SELECT COUNT(*)
INTO @address_type_exists
FROM information_schema.columns
WHERE table_schema = @current_schema
  AND table_name = 'tb_address_book'
  AND column_name = 'type';

SET @drop_address_type_sql = IF(
  @address_type_exists > 0,
  'ALTER TABLE tb_address_book DROP COLUMN type',
  'SELECT ''tb_address_book.type already removed'' AS message'
);

PREPARE drop_address_type_stmt FROM @drop_address_type_sql;
EXECUTE drop_address_type_stmt;
DEALLOCATE PREPARE drop_address_type_stmt;

SELECT COUNT(*) AS remaining_type_columns
FROM information_schema.columns
WHERE table_schema = @current_schema
  AND table_name = 'tb_address_book'
  AND column_name = 'type';
