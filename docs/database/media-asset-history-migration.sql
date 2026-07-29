-- ============================================================
-- Campus Runner 历史图片迁移台账
--
-- 用途：
-- 1. 按来源表、字段、业务记录、图片序号和规范化 URL 哈希记录迁移结果。
-- 2. 支持迁移任务失败后重复执行，避免重复创建媒体资源。
-- 3. 为旧字段兼容读取提供“该 URL 已迁移”的判定依据。
--
-- 执行要求：
-- 1. 必须先备份数据库。
-- 2. 必须先执行 media-asset-migration.sql 和
--    media-asset-phase2-migration.sql。
-- 3. dry-run 本身不要求本表；正式迁移和 verify 前必须执行本脚本。
-- 4. 本脚本不读取、修改或清空任何旧图片字段，也不操作 OSS。
-- 5. 台账至少保留到旧字段停止读写后的观察期结束。
-- ============================================================

CREATE TABLE IF NOT EXISTS `tb_media_migration_ledger` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '台账主键',
  `source_table` VARCHAR(64) NOT NULL COMMENT '旧数据来源表',
  `source_column` VARCHAR(64) NOT NULL COMMENT '旧图片来源字段',
  `business_id` BIGINT NOT NULL COMMENT '来源业务记录主键',
  `image_index` INT NOT NULL DEFAULT 0 COMMENT '多图中的原始序号',
  `legacy_url_hash` CHAR(64) NOT NULL COMMENT '规范化旧 URL 的 SHA-256',
  `source_kind` VARCHAR(24) NOT NULL COMMENT 'PROJECT_OSS/EXTERNAL_HTTPS/INVALID',
  `source_object_key` VARCHAR(191) NULL COMMENT '项目 OSS 来源对象 key',
  `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING'
      COMMENT 'PENDING/MIGRATED/SUPERSEDED/FAILED/MANUAL_REQUIRED',
  `media_asset_id` BIGINT NULL COMMENT '迁移成功或替代使用的媒体资源 id',
  `attempt_count` INT NOT NULL DEFAULT 0 COMMENT '执行尝试次数',
  `failure_code` VARCHAR(64) NULL COMMENT '稳定失败原因码',
  `failure_reason` VARCHAR(500) NULL COMMENT '不含旧 URL 和秘密的失败说明',
  `last_checked_at` DATETIME NULL COMMENT '最近处理或核验时间',
  `migrated_at` DATETIME NULL COMMENT '迁移完成时间',
  `create_time` DATETIME NOT NULL COMMENT '创建时间',
  `update_time` DATETIME NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_media_migration_source` (
    `source_table`,
    `source_column`,
    `business_id`,
    `image_index`,
    `legacy_url_hash`
  ),
  KEY `idx_media_migration_status` (`status`, `id`),
  KEY `idx_media_migration_asset` (`media_asset_id`),
  KEY `idx_media_migration_business` (
    `source_table`,
    `source_column`,
    `business_id`
  ),
  KEY `idx_media_migration_object` (`source_object_key`)
) ENGINE=InnoDB COMMENT='历史图片迁移幂等台账';

-- 验证：
-- SHOW TABLES LIKE 'tb_media_migration_ledger';
-- SHOW INDEX FROM tb_media_migration_ledger;
