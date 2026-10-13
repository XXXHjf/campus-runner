# 订单号唯一性迁移

新跑腿编号使用 `R` 前缀，新二手编号使用 `SH` 前缀，追加随机 UUID 字符并保持总长 32。
编号不依赖机器时钟、单进程计数或服务器节点配置。数据库全字段唯一索引是最终防线。
只有插入抛出 `DuplicateKeyException` 且锁定查询确认候选订单号已存在时，才重新生成，
总共最多插入 5 次；其他唯一约束和其他异常直接抛出。地址快照、媒体绑定、报价失效及
商品状态更新都在最终插入之后执行，不因编号重试重复处理。

历史数字编号、历史 SH 编号及关联支付/退款/转账编号均不修改。新编号按字符串查询，
现有接单、支付和回调入口继续兼容历史编号。转账尝试编号仍由现有支持方法生成，保持
尝试序号和长度约束。

## 迁移前核验与备份

先执行 [生产发布必检](../deployment/server-operations.md#生产发布必检清单)，使用既有
受保护的连接方式备份数据库，并保留两张订单表原始建表语句、索引定义和备份恢复方法。
不要把凭证放在命令、文档或日志中。2026-10-12 已完成生产执行与隔离库演练，跑腿表新增唯一索引，二手已有索引保留；证据及回滚位置见 [业务发布记录](../deployment/server-operations.md#2026-10-12-业务发布记录)。后续环境仍需独立核验。

只读检查（包含隐藏、取消订单，不能只检查活动订单）：

```sql
SHOW CREATE TABLE tb_orders;
SHOW CREATE TABLE tb_second_hand_order;
SELECT order_number, COUNT(*) AS copies FROM tb_orders GROUP BY order_number HAVING COUNT(*) > 1;
SELECT order_number, COUNT(*) AS copies FROM tb_second_hand_order GROUP BY order_number HAVING COUNT(*) > 1;
SELECT COUNT(*) AS missing FROM tb_orders WHERE order_number IS NULL OR CHAR_LENGTH(TRIM(order_number)) = 0;
SELECT COUNT(*) AS missing FROM tb_second_hand_order WHERE order_number IS NULL OR CHAR_LENGTH(TRIM(order_number)) = 0;
```

订单号列必须为至少 32 字符的 CHAR/VARCHAR。空号、重复号、字段过短或索引同名但结构不符时，
停止发布，人工核对历史订单及资金关联；不自动删单、重编号、改资金流水或扩大字段。

## 执行与验证

暂停订单写入并确认备份可用后，执行 [order-number-uniqueness.sql](order-number-uniqueness.sql)。
脚本核对历史数据与索引，仅缺少完整单列唯一索引时新增；已有等价索引会保留，不重复建立。
通常二手表已有 `uk_second_hand_order_number`，跑腿表需补 `uk_runner_order_number`，实际状态
必须查目标数据库。脚本需要 ALTER、CREATE ROUTINE 权限。记录各表 `index_added` 输出。
DDL 会自动提交，一张表完成而另一张失败时不要声称全部完成；解决阻塞后可重新运行。

迁移完成后检查两张表都存在仅包含完整 `order_number` 的唯一索引，无重复和空号，再启动
依赖新编号的 API，并按生产文档验证健康与业务读取。在隔离测试库验证重复编号插入被拒绝，
不要向生产写入测试资金订单。人工核对新跑腿、二手订单及历史订单读取、支付和回调关联。

本地验证：`OrderNumberGeneratorTest` 覆盖并发生成、确认编号冲突重试、其他唯一冲突不重试、
有限重试和异常传播；两类服务创建测试覆盖实际重试与后续副作用；现有转账编号和历史回调
测试保留历史编号。模拟 Mapper 测试不等于真实 MySQL 唯一索引或事务验证。

## 回滚

优先仅回退 API，保留唯一索引以防止旧毫秒编号再次产生重复数据。确需回退结构时，仅移除
本次 `index_added=1` 的索引，绝不删除迁移前已有等价索引。先暂停订单写入并按生产备份流程
操作；移除索引会恢复并发重号风险。新随机编号不需要也不应转换回数字，历史数据不变。
不要未经核对直接执行全库恢复，否则会丢失迁移后的新订单。
