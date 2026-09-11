-- ============================================================
-- 校园二手交易 V1 拟真测试数据
-- ------------------------------------------------------------
-- 用途：
-- 1. 给二手交易首页、详情页、收藏页、议价页、订单页和后台管理生成少量可测试数据。
-- 2. 覆盖商品在售、交易中、已售出、已下架等关键状态。
-- 3. 覆盖议价待回复、已接受、已拒绝，以及线下待交付、待确认和已完成订单。
-- 4. 覆盖在售、交易中、已售出和已下架商品的收藏展示。
--
-- 使用方式：
-- 1. 先执行 docs/database/second-hand-schema.sql。
-- 2. 再执行本文件。
--
-- 幂等说明：
-- - 本脚本使用固定 openid、商品标题和订单号判断是否已插入。
-- - 重复执行不会重复生成同一批测试数据。
--
-- 注意：
-- - 二手商品列表会按当前登录用户的 school_id 过滤。
-- - 如果你的真实微信登录用户看不到数据，请先确认该用户已认证，并且 school_id
--   与本脚本使用的 @seed_school_id 一致。文件末尾提供了可选修复 SQL。
-- ============================================================

-- ============================================================
-- 1. 准备学校和校区
--    优先使用库里已有的第一所学校；如果库里没有学校，则创建“测试大学”。
-- ============================================================
INSERT INTO tb_school(school_name, number_id, deleted)
SELECT '测试大学', 900001, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM tb_school WHERE deleted = 0
);

SET @seed_school_id = (
  SELECT id FROM tb_school WHERE deleted = 0 ORDER BY id ASC LIMIT 1
);

SET @seed_school_name = (
  SELECT school_name FROM tb_school WHERE id = @seed_school_id LIMIT 1
);

INSERT INTO tb_compus(school_id, school_name, number_id, compus_name, deleted)
SELECT @seed_school_id, @seed_school_name, 1, '主校区', 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1
  FROM tb_compus
  WHERE school_id = @seed_school_id
    AND deleted = 0
);

SET @seed_compus_id = (
  SELECT id
  FROM tb_compus
  WHERE school_id = @seed_school_id
    AND deleted = 0
  ORDER BY id ASC
  LIMIT 1
);

-- ============================================================
-- 2. 准备二手分类
-- ============================================================
INSERT INTO tb_second_hand_category(name, image, sort, deleted)
SELECT '数码电子', '', 10, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM tb_second_hand_category WHERE name = '数码电子' AND deleted = 0
);

INSERT INTO tb_second_hand_category(name, image, sort, deleted)
SELECT '文具教材', '', 20, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM tb_second_hand_category WHERE name = '文具教材' AND deleted = 0
);

INSERT INTO tb_second_hand_category(name, image, sort, deleted)
SELECT '生活用品', '', 30, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM tb_second_hand_category WHERE name = '生活用品' AND deleted = 0
);

INSERT INTO tb_second_hand_category(name, image, sort, deleted)
SELECT '运动户外', '', 40, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM tb_second_hand_category WHERE name = '运动户外' AND deleted = 0
);

SET @cat_digital = (
  SELECT id FROM tb_second_hand_category WHERE name = '数码电子' AND deleted = 0 ORDER BY id ASC LIMIT 1
);
SET @cat_book = (
  SELECT id FROM tb_second_hand_category WHERE name = '文具教材' AND deleted = 0 ORDER BY id ASC LIMIT 1
);
SET @cat_life = (
  SELECT id FROM tb_second_hand_category WHERE name = '生活用品' AND deleted = 0 ORDER BY id ASC LIMIT 1
);
SET @cat_sport = (
  SELECT id FROM tb_second_hand_category WHERE name = '运动户外' AND deleted = 0 ORDER BY id ASC LIMIT 1
);

-- ============================================================
-- 3. 准备已认证测试用户
--    authentication = 1：已校园认证。
--    student_id_card_review = 2：学生证审核通过。
-- ============================================================
INSERT INTO tb_user(
  username, realname, openid, sex, phone, authentication,
  school_id, stu_id, student_id_card_review, score, money,
  is_manager, deleted, create_time, update_time
)
SELECT
  '林同学', '林雨晴', 'seed_second_seller_001', 0, '13800001001', 1,
  @seed_school_id, '20260001', 2, 100, 0.00,
  0, 0, NOW() - INTERVAL 18 DAY, NOW() - INTERVAL 1 DAY
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM tb_user WHERE openid = 'seed_second_seller_001' AND deleted = 0
);

