-- 二手商品收藏功能迁移
-- 执行前必须完成数据库备份；本脚本可重复执行。

CREATE TABLE IF NOT EXISTS `tb_second_hand_favorite` (
  `id` BIGINT AUTO_INCREMENT NOT NULL COMMENT '收藏关系主键id',
  `user_id` BIGINT NOT NULL COMMENT '收藏用户id，关联 tb_user.id',
  `product_id` BIGINT NOT NULL COMMENT '商品id，关联 tb_second_hand_product.id',
  `create_time` DATETIME NOT NULL COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_second_hand_favorite_user_product` (`user_id`, `product_id`),
  KEY `idx_second_hand_favorite_user_time` (`user_id`, `create_time`),
  KEY `idx_second_hand_favorite_product` (`product_id`)
) ENGINE=InnoDB COMMENT='二手商品收藏关系表';

-- 以关系表为准修正冗余计数，便于脚本重复执行和故障恢复。
UPDATE tb_second_hand_product p
LEFT JOIN (
  SELECT product_id, COUNT(*) AS favorite_count
  FROM tb_second_hand_favorite
  GROUP BY product_id
) f ON f.product_id = p.id
SET p.favorite_count = COALESCE(f.favorite_count, 0);
