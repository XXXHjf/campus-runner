-- Back up tb_orders before running. Run once before deploying the dependent API.
-- Keep deleted unchanged: funds reconciliation, runner history and evidence remain accessible.
ALTER TABLE tb_orders
    ADD COLUMN publisher_hidden TINYINT NOT NULL DEFAULT 0 COMMENT '发单人列表隐藏标记';
