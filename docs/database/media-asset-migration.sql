-- ============================================================
-- Campus Runner 统一媒体资源表
--
-- 执行要求：
-- 1. 先备份数据库。
-- 2. 在发布统一媒体资源 API 前执行。
-- 3. 可重复执行。
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

-- 验证：
-- SHOW TABLES LIKE 'tb_media_asset';
