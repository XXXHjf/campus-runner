-- Campus Runner 旧媒体字段、用户收款码和二手兼容字段清理
--
-- 不可逆操作。执行前必须：
-- 1. 停止所有旧版 API 进程，禁止与旧版滚动共存。
-- 2. 完成全库备份并验证备份可恢复。
-- 3. 先运行下方绑定完整性检查；任一未删除记录的旧图片缺少新媒体绑定时脚本会终止。
-- 4. 使用已经移除旧字段读写的新版 JAR 启动。

DELIMITER $$

DROP PROCEDURE IF EXISTS cr_assert_legacy_media_migrated$$
CREATE PROCEDURE cr_assert_legacy_media_migrated(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_bound_type VARCHAR(64)
)
BEGIN
    DECLARE v_column_exists INT DEFAULT 0;
    DECLARE v_deleted_column_exists INT DEFAULT 0;
    DECLARE v_error_message VARCHAR(128);
    SELECT COUNT(*) INTO v_column_exists
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
      AND COLUMN_NAME = p_column_name;

    SELECT COUNT(*) INTO v_deleted_column_exists
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
      AND COLUMN_NAME = 'deleted';

    IF v_column_exists > 0 THEN
        SET @cr_missing = 0;
        SET @cr_sql = CONCAT(
            'SELECT COUNT(*) INTO @cr_missing FROM `', p_table_name, '` legacy ',
            'WHERE legacy.`', p_column_name, '` IS NOT NULL ',
            'AND TRIM(legacy.`', p_column_name, '`) <> '''' ',
            IF(v_deleted_column_exists > 0,
               'AND COALESCE(legacy.deleted, 0) = 0 ',
               ''),
            'AND NOT EXISTS (SELECT 1 FROM tb_media_asset media ',
            'WHERE media.bound_type = ''', p_bound_type, ''' ',
            'AND media.bound_id = legacy.id AND media.status = ''BOUND'')'
        );
        PREPARE cr_stmt FROM @cr_sql;
        EXECUTE cr_stmt;
        DEALLOCATE PREPARE cr_stmt;
        IF @cr_missing > 0 THEN
            SET v_error_message = CONCAT(
                p_table_name, '.', p_column_name,
                ' 缺少有效媒体绑定，数量: ', @cr_missing
            );
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = v_error_message;
        END IF;
    END IF;
END$$

DROP PROCEDURE IF EXISTS cr_drop_column_if_exists$$
CREATE PROCEDURE cr_drop_column_if_exists(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64)
)
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @cr_sql = CONCAT('ALTER TABLE `', p_table_name, '` DROP COLUMN `', p_column_name, '`');
        PREPARE cr_stmt FROM @cr_sql;
        EXECUTE cr_stmt;
        DEALLOCATE PREPARE cr_stmt;
    END IF;
END$$

DROP PROCEDURE IF EXISTS cr_drop_index_if_exists$$
CREATE PROCEDURE cr_drop_index_if_exists(
    IN p_table_name VARCHAR(64),
    IN p_index_name VARCHAR(64)
)
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND INDEX_NAME = p_index_name
    ) THEN
        SET @cr_sql = CONCAT('ALTER TABLE `', p_table_name, '` DROP INDEX `', p_index_name, '`');
        PREPARE cr_stmt FROM @cr_sql;
        EXECUTE cr_stmt;
        DEALLOCATE PREPARE cr_stmt;
    END IF;
END$$

DROP PROCEDURE IF EXISTS cr_assert_second_hand_pickup_migrated$$
CREATE PROCEDURE cr_assert_second_hand_pickup_migrated()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'tb_second_hand_product'
          AND COLUMN_NAME = 'pickup_location'
    ) THEN
        SELECT COUNT(*) INTO @cr_missing
        FROM tb_second_hand_product
        WHERE pickup_location IS NOT NULL
          AND TRIM(pickup_location) <> ''
          AND (pickup_address_snapshot IS NULL OR TRIM(pickup_address_snapshot) = '');
        IF @cr_missing > 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '二手商品自提点快照尚未回填完整';
        END IF;
    END IF;
    SELECT COUNT(*) INTO @cr_missing
    FROM tb_second_hand_product
    WHERE pickup_only IS NULL;
    IF @cr_missing > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '二手商品交付方式尚未回填完整';
    END IF;
END$$

CALL cr_assert_legacy_media_migrated('tb_category', 'image', 'ORDER_CATEGORY')$$
CALL cr_assert_legacy_media_migrated('tb_banner', 'img_url', 'BANNER')$$
CALL cr_assert_legacy_media_migrated('tb_orders', 'image', 'ORDER')$$
CALL cr_assert_legacy_media_migrated('tb_take_orders', 'image', 'TAKE_ORDER')$$
CALL cr_assert_legacy_media_migrated('tb_user', 'head_img', 'USER_AVATAR')$$
CALL cr_assert_legacy_media_migrated('tb_user', 'student_id_card', 'USER_STUDENT_CARD')$$
CALL cr_assert_legacy_media_migrated('tb_second_hand_category', 'image', 'SECOND_HAND_CATEGORY')$$
CALL cr_assert_legacy_media_migrated('tb_second_hand_category', 'image_asset_id', 'SECOND_HAND_CATEGORY')$$
CALL cr_assert_legacy_media_migrated('tb_second_hand_product', 'images', 'SECOND_HAND_PRODUCT')$$

-- 旧二手配送字段必须已经完整回填到当前字段。
CALL cr_assert_second_hand_pickup_migrated()$$

-- 私下转账收款码已经停止使用；其媒体对象进入七天延迟删除，不直接删除 OSS 文件。
UPDATE tb_media_asset
SET status = 'PENDING_DELETE',
    bound_type = NULL,
    bound_id = NULL,
    bound_at = NULL,
    delete_after = DATE_ADD(NOW(), INTERVAL 7 DAY),
    update_time = NOW()
WHERE purpose = 'PAYMENT_QR'
   OR bound_type IN ('USER_ALIPAY_PAYMENT', 'USER_WECHAT_PAYMENT')$$

CALL cr_drop_index_if_exists('tb_second_hand_category', 'idx_second_hand_category_image_asset')$$

CALL cr_drop_column_if_exists('tb_category', 'image')$$
CALL cr_drop_column_if_exists('tb_banner', 'img_url')$$
CALL cr_drop_column_if_exists('tb_orders', 'image')$$
CALL cr_drop_column_if_exists('tb_take_orders', 'image')$$
CALL cr_drop_column_if_exists('tb_user', 'head_img')$$
CALL cr_drop_column_if_exists('tb_user', 'student_id_card')$$
CALL cr_drop_column_if_exists('tb_user', 'alipay_payment_code')$$
CALL cr_drop_column_if_exists('tb_user', 'wechat_payment_code')$$
CALL cr_drop_column_if_exists('tb_second_hand_category', 'image')$$
CALL cr_drop_column_if_exists('tb_second_hand_category', 'image_asset_id')$$
CALL cr_drop_column_if_exists('tb_second_hand_product', 'images')$$
CALL cr_drop_column_if_exists('tb_second_hand_product', 'pickup_location')$$
CALL cr_drop_column_if_exists('tb_second_hand_product', 'support_delivery')$$

DROP PROCEDURE cr_assert_legacy_media_migrated$$
DROP PROCEDURE cr_drop_column_if_exists$$
DROP PROCEDURE cr_drop_index_if_exists$$
DROP PROCEDURE cr_assert_second_hand_pickup_migrated$$

DELIMITER ;

-- 验证：以下查询应返回 0。
SELECT COUNT(*) AS remaining_legacy_columns
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND (
    (TABLE_NAME = 'tb_category' AND COLUMN_NAME = 'image')
    OR (TABLE_NAME = 'tb_banner' AND COLUMN_NAME = 'img_url')
    OR (TABLE_NAME = 'tb_orders' AND COLUMN_NAME = 'image')
    OR (TABLE_NAME = 'tb_take_orders' AND COLUMN_NAME = 'image')
    OR (TABLE_NAME = 'tb_user' AND COLUMN_NAME IN
        ('head_img', 'student_id_card', 'alipay_payment_code', 'wechat_payment_code'))
    OR (TABLE_NAME = 'tb_second_hand_category' AND COLUMN_NAME IN ('image', 'image_asset_id'))
    OR (TABLE_NAME = 'tb_second_hand_product' AND COLUMN_NAME IN
        ('images', 'pickup_location', 'support_delivery'))
  );
