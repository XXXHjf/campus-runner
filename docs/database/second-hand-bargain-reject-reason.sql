-- 在已确认的业务库执行；先按 server-operations.md 暂停交易写入并备份数据库。
-- 先执行字段迁移与失效修复，再启动依赖字段的新 API。脚本不向历史记录虚构拒绝原因。
-- 可重复执行；不覆盖已有列、不删除历史记录。
SELECT DATABASE() AS target_database;

SET @cr_bargain_reason_ddl = IF(
  EXISTS(SELECT 1 FROM information_schema.columns
         WHERE table_schema = DATABASE() AND table_name = 'tb_second_hand_bargain'
           AND column_name = 'reject_reason_code'),
  'SELECT ''reject_reason_code already exists'' AS migration_status',
  'ALTER TABLE tb_second_hand_bargain ADD COLUMN reject_reason_code VARCHAR(32) NULL COMMENT ''卖家拒绝原因''');
PREPARE cr_bargain_reason_stmt FROM @cr_bargain_reason_ddl;
EXECUTE cr_bargain_reason_stmt;
DEALLOCATE PREPARE cr_bargain_reason_stmt;

SET @cr_bargain_note_ddl = IF(
  EXISTS(SELECT 1 FROM information_schema.columns
         WHERE table_schema = DATABASE() AND table_name = 'tb_second_hand_bargain'
           AND column_name = 'reject_note'),
  'SELECT ''reject_note already exists'' AS migration_status',
  'ALTER TABLE tb_second_hand_bargain ADD COLUMN reject_note VARCHAR(40) CHARACTER SET utf8mb4 NULL COMMENT ''自定义拒绝原因，最多40字''');
PREPARE cr_bargain_note_stmt FROM @cr_bargain_note_ddl;
EXECUTE cr_bargain_note_stmt;
DEALLOCATE PREPARE cr_bargain_note_stmt;

SELECT column_name, column_type, is_nullable, character_set_name
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'tb_second_hand_bargain'
  AND column_name IN ('reject_reason_code', 'reject_note');