INSERT INTO tb_user(
  username, realname, openid, sex, phone, authentication,
  school_id, stu_id, student_id_card_review, score, money,
  is_manager, deleted, create_time, update_time
)
SELECT
  '周同学', '周亦然', 'seed_second_seller_002', 1, '13800001002', 1,
  @seed_school_id, '20260002', 2, 98, 0.00,
  0, 0, NOW() - INTERVAL 16 DAY, NOW() - INTERVAL 2 DAY
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM tb_user WHERE openid = 'seed_second_seller_002' AND deleted = 0
);

INSERT INTO tb_user(
  username, realname, openid, sex, phone, authentication,
  school_id, stu_id, student_id_card_review, score, money,
  is_manager, deleted, create_time, update_time
)
SELECT
  '陈同学', '陈星河', 'seed_second_buyer_001', 1, '13800002001', 1,
  @seed_school_id, '20260003', 2, 100, 0.00,
  0, 0, NOW() - INTERVAL 12 DAY, NOW() - INTERVAL 1 DAY
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM tb_user WHERE openid = 'seed_second_buyer_001' AND deleted = 0
);

INSERT INTO tb_user(
  username, realname, openid, sex, phone, authentication,
  school_id, stu_id, student_id_card_review, score, money,
  is_manager, deleted, create_time, update_time
)
SELECT
  '许同学', '许知夏', 'seed_second_buyer_002', 0, '13800002002', 1,
  @seed_school_id, '20260004', 2, 96, 0.00,
  0, 0, NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 3 DAY
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM tb_user WHERE openid = 'seed_second_buyer_002' AND deleted = 0
);

SET @seller_lin = (
  SELECT id FROM tb_user WHERE openid = 'seed_second_seller_001' AND deleted = 0 LIMIT 1
);
SET @seller_zhou = (
  SELECT id FROM tb_user WHERE openid = 'seed_second_seller_002' AND deleted = 0 LIMIT 1
);
SET @buyer_chen = (
  SELECT id FROM tb_user WHERE openid = 'seed_second_buyer_001' AND deleted = 0 LIMIT 1
);
SET @buyer_xu = (
  SELECT id FROM tb_user WHERE openid = 'seed_second_buyer_002' AND deleted = 0 LIMIT 1
);

-- ============================================================
-- 4. 准备二手商品
--    status：0在售，1历史锁定态，2交易中，3已售出，4已下架。
-- ============================================================
INSERT INTO tb_second_hand_product(
  seller_id, school_id, compus_id, category_id, title, description,
  condition_level, price, pickup_address_snapshot, pickup_only, negotiable,
  status, view_count, favorite_count, deleted, create_time, update_time
)
SELECT
  @seller_lin, @seed_school_id, @seed_compus_id, @cat_digital,
  '[测试] iPad Air 5 64G 深空灰',
  '自用 iPad Air 5，屏幕无划痕，电池状态正常，送保护壳和二代笔替代笔。适合上课记笔记。',
  '九成新', 2850.00, '主校区图书馆一楼大厅', 1, 1,
  0, 38, 0, 0, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 2 HOUR
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM tb_second_hand_product WHERE title = '[测试] iPad Air 5 64G 深空灰' AND deleted = 0
);

INSERT INTO tb_second_hand_product(
  seller_id, school_id, compus_id, category_id, title, description,
  condition_level, price, pickup_address_snapshot, pickup_only, negotiable,
  status, view_count, favorite_count, deleted, create_time, update_time
)
SELECT
  @seller_zhou, @seed_school_id, @seed_compus_id, @cat_book,
  '[测试] 高等数学同济第七版上下册',
  '上下册合售，有少量铅笔笔记，期末复习很够用。另送一本线代习题册。',
  '八成新', 32.00, '教学楼 A 座门口', 0, 1,
  0, 21, 0, 0, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 5 HOUR
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM tb_second_hand_product WHERE title = '[测试] 高等数学同济第七版上下册' AND deleted = 0
);

INSERT INTO tb_second_hand_product(
  seller_id, school_id, compus_id, category_id, title, description,
  condition_level, price, pickup_address_snapshot, pickup_only, negotiable,
  status, view_count, favorite_count, deleted, create_time, update_time
)
SELECT
  @seller_lin, @seed_school_id, @seed_compus_id, @cat_sport,
  '[测试] 迪卡侬山地车 适合校园通勤',
  '刹车和变速都正常，车筐有一点旧。支持晚饭后送到宿舍区附近。',
  '七成新', 420.00, '东门快递站旁', 0, 1,
  2, 55, 0, 0, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 HOUR
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM tb_second_hand_product WHERE title = '[测试] 迪卡侬山地车 适合校园通勤' AND deleted = 0
);

