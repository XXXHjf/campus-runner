-- ============================================================
-- Campus Runner 统一媒体资源第二阶段迁移
--
-- 用途：跑腿分类、轮播图、二手商品、跑腿订单、交付凭证、头像和学生证
-- 统一使用 tb_media_asset.bound_type + bound_id 关联业务记录。
--
-- 执行要求：
-- 1. 先备份数据库。
-- 2. 必须先执行 media-asset-migration.sql。
-- 3. 在发布依赖 sort_order 的 API 前执行。
-- 4. 本脚本不修改或删除任何历史 URL 和 OSS 文件，可重复执行。
-- ============================================================

SET @has_media_sort_order = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'tb_media_asset'
    AND COLUMN_NAME = 'sort_order'
);

SET @add_media_sort_order_sql = IF(
  @has_media_sort_order = 0,
  'ALTER TABLE tb_media_asset ADD COLUMN sort_order INT NOT NULL DEFAULT 0 COMMENT ''同一业务多图顺序'' AFTER bound_id',
  'SELECT 1'
);

PREPARE add_media_sort_order_stmt FROM @add_media_sort_order_sql;
EXECUTE add_media_sort_order_stmt;
DEALLOCATE PREPARE add_media_sort_order_stmt;

-- 验证：
-- SHOW COLUMNS FROM tb_media_asset LIKE 'sort_order';
