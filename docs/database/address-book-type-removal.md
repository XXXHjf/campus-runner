# 地址簿类型字段移除

## 背景

地址现在是用户的统一常用地址。取件或收件只表示地址在某一笔跑腿订单中的用途，不再是
地址簿记录自身的属性，因此 `tb_address_book.type` 已无业务含义。

## 变更范围

- 新增、编辑、查询和设置默认地址不再接收或返回 `type`。
- 跑腿发布页的取件地址与收件地址都读取同一份地址簿。
- `tb_address_book` 删除 `type` 列，其他地址数据不变。

## 执行顺序

1. 备份数据库，并保留当前 API JAR 作为回滚文件。
2. 停止旧版 API，避免旧版按全列顺序写入地址簿。
3. 执行 `address-book-type-removal.sql`。
4. 启动新版 API。
5. 验证地址列表、新增、编辑、设默认地址，以及发布跑腿时选择取件和收件地址。

新版 MyBatis 写入使用明确列名，即使回滚脚本临时恢复 `type` 列也能继续工作。

## 验证

```sql
SHOW COLUMNS FROM tb_address_book LIKE 'type';

SELECT id, compus_id, building_id, details, label, is_default
FROM tb_address_book
WHERE deleted = 0
ORDER BY id DESC
LIMIT 10;
```

第一条查询应返回空结果。随后分别调用地址列表、新增和编辑接口，确认不会出现 SQL
字段错误。

## 回滚

如果必须回滚到仍依赖 `type` 的旧版 API：

1. 停止新版 API。
2. 执行 `address-book-type-removal-rollback.sql`，恢复可空的 `type` 列。
3. 恢复旧版 API JAR。

回滚只能恢复表结构，不能还原历史地址的取件/收件分类；恢复后的 `type` 值均为空。