INSERT INTO tb_second_hand_product(
  seller_id, school_id, compus_id, category_id, title, description,
  condition_level, price, pickup_address_snapshot, pickup_only, negotiable,
  status, view_count, favorite_count, deleted, create_time, update_time
)
SELECT
  @seller_zhou, @seed_school_id, @seed_compus_id, @cat_digital,
  '[测试] Sony WH-1000XM4 降噪耳机',
  '功能正常，耳罩去年换过，轻微使用痕迹。当前已有线下订单，用来测试交易中状态。',
  '八成新', 799.00, '生活区 3 号楼楼下', 1, 1,
  2, 46, 0, 0, NOW() - INTERVAL 8 HOUR, NOW() - INTERVAL 20 MINUTE
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM tb_second_hand_product WHERE title = '[测试] Sony WH-1000XM4 降噪耳机' AND deleted = 0
);

INSERT INTO tb_second_hand_product(
  seller_id, school_id, compus_id, category_id, title, description,
  condition_level, price, pickup_address_snapshot, pickup_only, negotiable,
  status, view_count, favorite_count, deleted, create_time, update_time
)
SELECT
  @seller_lin, @seed_school_id, @seed_compus_id, @cat_life,
  '[测试] 小熊电煮锅 1.5L',
  '宿舍煮面神器，已下单待交付，用来测试卖家标记交付和买家确认完成。',
  '九成新', 58.00, '南门便利店门口', 1, 0,
  2, 19, 0, 0, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 30 MINUTE
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM tb_second_hand_product WHERE title = '[测试] 小熊电煮锅 1.5L' AND deleted = 0
);

INSERT INTO tb_second_hand_product(
  seller_id, school_id, compus_id, category_id, title, description,
  condition_level, price, pickup_address_snapshot, pickup_only, negotiable,
  status, view_count, favorite_count, deleted, create_time, update_time
)
SELECT
  @seller_zhou, @seed_school_id, @seed_compus_id, @cat_book,
  '[测试] 四级真题试卷 近三年',
  '已成交商品，用来测试已售出列表和订单完成态。',
  '八成新', 18.00, '图书馆自习区门口', 1, 1,
  3, 12, 0, 0, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 1 DAY
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM tb_second_hand_product WHERE title = '[测试] 四级真题试卷 近三年' AND deleted = 0
);

INSERT INTO tb_second_hand_product(
  seller_id, school_id, compus_id, category_id, title, description,
  condition_level, price, pickup_address_snapshot, pickup_only, negotiable,
  status, view_count, favorite_count, deleted, create_time, update_time
)
SELECT
  @seller_lin, @seed_school_id, @seed_compus_id, @cat_life,
  '[测试] 台灯 已下架样例',
  '后台下架样例，前台默认列表不展示，后台商品管理可查看。',
  '七成新', 25.00, '西区食堂门口', 1, 1,
  4, 7, 0, 0, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 1 DAY
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM tb_second_hand_product WHERE title = '[测试] 台灯 已下架样例' AND deleted = 0
);

SET @p_ipad = (
  SELECT id FROM tb_second_hand_product WHERE title = '[测试] iPad Air 5 64G 深空灰' AND deleted = 0 LIMIT 1
);
SET @p_books = (
  SELECT id FROM tb_second_hand_product WHERE title = '[测试] 高等数学同济第七版上下册' AND deleted = 0 LIMIT 1
);
SET @p_bike = (
  SELECT id FROM tb_second_hand_product WHERE title = '[测试] 迪卡侬山地车 适合校园通勤' AND deleted = 0 LIMIT 1
);
SET @p_headphone = (
  SELECT id FROM tb_second_hand_product WHERE title = '[测试] Sony WH-1000XM4 降噪耳机' AND deleted = 0 LIMIT 1
);
SET @p_pot = (
  SELECT id FROM tb_second_hand_product WHERE title = '[测试] 小熊电煮锅 1.5L' AND deleted = 0 LIMIT 1
);
SET @p_exam = (
  SELECT id FROM tb_second_hand_product WHERE title = '[测试] 四级真题试卷 近三年' AND deleted = 0 LIMIT 1
);
SET @p_lamp = (
  SELECT id FROM tb_second_hand_product WHERE title = '[测试] 台灯 已下架样例' AND deleted = 0 LIMIT 1
);

