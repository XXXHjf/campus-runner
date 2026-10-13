# 二手私信与卖家收款迁移

对应脚本：`second-hand-conversation-transfer-migration.sql`。

## 目的与约束

本次迁移为二手私信增加已读状态，为卖家微信收款保存可查询、可重试的独立状态，并新增
独立二手服务费率。迁移只增加字段和索引、扩大失败原因字段，不删除现有业务数据。

部署顺序必须是：停止发布写入窗口并备份数据库，执行迁移，部署新版 API，再发布新版
管理端和小程序。旧 API 不会使用新增列，但新版 API 启动前必须完成迁移。

## 执行

先生成可恢复的数据库备份，再执行：

```bash
mysql -h 数据库地址 -u 数据库用户名 -p 数据库名 \
  < docs/database/second-hand-conversation-transfer-migration.sql
```

脚本可重复执行。历史状态为收款中或收款失败的订单会沿用旧订单号作为当前商户转账单号，
用于先向微信查询真实结果；管理员不得在未查询前直接重新付款。

## 验证

```sql
SHOW COLUMNS FROM tb_second_hand_order LIKE 'transfer_out_bill_no';
SHOW COLUMNS FROM tb_second_hand_order LIKE 'transfer_state';
SHOW COLUMNS FROM tb_second_hand_order LIKE 'transfer_package_info';
SHOW COLUMNS FROM tb_second_hand_message LIKE 'read_time';

SHOW INDEX FROM tb_second_hand_order
WHERE Key_name = 'uk_second_hand_transfer_out_bill_no';

SELECT config_key, config_value
FROM tb_system_config
WHERE config_key = 'second_hand_service_fee_rate';

SELECT COUNT(*) AS missing_transfer_number
FROM tb_second_hand_order
WHERE status IN (8, 10) AND transfer_out_bill_no IS NULL;
```

最后一项应为 `0`。脚本首次插入的费率 `0.03` 仅是历史 ONLINE 兼容默认值；当前 OFFLINE 新订单的服务费固定为零，管理端隐藏二手费率，不通过修改它实现线下免服务费。

发布后验证当前 OFFLINE 下单、双方电话、私信及未读数。卖家微信确认收款及后台同步仅适用于存在的历史 ONLINE 订单，应在核对真实资金和场景准入后验证；不得新建线上订单作为常规冒烟。

## 回滚

优先回滚 API、管理端和小程序版本，保留新增列和索引；这些结构对旧版代码是向后兼容的，
也保留了收款核对证据。不要为了代码回滚立即删除转账状态或消息已读数据。

只有确认不再需要这些数据并完成新备份后，才可在维护窗口手工删除新增索引和字段。删除前
应导出 `transfer_out_bill_no`、`transfer_bill_no`、`transfer_state`、
`transfer_package_info` 和 `read_time`，并先确认所有收款中订单的微信真实状态。二手服务费
配置可以保留；旧版不会读取它。
