# 二手商品收藏迁移

## 目的与约束

本迁移新增 `tb_second_hand_favorite`，记录“用户—商品”的唯一收藏关系。商品表已有的
`favorite_count` 继续作为列表和后台统计使用的冗余计数；收藏或取消收藏时，API 在同一事务
内更新关系与计数。

已售出或已下架的商品不会自动删除收藏关系，“我的收藏”会展示商品当前状态；逻辑删除的
商品不再出现在列表中。卖家不能收藏自己的商品。

## 发布顺序

1. 确认本次发布包含数据库迁移、API 和小程序。
2. 备份生产数据库，并记录可恢复的备份位置。
3. 执行 `docs/database/second-hand-favorite-migration.sql`。
4. 验证表、唯一索引和收藏计数后，再启动新版 API。
5. 发布小程序并完成收藏、取消收藏和“我的收藏”冒烟验证。

## 验证

```sql
SHOW CREATE TABLE tb_second_hand_favorite;

SELECT COUNT(*) AS duplicate_pairs
FROM (
  SELECT user_id, product_id
  FROM tb_second_hand_favorite
  GROUP BY user_id, product_id
  HAVING COUNT(*) > 1
) duplicates;

SELECT COUNT(*) AS inconsistent_products
FROM tb_second_hand_product p
LEFT JOIN (
  SELECT product_id, COUNT(*) AS favorite_count
  FROM tb_second_hand_favorite
  GROUP BY product_id
) f ON f.product_id = p.id
WHERE COALESCE(p.favorite_count, 0) <> COALESCE(f.favorite_count, 0);
```

`duplicate_pairs` 和 `inconsistent_products` 均应为 `0`。接口验证至少覆盖：重复收藏不重复
计数、重复取消不出现负数、自己的商品不能收藏、已售出或下架商品仍可在收藏列表识别状态。

## 回滚

先回滚小程序和 API，确认旧版本不再读写收藏表后，再执行：

```sql
DROP TABLE IF EXISTS tb_second_hand_favorite;
UPDATE tb_second_hand_product SET favorite_count = 0;
```

删除收藏表会永久丢失用户收藏关系。若仍需保留数据，应使用发布前数据库备份恢复，不要执行
上述删除语句。