-- ============================================================
-- 5. 准备商品收藏
--    两名买家都收藏部分在售商品，并分别覆盖交易中、已售出和已下架状态。
-- ============================================================
INSERT IGNORE INTO tb_second_hand_favorite(user_id, product_id, create_time)
VALUES
  (@buyer_chen, @p_ipad, NOW() - INTERVAL 2 DAY),
  (@buyer_xu, @p_ipad, NOW() - INTERVAL 1 DAY),
  (@buyer_chen, @p_books, NOW() - INTERVAL 10 HOUR),
  (@buyer_xu, @p_headphone, NOW() - INTERVAL 6 HOUR),
  (@buyer_chen, @p_exam, NOW() - INTERVAL 4 HOUR),
  (@buyer_xu, @p_lamp, NOW() - INTERVAL 2 HOUR);

UPDATE tb_second_hand_product p
LEFT JOIN (
  SELECT product_id, COUNT(*) AS favorite_count
  FROM tb_second_hand_favorite
  GROUP BY product_id
) f ON f.product_id = p.id
SET p.favorite_count = COALESCE(f.favorite_count, 0)
WHERE p.id IN (@p_ipad, @p_books, @p_bike, @p_headphone, @p_pot, @p_exam, @p_lamp);

-- ============================================================
-- 6. 准备议价记录
--    status：0待回复，1已接受，2已拒绝，3已失效。
-- ============================================================
INSERT INTO tb_second_hand_bargain(
  product_id, buyer_id, seller_id, offer_price, message,
  status, attempt_no, deleted, create_time, update_time
)
SELECT
  @p_ipad, @buyer_chen, @seller_lin, 2680.00,
  '同学你好，今天晚上图书馆能自提的话 2680 可以吗？',
  0, 1, 0, NOW() - INTERVAL 3 HOUR, NOW() - INTERVAL 3 HOUR
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1
  FROM tb_second_hand_bargain
  WHERE product_id = @p_ipad
    AND buyer_id = @buyer_chen
    AND attempt_no = 1
    AND deleted = 0
);

INSERT INTO tb_second_hand_bargain(
  product_id, buyer_id, seller_id, offer_price, message,
  status, attempt_no, deleted, create_time, update_time
)
SELECT
  @p_books, @buyer_xu, @seller_zhou, 25.00,
  '我只要高数上下册，25 元可以吗？',
  2, 1, 0, NOW() - INTERVAL 7 HOUR, NOW() - INTERVAL 6 HOUR
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1
  FROM tb_second_hand_bargain
  WHERE product_id = @p_books
    AND buyer_id = @buyer_xu
    AND attempt_no = 1
    AND deleted = 0
);

INSERT INTO tb_second_hand_bargain(
  product_id, buyer_id, seller_id, offer_price, message,
  status, attempt_no, deleted, create_time, update_time
)
SELECT
  @p_headphone, @buyer_chen, @seller_zhou, 760.00,
  '今晚生活区 3 号楼可以自提，760 线下交易。',
  1, 1, 0, NOW() - INTERVAL 35 MINUTE, NOW() - INTERVAL 30 MINUTE
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1
  FROM tb_second_hand_bargain
  WHERE product_id = @p_headphone
    AND buyer_id = @buyer_chen
    AND attempt_no = 1
    AND deleted = 0
);

SET @b_headphone = (
  SELECT id
  FROM tb_second_hand_bargain
  WHERE product_id = @p_headphone
    AND buyer_id = @buyer_chen
    AND attempt_no = 1
    AND deleted = 0
  LIMIT 1
);

-- ============================================================
-- 7. 准备二手订单
--    trade_mode：OFFLINE；status：1待交付，2已交付待确认，3已完成。
-- ============================================================
INSERT INTO tb_second_hand_order(
  order_number, product_id, bargain_id, buyer_id, seller_id, trade_mode,
  product_amount, pay_amount, service_fee_rate, service_fee, seller_income,
  delivery_mode, delivery_remark, status, pay_time, cancel_time, cancel_reason,
  delivered_time, confirm_deadline, finish_time, transfer_time, transfer_fail_reason,
  deleted, create_time, update_time
)
SELECT
  'SHSEED202607060001', @p_headphone, @b_headphone, @buyer_chen, @seller_zhou, 'OFFLINE',
  760.00, 760.00, 0.0000, 0.00, 760.00,
  0, '生活区 3 号楼楼下自提', 1, NULL, NULL, NULL,
  NULL, NULL, NULL, NULL, NULL,
  0, NOW() - INTERVAL 25 MINUTE, NOW() - INTERVAL 25 MINUTE
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM tb_second_hand_order WHERE order_number = 'SHSEED202607060001' AND deleted = 0
);

