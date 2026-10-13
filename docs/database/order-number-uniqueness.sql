-- Run only after the backup and read-only audit in order-number-uniqueness.md.
-- No existing number is rewritten, including cancelled or hidden orders.
DELIMITER $$
DROP PROCEDURE IF EXISTS cr_migrate_order_number_uniqueness$$
CREATE PROCEDURE cr_migrate_order_number_uniqueness(IN order_table VARCHAR(64), IN new_index VARCHAR(64))
BEGIN
    DECLARE full_unique_count INT DEFAULT 0;
    DECLARE number_capacity BIGINT DEFAULT 0;
    DECLARE name_in_use INT DEFAULT 0;
    IF NOT ((order_table = 'tb_orders' AND new_index = 'uk_runner_order_number')
        OR (order_table = 'tb_second_hand_order' AND new_index = 'uk_second_hand_order_number')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unexpected order table or index';
    END IF;
    SELECT COALESCE(MAX(CHARACTER_MAXIMUM_LENGTH), 0) INTO number_capacity
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = order_table AND COLUMN_NAME = 'order_number'
      AND DATA_TYPE IN ('char', 'varchar');
    IF number_capacity < 32 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Order number column missing or shorter than 32 characters';
    END IF;
    SET @cr_order_number_sql = CONCAT('SELECT COUNT(*) INTO @cr_invalid_order_numbers FROM `', order_table,
        '` WHERE order_number IS NULL OR CHAR_LENGTH(TRIM(order_number)) = 0');
    PREPARE cr_order_number_stmt FROM @cr_order_number_sql;
    EXECUTE cr_order_number_stmt;
    DEALLOCATE PREPARE cr_order_number_stmt;
    IF @cr_invalid_order_numbers > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing historical order numbers require manual review';
    END IF;
    SET @cr_order_number_sql = CONCAT('SELECT COUNT(*) INTO @cr_duplicate_order_numbers FROM (SELECT order_number FROM `',
        order_table, '` GROUP BY order_number HAVING COUNT(*) > 1) AS duplicates_found');
    PREPARE cr_order_number_stmt FROM @cr_order_number_sql;
    EXECUTE cr_order_number_stmt;
    DEALLOCATE PREPARE cr_order_number_stmt;
    IF @cr_duplicate_order_numbers > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate historical order numbers require manual review';
    END IF;
    SELECT COUNT(*) INTO full_unique_count FROM (
        SELECT INDEX_NAME FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = order_table AND NON_UNIQUE = 0
        GROUP BY INDEX_NAME
        HAVING COUNT(*) = 1 AND MAX(COLUMN_NAME = 'order_number') = 1 AND MAX(SUB_PART IS NOT NULL) = 0
    ) AS existing_full_unique_indexes;
    IF full_unique_count = 0 THEN
        SELECT COUNT(*) INTO name_in_use FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = order_table AND INDEX_NAME = new_index;
        IF name_in_use > 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Expected unique index name is already used by another index';
        END IF;
        SET @cr_order_number_sql = CONCAT('ALTER TABLE `', order_table, '` ADD UNIQUE INDEX `', new_index, '` (`order_number`)');
        PREPARE cr_order_number_stmt FROM @cr_order_number_sql;
        EXECUTE cr_order_number_stmt;
        DEALLOCATE PREPARE cr_order_number_stmt;
    END IF;
    SELECT order_table AS checked_table, (full_unique_count = 0) AS index_added;
END$$
CALL cr_migrate_order_number_uniqueness('tb_orders', 'uk_runner_order_number')$$
CALL cr_migrate_order_number_uniqueness('tb_second_hand_order', 'uk_second_hand_order_number')$$
DROP PROCEDURE cr_migrate_order_number_uniqueness$$
DELIMITER ;
