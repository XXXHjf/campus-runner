# 跑腿订单服务费覆盖：只读排查

## 原因与修复范围

`OrderMapper.xml` 的通用 `update` 曾在 `serviceFeeRate != null` 时把费率写入
`service_fee`。传入完整订单的接单、配送、确认收货、取消及退款准备流程可能触发此错误。
已修正为仅在 `serviceFee != null` 时绑定 `serviceFee`；上述状态调用改用局部对象，
不重写 `price`、`product_amount`、`service_fee_rate`、`service_fee`、`pay_amount`。
退款结果使用的 `updateStatusByOrderNumber` 原本就只写状态；未支付超时取消原本就使用局部对象。
二手业务使用独立的 `SecondHandOrderMapper`，不属于本次错误映射范围。

修复只防止后续覆盖，不恢复已经损坏的快照。生产受影响数量、实际部署版本和结构尚未核实。
本次无结构迁移、无历史数据更新。验证使用动态 SQL 参数映射测试和取消、退款单元测试，
不等同于生产数据库或微信实付验证。若代码回滚到错误版本，该覆盖风险也会恢复。

## 执行方式

使用只有 SELECT/元数据查看权限的账号，优先在只读副本或备份库执行。密码通过交互输入，
不放在命令参数、文档或输出里。以下都是只读查询，先确认目标库和实际结构：

```sql
SELECT DATABASE(), VERSION();
SHOW COLUMNS FROM tb_orders;
SHOW COLUMNS FROM tb_payment_log;
SHOW COLUMNS FROM tb_refund_info;
```

确认字段存在后执行后续查询。若生产没有 `product_amount`，先确认所查订单均属于普通跑腿，
再将查询中的 `COALESCE(product_amount, 0)` 替换为 `0`，不得套用到代买订单。
按已确认的错误版本部署区间和订单 ID 分批查询；以下示例的 `id > 0` 可改为上一批末尾 ID。
不要只筛取消/退款状态：其他状态也可能触发覆盖，逻辑删除的订单同样应核对。

## 订单金额一致性

当前计费关系为：用户实付 = 跑腿费 + 商品金额 + 服务费。
服务费存在最低收费配置，因此不能仅用跑腿费乘费率推算历史服务费，也不能用当前配置重算。

```sql
SELECT id, order_number, status, deleted, create_time, cancel_time,
       price, product_amount, service_fee_rate, service_fee, pay_amount,
       ROUND(pay_amount - price - COALESCE(product_amount, 0), 2) AS implied_fee,
       ROUND(service_fee_rate, 2) AS rate_written_as_money,
       CASE WHEN service_fee = ROUND(service_fee_rate, 2)
            THEN 1 ELSE 0 END AS matches_old_bug
FROM tb_orders
WHERE id > 0
  AND (price IS NULL OR pay_amount IS NULL OR service_fee IS NULL
       OR ROUND(pay_amount - price - COALESCE(product_amount, 0) - service_fee, 2) <> 0)
ORDER BY id
LIMIT 200;
```

`matches_old_bug=1` 且金额不平衡是疑似覆盖特征，不能单凭费率与金额相等认定受损。
缺少金额字段、旧计费规则或其他金额变化需单独查证。金额平衡也不能证明快照一定正确。
发现疑似订单后，逐单与支付、退款记录及商户平台对照：

```sql
SELECT o.id, o.order_number, o.status, o.pay_amount, o.service_fee,
       p.id AS payment_log_id, p.trade_state, p.trade_type, p.transaction_id,
       p.total AS paid_cents, p.service_fee AS payment_fee_cents,
       p.total / 100.00 AS paid_yuan,
       p.service_fee / 100.00 AS payment_fee_yuan
FROM tb_orders o
LEFT JOIN tb_payment_log p ON p.order_number = o.order_number AND p.deleted = 0
WHERE o.id = 123; -- 替换为待核对订单 ID

SELECT order_number, refund_number, refund_id, refund_status,
       total_fee AS original_cents, refund AS refund_cents, create_time, update_time
FROM tb_refund_info
WHERE order_number = '待核对订单号';
```

订单金额单位为元，支付流水的 `total`/`service_fee`、退款记录的 `total_fee`/`refund` 为分。
保留所有流水核对，不能默认最后一条就是有效实付。`trade_type=MOCK` 或交易号以 `MOCK_`
开头的是模拟记录，不作为真实到账依据。支付流水里的服务费同样取自当时订单快照，可能已经受损；
旧实现还使用浮点转分，应允许核对时识别转换误差。真实支付总额及退款结果以商户平台为准。

输出仅形成候选订单清单及证据，不执行 UPDATE。后续如需修复，先逐单确认正确金额、
覆盖发生时间和可信来源，再另行批准数据修复方案、备份与回滚步骤。
