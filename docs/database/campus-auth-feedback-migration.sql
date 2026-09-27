-- Campus authentication feedback. Back up the database before execution.
-- Run before the new API; coordinate admin upgrade (review version is required).
SET @current_schema = DATABASE();

SELECT COUNT(*) INTO @auth_column_exists FROM information_schema.columns
WHERE table_schema = @current_schema AND table_name = 'tb_user' AND column_name = 'student_id_card_reject_reason';
SET @auth_migration_sql = IF(@auth_column_exists = 0,
  'ALTER TABLE tb_user ADD COLUMN student_id_card_reject_reason VARCHAR(100) NULL',
  'SELECT ''student_id_card_reject_reason already exists'' AS message');
PREPARE auth_migration_stmt FROM @auth_migration_sql;
EXECUTE auth_migration_stmt;
DEALLOCATE PREPARE auth_migration_stmt;

SELECT COUNT(*) INTO @auth_column_exists FROM information_schema.columns
WHERE table_schema = @current_schema AND table_name = 'tb_user' AND column_name = 'auth_review_version';
SET @auth_migration_sql = IF(@auth_column_exists = 0,
  'ALTER TABLE tb_user ADD COLUMN auth_review_version BIGINT NOT NULL DEFAULT 0',
  'SELECT ''auth_review_version already exists'' AS message');
PREPARE auth_migration_stmt FROM @auth_migration_sql;
EXECUTE auth_migration_stmt;
DEALLOCATE PREPARE auth_migration_stmt;

SELECT column_name, column_type, is_nullable, column_default
FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_user'
AND column_name IN ('student_id_card_reject_reason', 'auth_review_version');
