-- 发布修复版本时暂停二手交易写入并备份数据库后执行；不删除历史议价。
-- 订单取消后也不恢复旧报价：只关闭下单之前产生的待处理议价。
-- 查询结果需在执行前保存，用于核对和按备份精确回滚。
select b.id, b.product_id, b.status, b.update_time
from tb_second_hand_bargain b
join tb_second_hand_product p on p.id = b.product_id
where b.deleted = 0 and b.status = 0
  and (p.status in (1, 2, 3) or exists (
      select 1 from tb_second_hand_order o
      where o.product_id = b.product_id and o.create_time >= b.create_time
  ));

start transaction;
update tb_second_hand_bargain b
join tb_second_hand_product p on p.id = b.product_id
set b.status = 3, b.update_time = now()
where b.deleted = 0 and b.status = 0
  and (p.status in (1, 2, 3) or exists (
      select 1 from tb_second_hand_order o
      where o.product_id = b.product_id and o.create_time >= b.create_time
  ));
commit;

-- 重跑上面的 select 应返回 0 条；脚本可重复执行。