INSERT INTO tb_second_hand_order(
  order_number, product_id, bargain_id, buyer_id, seller_id, trade_mode,
  product_amount, pay_amount, service_fee_rate, service_fee, seller_income,
  delivery_mode, delivery_remark, status, pay_time, cancel_time, cancel_reason,
  delivered_time, confirm_deadline, finish_time, transfer_time, transfer_fail_reason,
  deleted, create_time, update_time
)
SELECT
  'SHSEED202607060002', @p_pot, NULL, @buyer_xu, @seller_lin, 'OFFLINE',
  58.00, 58.00, 0.0000, 0.00, 58.00,
  0, '南门便利店门口，晚 8 点左右', 1, NULL, NULL, NULL,
  NULL, NULL, NULL, NULL, NULL,
  0, NOW() - INTERVAL 1 HOUR, NOW() - INTERVAL 45 MINUTE
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM tb_second_hand_order WHERE order_number = 'SHSEED202607060002' AND deleted = 0
);

INSERT INTO tb_second_hand_order(
  order_number, product_id, bargain_id, buyer_id, seller_id, trade_mode,
  product_amount, pay_amount, service_fee_rate, service_fee, seller_income,
  delivery_mode, delivery_remark, status, pay_time, cancel_time, cancel_reason,
  delivered_time, confirm_deadline, finish_time, transfer_time, transfer_fail_reason,
  deleted, create_time, update_time
)
SELECT
  'SHSEED202607060003', @p_bike, NULL, @buyer_chen, @seller_lin, 'OFFLINE',
  420.00, 420.00, 0.0000, 0.00, 420.00,
  1, '卖家配送到东门快递站旁，已交付，等待买家确认', 2, NULL, NULL, NULL,
  NOW() - INTERVAL 90 MINUTE, NULL, NULL, NULL, NULL,
  0, NOW() - INTERVAL 4 HOUR, NOW() - INTERVAL 90 MINUTE
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM tb_second_hand_order WHERE order_number = 'SHSEED202607060003' AND deleted = 0
);

INSERT INTO tb_second_hand_order(
  order_number, product_id, bargain_id, buyer_id, seller_id, trade_mode,
  product_amount, pay_amount, service_fee_rate, service_fee, seller_income,
  delivery_mode, delivery_remark, status, pay_time, cancel_time, cancel_reason,
  delivered_time, confirm_deadline, finish_time, transfer_time, transfer_fail_reason,
  deleted, create_time, update_time
)
SELECT
  'SHSEED202607060004', @p_exam, NULL, @buyer_xu, @seller_zhou, 'OFFLINE',
  18.00, 18.00, 0.0000, 0.00, 18.00,
  0, '图书馆自习区门口已自提', 3, NULL, NULL, NULL,
  NOW() - INTERVAL 2 DAY + INTERVAL 30 MINUTE,
  NULL,
  NOW() - INTERVAL 1 DAY,
  NULL,
  NULL,
  0, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 23 HOUR
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM tb_second_hand_order WHERE order_number = 'SHSEED202607060004' AND deleted = 0
);

SET @o_headphone = (
  SELECT id FROM tb_second_hand_order WHERE order_number = 'SHSEED202607060001' AND deleted = 0 LIMIT 1
);
SET @o_pot = (
  SELECT id FROM tb_second_hand_order WHERE order_number = 'SHSEED202607060002' AND deleted = 0 LIMIT 1
);
SET @o_bike = (
  SELECT id FROM tb_second_hand_order WHERE order_number = 'SHSEED202607060003' AND deleted = 0 LIMIT 1
);

-- ============================================================
-- 8. 准备私密留言
-- ============================================================
INSERT INTO tb_second_hand_message(product_id, order_id, sender_id, receiver_id, content, deleted, create_time)
SELECT @p_ipad, NULL, @buyer_chen, @seller_lin, '同学，iPad 屏幕有贴膜吗？电池续航上课记笔记够用吗？', 0, NOW() - INTERVAL 4 HOUR
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM tb_second_hand_message
  WHERE product_id = @p_ipad
    AND sender_id = @buyer_chen
    AND receiver_id = @seller_lin
    AND content = '同学，iPad 屏幕有贴膜吗？电池续航上课记笔记够用吗？'
    AND deleted = 0
);

