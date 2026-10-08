-- Run only after stopping all API writers and taking a verified database backup.
-- Existing addresses are migration-time evidence, not reconstructed submission-time evidence.
CREATE TABLE IF NOT EXISTS tb_order_address_snapshot (
    order_id BIGINT NOT NULL,
    role VARCHAR(10) NOT NULL,
    address_id BIGINT NULL,
    school_id BIGINT NULL,
    compus_id BIGINT NULL,
    build_category_id BIGINT NULL,
    building_id BIGINT NULL,
    address TEXT NOT NULL,
    public_address TEXT NOT NULL,
    source VARCHAR(20) NOT NULL,
    captured_at DATETIME NOT NULL,
    PRIMARY KEY (order_id, role),
    KEY idx_snapshot_location (role, school_id, compus_id, build_category_id, building_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Include hidden/deleted orders and soft-deleted addresses. Never overwrite an existing snapshot.
INSERT INTO tb_order_address_snapshot
    (order_id, role, address_id, school_id, compus_id, build_category_id, building_id,
     address, public_address, source, captured_at)
SELECT o.id, roles.role,
       CASE WHEN roles.role = 'PICKUP' THEN o.pick_up_address ELSE o.recive_address END,
       a.school_id, a.compus_id, a.build_category_id, a.building_id,
       CASE WHEN a.id IS NULL THEN '历史地址信息缺失'
            ELSE CONCAT(CONCAT_WS(' ', s.school_name, c.compus_name, t.name, b.building_name, a.details),
                CASE WHEN s.id IS NULL OR c.id IS NULL OR t.id IS NULL OR b.id IS NULL
                          OR a.details IS NULL THEN '（历史地址信息不完整）' ELSE '' END) END,
       CASE WHEN a.id IS NULL THEN '历史地址信息缺失'
            ELSE CONCAT_WS(' ', s.school_name, c.compus_name, t.name, b.building_name) END,
       CASE WHEN a.id IS NULL THEN 'LEGACY_MISSING'
            WHEN s.id IS NULL OR c.id IS NULL OR t.id IS NULL OR b.id IS NULL OR a.details IS NULL
                THEN 'LEGACY_PARTIAL' ELSE 'LEGACY_CURRENT' END,
       CURRENT_TIMESTAMP
FROM tb_orders o
CROSS JOIN (SELECT 'PICKUP' AS role UNION ALL SELECT 'RECEIVE') roles
LEFT JOIN tb_address_book a ON a.id =
    CASE WHEN roles.role = 'PICKUP' THEN o.pick_up_address ELSE o.recive_address END
LEFT JOIN tb_school s ON s.id = a.school_id
LEFT JOIN tb_compus c ON c.id = a.compus_id
LEFT JOIN tb_build_category t ON t.id = a.build_category_id
LEFT JOIN tb_building b ON b.id = a.building_id
WHERE NOT EXISTS (
    SELECT 1 FROM tb_order_address_snapshot existing
    WHERE existing.order_id = o.id AND existing.role = roles.role
);

-- Must be zero before starting the new API.
SELECT COUNT(*) AS orders_without_two_snapshots
FROM tb_orders o
WHERE (SELECT COUNT(*) FROM tb_order_address_snapshot a
       WHERE a.order_id = o.id AND a.role IN ('PICKUP', 'RECEIVE')) != 2;
SELECT source, COUNT(*) AS snapshot_count FROM tb_order_address_snapshot GROUP BY source;
