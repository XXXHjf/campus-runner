-- 发布修复版本时暂停二手交易写入并备份数据库后执行；不删除历史议价、不发送历史通知。
-- 只关闭未关联订单的待回复及已接受报价；重新上架、订单取消均不恢复旧报价。
-- 没有历史证据的过去下架不自行推断。
-- 查询结果需在执行前保存，用于核对和按备份精确回滚。
select b.id, b.product_id, b.status, b.update_time
from tb_second_hand_bargain b
left join tb_second_hand_product p on p.id = b.product_id
where b.deleted = 0 and b.status in (0, 1)
  and not exists (select 1 from tb_second_hand_order linked where linked.bargain_id = b.id)
  and (p.id is null or p.deleted != 0 or p.status != 0 or exists (
      select 1 from tb_second_hand_order o
      where o.product_id = b.product_id and o.create_time >= b.create_time
  ));

start transaction;
update tb_second_hand_bargain b
left join tb_second_hand_product p on p.id = b.product_id
set b.status = 3, b.update_time = now()
where b.deleted = 0 and b.status in (0, 1)
  and not exists (select 1 from tb_second_hand_order linked where linked.bargain_id = b.id)
  and (p.id is null or p.deleted != 0 or p.status != 0 or exists (
      select 1 from tb_second_hand_order o
      where o.product_id = b.product_id and o.create_time >= b.create_time
  ));
commit;

-- 重跑上面的 select 应返回 0 条；脚本可重复执行。
