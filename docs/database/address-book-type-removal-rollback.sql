-- 仅恢复旧版 API 需要的表结构；历史地址类型无法从现有数据中还原。

SET @current_schema = DATABASE();

SELECT COUNT(*)
INTO @address_type_exists
FROM information_schema.columns
WHERE table_schema = @current_schema
  AND table_name = 'tb_address_book'
  AND column_name = 'type';

SET @add_address_type_sql = IF(
  @address_type_exists = 0,
  'ALTER TABLE tb_address_book ADD COLUMN type INT NULL COMMENT ''已弃用：旧版地址种类'' AFTER is_default',
  'SELECT ''tb_address_book.type already exists'' AS message'
);

PREPARE add_address_type_stmt FROM @add_address_type_sql;
EXECUTE add_address_type_stmt;
DEALLOCATE PREPARE add_address_type_stmt;

SELECT COUNT(*) AS restored_type_columns
FROM information_schema.columns
WHERE table_schema = @current_schema
  AND table_name = 'tb_address_book'
  AND column_name = 'type';
