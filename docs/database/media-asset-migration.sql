-- ============================================================
-- Campus Runner 统一媒体资源表与历史二手分类图片兼容迁移
--
-- 执行要求：
-- 1. 先备份数据库。
-- 2. 先确保 tb_second_hand_category 已存在；全新环境先执行 second-hand-schema.sql。
-- 3. 在发布依赖 image_asset_id 的 API 前执行。
-- 4. 脚本不删除旧 image 字段或旧 OSS 文件，可重复执行。
-- ============================================================

CREATE TABLE IF NOT EXISTS `tb_media_asset` (
  `id` BIGINT AUTO_INCREMENT NOT NULL COMMENT '媒体资源主键',
  `object_key` VARCHAR(500) NOT NULL COMMENT 'OSS 对象键，不保存签名 URL',
  `purpose` VARCHAR(64) NOT NULL COMMENT '业务用途',
  `visibility` VARCHAR(16) NOT NULL COMMENT 'PUBLIC 或 PRIVATE',
  `owner_type` VARCHAR(16) NOT NULL COMMENT 'USER 或 ADMIN',
  `owner_id` BIGINT NOT NULL COMMENT '上传者 id',
  `status` VARCHAR(24) NOT NULL COMMENT 'UPLOADING/TEMP/BOUND/PENDING_DELETE/DELETED/FAILED',
  `mime_type` VARCHAR(64) NOT NULL COMMENT '服务端识别的 MIME 类型',
  `file_size` BIGINT NOT NULL COMMENT '文件字节数',
  `width` INT NOT NULL COMMENT '图片宽度',
  `height` INT NOT NULL COMMENT '图片高度',
  `bound_type` VARCHAR(64) NULL COMMENT '绑定的业务类型',
  `bound_id` BIGINT NULL COMMENT '绑定的业务主键',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '同一业务多图顺序',
  `bound_at` DATETIME NULL COMMENT '绑定时间',
  `expires_at` DATETIME NULL COMMENT '临时资源过期时间',
  `delete_after` DATETIME NULL COMMENT '允许物理删除的时间',
  `delete_retry_count` INT NOT NULL DEFAULT 0 COMMENT 'OSS 删除重试次数',
  `last_error` VARCHAR(500) NULL COMMENT '最近一次资源处理错误',
  `create_time` DATETIME NOT NULL COMMENT '创建时间',
  `update_time` DATETIME NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_media_asset_object_key` (`object_key`),
  KEY `idx_media_asset_owner_status` (`owner_type`, `owner_id`, `status`),
  KEY `idx_media_asset_cleanup` (`status`, `expires_at`, `delete_after`),
  KEY `idx_media_asset_binding` (`bound_type`, `bound_id`)
) ENGINE=InnoDB COMMENT='统一媒体资源及生命周期';

SET @has_image_asset_id = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'tb_second_hand_category'
    AND COLUMN_NAME = 'image_asset_id'
);
SET @add_image_asset_id_sql = IF(
  @has_image_asset_id = 0,
  'ALTER TABLE tb_second_hand_category ADD COLUMN image_asset_id BIGINT NULL COMMENT ''新媒体资源 id；为空时兼容读取旧 image'' AFTER image',
  'SELECT 1'
);
PREPARE add_image_asset_id_stmt FROM @add_image_asset_id_sql;
EXECUTE add_image_asset_id_stmt;
DEALLOCATE PREPARE add_image_asset_id_stmt;

SET @has_category_image_asset_index = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'tb_second_hand_category'
    AND INDEX_NAME = 'idx_second_hand_category_image_asset'
);
SET @add_category_image_asset_index_sql = IF(
  @has_category_image_asset_index = 0,
  'ALTER TABLE tb_second_hand_category ADD INDEX idx_second_hand_category_image_asset (image_asset_id)',
  'SELECT 1'
);
PREPARE add_category_image_asset_index_stmt FROM @add_category_image_asset_index_sql;
EXECUTE add_category_image_asset_index_stmt;
DEALLOCATE PREPARE add_category_image_asset_index_stmt;

-- 验证：
-- SHOW TABLES LIKE 'tb_media_asset';
-- SHOW COLUMNS FROM tb_second_hand_category LIKE 'image_asset_id';
