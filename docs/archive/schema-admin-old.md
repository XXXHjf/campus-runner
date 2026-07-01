## 订单表

```sql
CREATE TABLE `tb_orders`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `pick_up_address` bigint(20) NULL DEFAULT NULL COMMENT '取件地址',
  `recive_address` bigint(20) NULL DEFAULT NULL COMMENT '收件地址',
  `order_number` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '订单号',
  `price` decimal(10, 2) NULL DEFAULT NULL COMMENT '订单基础金额(骑手实际收入)',
  `service_fee_rate` decimal(10, 2) NULL DEFAULT NULL COMMENT '付费费率快照',
  `service_fee` decimal(10, 2) NULL DEFAULT NULL COMMENT '服务费',
  `pay_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '用户支付总额',
  `real_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '禁用 实际成交价格',
  `delivery_time` datetime NULL DEFAULT NULL COMMENT '订单送达时间',
  `cancel_time` datetime NULL DEFAULT NULL COMMENT '订单取消时间',
  `cancel_reson` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '取消原因',
  `exceed_time` datetime NULL DEFAULT NULL COMMENT '超时时间',
  `gap` int(11) NULL DEFAULT NULL COMMENT '超时间隔',
  `create_time` datetime NULL DEFAULT NULL COMMENT '订单创建时间',
  `door_access` int(11) NULL DEFAULT NULL COMMENT '是否门禁(0 否 1 是)',
  `user_id` bigint(20) NULL DEFAULT NULL COMMENT '发单人id',
  `username` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '订单用的昵称',
  `phone` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '订单用的电话字段',
  `status` int(11) NULL DEFAULT NULL COMMENT '订单状态(-4退款异常 -3退款成功 -2退款中 -1未支付 0待接单 1已接单 2派送中 3已送达 4已取消 5已完成 6提现成功 7提现失败)',
  `note` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '订单说明信息',
  `image` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '图片路径',
  `category_id` bigint(20) NULL DEFAULT NULL COMMENT '类别id',
  `deleted` int(11) NULL DEFAULT 0 COMMENT '逻辑删除字段(0 未删除, 1 已删除)',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 286 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '订单表' ROW_FORMAT = Compact;
```

## 系统配置表

```sql
CREATE TABLE `tb_system_config`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `config_key` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '配置键',
  `config_value` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '配置值',
  `description` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '说明',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '系统配置表' ROW_FORMAT = Compact;
```

