# 二手交易线下模式迁移

对应脚本：`second-hand-offline-trade-migration.sql`。

## 目的与当前约束

当前二手模块只提供商品展示、私信、议价、下单和交付记录。平台不代收买家货款，不向卖家
结算，不收取二手交易服务费，也不承诺担保、退款或争议赔付。买卖双方自行协商付款与交付。

迁移为订单增加 `trade_mode` 快照：存量订单默认保留为 `ONLINE`，新订单写入 `OFFLINE`。
这样可以继续核对历史已付款订单，同时确保新订单不会误入旧支付、退款和卖家收款流程。

## 发布前核对

必须先备份数据库，再统计存量线上订单：

```sql
SELECT status, COUNT(*) AS order_count
FROM tb_second_hand_order
WHERE deleted = 0
GROUP BY status
ORDER BY status;

SELECT id, order_number, status, pay_time, transfer_state
FROM tb_second_hand_order
WHERE deleted = 0 AND status IN (0, 1, 2, 5, 7, 8, 10, 11)
ORDER BY create_time;
```

- 状态 `0` 的待支付订单应先确认微信侧没有成功付款，再关闭并恢复商品；不要只改数据库状态。
- 状态 `1/2/5/7/8/10/11` 可能涉及真实资金或待处理交付，必须逐单核对，不能批量改成线下订单。
- 若没有存量订单，也要保留查询结果作为发布记录。

## 执行顺序

1. 停止二手模块写入并备份数据库。
2. 执行迁移脚本。
3. 部署新版 API。
4. 发布新版管理端和小程序。

```bash
mysql -h 数据库地址 -u 数据库用户名 -p 数据库名 \
  < docs/database/second-hand-offline-trade-migration.sql
```

## 验证

```sql
SHOW COLUMNS FROM tb_second_hand_order LIKE 'trade_mode';

SELECT config_key, config_value
FROM tb_system_config
WHERE config_key = 'second_hand_trade_mode';

SELECT trade_mode, status, COUNT(*) AS order_count
FROM tb_second_hand_order
WHERE deleted = 0
GROUP BY trade_mode, status
ORDER BY trade_mode, status;

SELECT COUNT(*) AS invalid_offline_orders
FROM tb_second_hand_order
WHERE trade_mode = 'OFFLINE'
  AND status NOT IN (1, 2, 3, 4, 11);
```

配置值必须为 `OFFLINE`，最后一项必须为 `0`。随后分别验证直接下单和接受议价：订单应直接
进入待交付，商品进入交易中，服务费为零，双方电话可见，并且不会生成微信预支付参数。
再分别验证买家自提和卖家配送；配送订单必须保留下单时的买家地址快照。卖家标记交付前，
买家和卖家都应能取消并让商品恢复在售；标记交付后双方不能再自行取消。

## 回滚

代码回滚前先处理新版产生的 `OFFLINE` 订单，避免旧版把状态 `1` 误认为已经线上付款。确认
不存在未处理的线下订单后，才能将 `second_hand_trade_mode` 改回 `ONLINE` 并回滚应用。

优先保留 `trade_mode` 列；旧版显式列写入不会受该默认列影响。只有完成新备份并确认不再需要
区分历史交易模式时，才在维护窗口手工删除该列。
