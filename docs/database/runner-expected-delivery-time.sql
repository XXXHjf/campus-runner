-- 先备份 tb_orders，再执行此增量迁移，最后部署新版 API。
-- 旧订单保持 NULL：无法可靠恢复用户最初选中的时间。
ALTER TABLE tb_orders
    ADD COLUMN expected_delivery_time DATETIME NULL COMMENT '用户选定的预期送达时间' AFTER delivery_time;