INSERT INTO tb_second_hand_message(product_id, order_id, sender_id, receiver_id, content, deleted, create_time)
SELECT @p_ipad, NULL, @seller_lin, @buyer_chen, '有钢化膜，平时上一下午课没问题，保护壳也一起送。', 0, NOW() - INTERVAL 3 HOUR - INTERVAL 40 MINUTE
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM tb_second_hand_message
  WHERE product_id = @p_ipad
    AND sender_id = @seller_lin
    AND receiver_id = @buyer_chen
    AND content = '有钢化膜，平时上一下午课没问题，保护壳也一起送。'
    AND deleted = 0
);

INSERT INTO tb_second_hand_message(product_id, order_id, sender_id, receiver_id, content, deleted, create_time)
SELECT @p_headphone, @o_headphone, @seller_zhou, @buyer_chen, '我已接受议价，订单已经生成，今晚见面验货后再付款。', 0, NOW() - INTERVAL 28 MINUTE
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM tb_second_hand_message
  WHERE product_id = @p_headphone
    AND order_id = @o_headphone
    AND sender_id = @seller_zhou
    AND receiver_id = @buyer_chen
    AND content = '我已接受议价，订单已经生成，今晚见面验货后再付款。'
    AND deleted = 0
);

INSERT INTO tb_second_hand_message(product_id, order_id, sender_id, receiver_id, content, deleted, create_time)
SELECT @p_pot, @o_pot, @buyer_xu, @seller_lin, '晚上 8 点南门便利店门口见，我验货后当面付款。', 0, NOW() - INTERVAL 40 MINUTE
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM tb_second_hand_message
  WHERE product_id = @p_pot
    AND order_id = @o_pot
    AND sender_id = @buyer_xu
    AND receiver_id = @seller_lin
    AND content = '晚上 8 点南门便利店门口见，我验货后当面付款。'
    AND deleted = 0
);

INSERT INTO tb_second_hand_message(product_id, order_id, sender_id, receiver_id, content, deleted, create_time)
SELECT @p_bike, @o_bike, @seller_lin, @buyer_chen, '车已经送到东门快递站旁边，请检查后确认交易完成。', 0, NOW() - INTERVAL 80 MINUTE
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM tb_second_hand_message
  WHERE product_id = @p_bike
    AND order_id = @o_bike
    AND sender_id = @seller_lin
    AND receiver_id = @buyer_chen
    AND content = '车已经送到东门快递站旁边，请检查后确认交易完成。'
    AND deleted = 0
);

-- ============================================================
-- 9. 执行后检查
-- ============================================================
SELECT
  @seed_school_id AS seed_school_id,
  @seed_school_name AS seed_school_name,
  @seed_compus_id AS seed_compus_id;

SELECT id, name, sort
FROM tb_second_hand_category
WHERE deleted = 0
ORDER BY sort ASC, id ASC;

SELECT id, title, price, status, favorite_count, seller_id, school_id
FROM tb_second_hand_product
WHERE title LIKE '[测试]%'
  AND deleted = 0
ORDER BY create_time DESC;

SELECT f.id, f.user_id, f.product_id, f.create_time
FROM tb_second_hand_favorite f
INNER JOIN tb_second_hand_product p ON p.id = f.product_id
WHERE p.title LIKE '[测试]%'
ORDER BY f.create_time DESC;

SELECT id, order_number, product_id, buyer_id, seller_id, trade_mode, pay_amount, status
FROM tb_second_hand_order
WHERE order_number LIKE 'SHSEED%'
  AND deleted = 0
ORDER BY create_time DESC;

-- ============================================================
-- 10. 可选：让当前真实登录用户能看到这批数据
-- ------------------------------------------------------------
-- 如果你用自己的微信登录后看不到二手数据，通常是当前用户未认证或 school_id 不一致。
-- 先查出自己的用户 id：
--
-- SELECT id, username, openid, authentication, school_id, student_id_card_review
-- FROM tb_user
-- WHERE deleted = 0
-- ORDER BY id DESC;
--
-- 然后把下面的 你的用户id 替换成真实 id 后执行：
--
-- UPDATE tb_user
-- SET authentication = 1,
--     school_id = @seed_school_id,
--     student_id_card_review = 2,
--     update_time = NOW()
-- WHERE id = 你的用户id
--   AND deleted = 0;
-- ============================================================
