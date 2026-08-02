package com.mikasa.campusrunner.service.impl.user;

import com.alibaba.fastjson.JSONObject;
import com.mikasa.campusrunner.common.constant.DeleteConstant;
import com.mikasa.campusrunner.common.constant.MediaAssetConstant;
import com.mikasa.campusrunner.common.constant.MediaPurpose;
import com.mikasa.campusrunner.common.constant.MessageConstant;
import com.mikasa.campusrunner.common.constant.SecondHandConstant;
import com.mikasa.campusrunner.common.constant.WeChatPayConstant;
import com.mikasa.campusrunner.common.constant.WeChatTransferConstant;
import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.ParamException;
import com.mikasa.campusrunner.common.exception.SecondHandException;
import com.mikasa.campusrunner.common.exception.UserException;
import com.mikasa.campusrunner.common.properties.WeChatProperties;
import com.mikasa.campusrunner.common.utils.WeChatPayUtil;
import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.dto.*;
import com.mikasa.campusrunner.pojo.dto.admin.AdminSecondHandCategoryDTO;
import com.mikasa.campusrunner.pojo.entity.*;
import com.mikasa.campusrunner.pojo.vo.*;
import com.mikasa.campusrunner.service.user.SecondHandService;
import com.mikasa.campusrunner.service.MediaAssetService;
import com.wechat.pay.contrib.apache.httpclient.util.AesUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class SecondHandServiceImpl implements SecondHandService {
    @Autowired
    private SecondHandCategoryMapper categoryMapper;
    @Autowired
    private SecondHandProductMapper productMapper;
    @Autowired
    private SecondHandOrderMapper orderMapper;
    @Autowired
    private SecondHandBargainMapper bargainMapper;
    @Autowired
    private SecondHandMessageMapper messageMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AdminSystemConfigMapper configMapper;
    @Autowired
    private PaymentLogMapper paymentLogMapper;
    @Autowired
    private WxTransferLogMapper wxTransferLogMapper;
    @Autowired
    private RefundInfoMapper refundInfoMapper;
    @Autowired
    private WeChatProperties weChatProperties;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private MediaAssetService mediaAssetService;
    @Autowired
    private CloseableHttpClient wxPayClient;
    @Value("${com.mikasa.campus-runner.dev.mock-payment-enabled:false}")
    private Boolean mockPaymentEnabled;

    private final ReentrantLock payNotifyLock = new ReentrantLock();
    private final ReentrantLock transferNotifyLock = new ReentrantLock();
    private final ReentrantLock refundNotifyLock = new ReentrantLock();

    @Override
    public List<SecondHandCategory> listCategories() {
        List<SecondHandCategory> categories = categoryMapper.list();
        categories.forEach(this::resolveCategoryImage);
        return categories;
    }

    @Override
    @Transactional
    public SecondHandCategory saveCategory(AdminSecondHandCategoryDTO dto) {
        validateCategory(dto, true);
        SecondHandCategory category = new SecondHandCategory();
        category.setName(dto.getName().trim());
        category.setSort(dto.getSort() == null ? 0 : dto.getSort());
        category.setDeleted(DeleteConstant.UN_DELETED);
        categoryMapper.insert(category);
        mediaAssetService.replaceBinding(
                List.of(dto.getImageAssetId()),
                MediaPurpose.SECOND_HAND_CATEGORY_ICON.name(),
                MediaAssetConstant.OWNER_ADMIN,
                BaseContext.getCurrentId(),
                MediaAssetConstant.BOUND_SECOND_HAND_CATEGORY,
                category.getId(),
                1,
                Duration.ofDays(7));
        resolveCategoryImage(category);
        return category;
    }

    @Override
    @Transactional
    public void updateCategory(Long id, AdminSecondHandCategoryDTO dto) {
        validateCategory(dto, false);
        SecondHandCategory existing = categoryMapper.getByIdForUpdate(id);
        if (existing == null) {
            throw new SecondHandException("分类不存在");
        }

        SecondHandCategory update = new SecondHandCategory();
        update.setId(id);
        update.setName(dto.getName() == null ? null : dto.getName().trim());
        update.setSort(dto.getSort());

        if (dto.getImageAssetId() != null) {
            mediaAssetService.replaceBinding(
                    List.of(dto.getImageAssetId()),
                    MediaPurpose.SECOND_HAND_CATEGORY_ICON.name(),
                    MediaAssetConstant.OWNER_ADMIN,
                    BaseContext.getCurrentId(),
                    MediaAssetConstant.BOUND_SECOND_HAND_CATEGORY,
                    id,
                    1,
                    Duration.ofDays(7));
        }
        categoryMapper.update(update);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        SecondHandCategory existing = categoryMapper.getByIdForUpdate(id);
        if (existing == null) {
            throw new SecondHandException("分类不存在");
        }
        categoryMapper.deleteById(id);
        mediaAssetService.replaceBinding(
                List.of(),
                MediaPurpose.SECOND_HAND_CATEGORY_ICON.name(),
                MediaAssetConstant.OWNER_ADMIN,
                BaseContext.getCurrentId(),
                MediaAssetConstant.BOUND_SECOND_HAND_CATEGORY,
                id,
                1,
                Duration.ofDays(7));
    }

    @Override
    @Transactional
    public SecondHandProductVO publishProduct(SecondHandProductDTO dto) {
        ensureAuthenticated();
        validateProduct(dto);
        Long userId = BaseContext.getCurrentId();
        Long schoolId = userMapper.getSchoolId(userId);
        LocalDateTime now = LocalDateTime.now();
        Integer pickupOnly = resolvePickupOnly(dto);
        String pickupSnapshot = resolvePickupSnapshot(dto);
        SecondHandProduct product = SecondHandProduct.builder()
                .sellerId(userId)
                .schoolId(schoolId)
                .compusId(dto.getCompusId())
                .categoryId(dto.getCategoryId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .conditionLevel(dto.getConditionLevel())
                .price(dto.getPrice())
                .pickupAddressId(dto.getPickupAddressId())
                .pickupAddressSnapshot(pickupSnapshot)
                .pickupOnly(pickupOnly)
                .negotiable(defaultOne(dto.getNegotiable()))
                .status(SecondHandConstant.PRODUCT_ON_SALE)
                .viewCount(0)
                .favoriteCount(0)
                .deleted(DeleteConstant.UN_DELETED)
                .createTime(now)
                .updateTime(now)
                .build();
        productMapper.insert(product);
        if (dto.getImageAssetIds() != null) {
            mediaAssetService.replaceBinding(
                    dto.getImageAssetIds(),
                    MediaPurpose.SECOND_HAND_PRODUCT_IMAGE.name(),
                    MediaAssetConstant.OWNER_USER,
                    userId,
                    MediaAssetConstant.BOUND_SECOND_HAND_PRODUCT,
                    product.getId(),
                    6,
                    Duration.ofDays(7));
        }
        return resolveProductImages(productMapper.detail(product.getId()));
    }

    @Override
    @Transactional
    public void updateProduct(Long id, SecondHandProductDTO dto) {
        ensureAuthenticated();
        SecondHandProduct product = requireProduct(id);
        ensureOwner(product.getSellerId());
        if (!isStatus(product.getStatus(), SecondHandConstant.PRODUCT_ON_SALE) &&
                !isStatus(product.getStatus(), SecondHandConstant.PRODUCT_OFF_SHELF)) {
            throw new SecondHandException("交易中的商品不可编辑");
        }
        SecondHandProduct update = new SecondHandProduct();
        BeanUtils.copyProperties(dto, update);
        update.setId(id);
        if (dto.getPickupOnly() != null) {
            Integer pickupOnly = resolvePickupOnly(dto);
            update.setPickupOnly(pickupOnly);
        }
        update.setUpdateTime(LocalDateTime.now());
        productMapper.update(update);
        if (dto.getImageAssetIds() != null) {
            mediaAssetService.replaceBinding(
                    dto.getImageAssetIds(),
                    MediaPurpose.SECOND_HAND_PRODUCT_IMAGE.name(),
                    MediaAssetConstant.OWNER_USER,
                    BaseContext.getCurrentId(),
                    MediaAssetConstant.BOUND_SECOND_HAND_PRODUCT,
                    id,
                    6,
                    Duration.ofDays(7));
        }
    }

    @Override
    @Transactional
    public void updateProductStatus(Long id, Integer status) {
        ensureAuthenticated();
        SecondHandProduct product = requireProduct(id);
        ensureOwner(product.getSellerId());
        if (!isStatus(product.getStatus(), SecondHandConstant.PRODUCT_ON_SALE) &&
                !isStatus(product.getStatus(), SecondHandConstant.PRODUCT_OFF_SHELF)) {
            throw new SecondHandException("交易中的商品不可上下架");
        }
        if (!isStatus(status, SecondHandConstant.PRODUCT_ON_SALE) &&
                !isStatus(status, SecondHandConstant.PRODUCT_OFF_SHELF)) {
            throw new SecondHandException("只能上架或下架自己的商品");
        }
        productMapper.updateStatus(id, status);
    }

    @Override
    public void adminUpdateProductStatus(Long id, Integer status) {
        validateProductStatus(status);
        requireProduct(id);
        productMapper.updateStatus(id, status);
    }

    @Override
    public List<SecondHandProductVO> listProducts(SecondHandProductQueryDTO query) {
        Long schoolId = userMapper.getSchoolId(BaseContext.getCurrentId());
        if (query == null) {
            query = new SecondHandProductQueryDTO();
        }
        if (query.getStatus() == null) {
            query.setStatus(SecondHandConstant.PRODUCT_ON_SALE);
        }
        List<SecondHandProductVO> products = productMapper.list(query, schoolId);
        products.forEach(this::resolveProductImages);
        return products;
    }

    @Override
    public List<SecondHandProductVO> listMyProducts() {
        ensureAuthenticated();
        List<SecondHandProductVO> products =
                productMapper.listBySeller(BaseContext.getCurrentId());
        products.forEach(this::resolveProductImages);
        return products;
    }

    @Override
    public SecondHandProductVO productDetail(Long id) {
        productMapper.increaseViewCount(id);
        SecondHandProductVO detail = productMapper.detail(id);
        if (detail == null) {
            throw new SecondHandException("商品不存在");
        }
        return resolveProductImages(detail);
    }

    @Override
    @Transactional
    public SecondHandBargainVO createBargain(SecondHandBargainDTO dto) {
        ensureAuthenticated();
        SecondHandProduct product = requireProduct(dto.getProductId());
        Long buyerId = BaseContext.getCurrentId();
        if (!isSelfTradeAllowed() && product.getSellerId().equals(buyerId)) {
            throw new SecondHandException("不能向自己的商品议价");
        }
        if (!isStatus(product.getStatus(), SecondHandConstant.PRODUCT_ON_SALE)) {
            throw new SecondHandException("商品当前不可议价");
        }
        if (!Integer.valueOf(1).equals(product.getNegotiable())) {
            throw new SecondHandException("卖家未开启议价");
        }
        if (dto.getOfferPrice() == null || dto.getOfferPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ParamException("议价金额不正确");
        }
        int count = bargainMapper.countByBuyerAndProduct(buyerId, product.getId());
        int maxCount = getIntConfig("second_hand_bargain_max_count", SecondHandConstant.DEFAULT_BARGAIN_MAX_COUNT);
        if (count >= maxCount) {
            throw new SecondHandException("议价次数已用完");
        }
        LocalDateTime now = LocalDateTime.now();
        SecondHandBargain bargain = SecondHandBargain.builder()
                .productId(product.getId())
                .buyerId(buyerId)
                .sellerId(product.getSellerId())
                .offerPrice(dto.getOfferPrice())
                .message(dto.getMessage())
                .status(SecondHandConstant.BARGAIN_PENDING)
                .attemptNo(count + 1)
                .deleted(DeleteConstant.UN_DELETED)
                .createTime(now)
                .updateTime(now)
                .build();
        bargainMapper.insert(bargain);
        return bargainMapper.listByProduct(product.getId()).stream()
                .filter(item -> item.getId().equals(bargain.getId()))
                .findFirst()
                .orElseThrow(() -> new SecondHandException("议价创建失败"));
    }

    @Override
    @Transactional
    public SecondHandOrderVO acceptBargain(Long bargainId, SecondHandOrderCreateDTO dto) {
        ensureAuthenticated();
        SecondHandBargain bargain = requireBargain(bargainId);
        ensureOwner(bargain.getSellerId());
        if (!isStatus(bargain.getStatus(), SecondHandConstant.BARGAIN_PENDING)) {
            throw new SecondHandException("议价状态不可接受");
        }
        SecondHandProduct product = requireProduct(bargain.getProductId());
        SecondHandOrderCreateDTO orderDTO = dto == null ? new SecondHandOrderCreateDTO() : dto;
        orderDTO.setProductId(product.getId());
        orderDTO.setBargainId(bargain.getId());
        SecondHandOrderVO order = createOrderInternal(product, bargain.getBuyerId(), bargain.getOfferPrice(), orderDTO);
        bargain.setStatus(SecondHandConstant.BARGAIN_ACCEPTED);
        bargain.setUpdateTime(LocalDateTime.now());
        bargainMapper.update(bargain);
        bargainMapper.expirePendingByProduct(product.getId());
        return order;
    }

    @Override
    public void rejectBargain(Long bargainId) {
        ensureAuthenticated();
        SecondHandBargain bargain = requireBargain(bargainId);
        ensureOwner(bargain.getSellerId());
        bargain.setStatus(SecondHandConstant.BARGAIN_REJECTED);
        bargain.setUpdateTime(LocalDateTime.now());
        bargainMapper.update(bargain);
    }

    @Override
    public List<SecondHandBargainVO> listMyBargains() {
        ensureAuthenticated();
        return bargainMapper.listMine(BaseContext.getCurrentId());
    }

    @Override
    public List<SecondHandBargainVO> listProductBargains(Long productId) {
        ensureAuthenticated();
        SecondHandProduct product = requireProduct(productId);
        Long userId = BaseContext.getCurrentId();
        if (!product.getSellerId().equals(userId)) {
            return bargainMapper.listByProduct(productId).stream()
                    .filter(item -> item.getBuyerId().equals(userId))
                    .toList();
        }
        return bargainMapper.listByProduct(productId);
    }

    @Override
    @Transactional
    public SecondHandOrderVO createOrder(SecondHandOrderCreateDTO dto) {
        ensureAuthenticated();
        SecondHandProduct product = requireProduct(dto.getProductId());
        return createOrderInternal(product, BaseContext.getCurrentId(), product.getPrice(), dto);
    }

    @Override
    @Transactional
    public List<SecondHandOrderVO> listBuyerOrders() {
        ensureAuthenticated();
        refreshUnpaidTimeouts();
        return enrichOrders(orderMapper.listByBuyer(BaseContext.getCurrentId()));
    }

    @Override
    @Transactional
    public List<SecondHandOrderVO> listSellerOrders() {
        ensureAuthenticated();
        refreshUnpaidTimeouts();
        return enrichOrders(orderMapper.listBySeller(BaseContext.getCurrentId()));
    }

    @Override
    @Transactional
    public SecondHandOrderVO orderDetail(Long id) {
        ensureAuthenticated();
        refreshUnpaidTimeouts();
        SecondHandOrder order = requireOrder(id);
        Long userId = BaseContext.getCurrentId();
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            throw new SecondHandException("无权查看该订单");
        }
        return enrichOrder(orderMapper.detail(id));
    }

    @Override
    @Transactional
    public void markPaid(String orderNumber) {
        SecondHandOrder order = orderMapper.getByOrderNumber(orderNumber);
        if (order == null || !isStatus(order.getStatus(), SecondHandConstant.ORDER_PENDING_PAY)) {
            return;
        }
        order.setStatus(SecondHandConstant.ORDER_PAID_WAIT_DELIVERY);
        order.setPayTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.update(order);
        productMapper.markTrading(order.getProductId());
    }

    @Override
    @Transactional
    public void cancelOrder(Long id, String reason) {
        ensureAuthenticated();
        refreshUnpaidTimeouts();
        SecondHandOrder order = requireOrder(id);
        if (!order.getBuyerId().equals(BaseContext.getCurrentId())) {
            throw new SecondHandException("只有买家可以取消订单");
        }
        if (isStatus(order.getStatus(), SecondHandConstant.ORDER_CANCELED) ||
                isStatus(order.getStatus(), SecondHandConstant.ORDER_REFUNDING) ||
                isStatus(order.getStatus(), SecondHandConstant.ORDER_REFUND_SUCCESS) ||
                isStatus(order.getStatus(), SecondHandConstant.ORDER_REFUND_ABNORMAL)) {
            return;
        }
        if (isStatus(order.getStatus(), SecondHandConstant.ORDER_PENDING_PAY)) {
            try {
                weChatPayUtil.closeOrder(order.getOrderNumber());
            } catch (Exception e) {
                log.warn("关闭二手待支付微信订单失败, orderNumber={}", order.getOrderNumber(), e);
            }
            order.setStatus(SecondHandConstant.ORDER_CANCELED);
            order.setCancelReason(reason);
            order.setCancelTime(LocalDateTime.now());
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.update(order);
            productMapper.releaseLockedProduct(order.getProductId());
            return;
        }
        if (isStatus(order.getStatus(), SecondHandConstant.ORDER_PAID_WAIT_DELIVERY)) {
            order.setStatus(SecondHandConstant.ORDER_REFUNDING);
            order.setCancelReason(reason == null ? "买家取消订单" : reason);
            order.setCancelTime(LocalDateTime.now());
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.update(order);
            if (Boolean.TRUE.equals(mockPaymentEnabled)) {
                order.setStatus(SecondHandConstant.ORDER_REFUND_SUCCESS);
                order.setUpdateTime(LocalDateTime.now());
                orderMapper.update(order);
                productMapper.releaseRefundedProduct(order.getProductId());
                return;
            }
            requestRefund(order);
            return;
        }
        throw new SecondHandException("当前订单状态不可取消，请发起申诉");
    }

    @Override
    @Transactional
    public void markDelivered(Long id) {
        ensureAuthenticated();
        refreshUnpaidTimeouts();
        SecondHandOrder order = requireOrder(id);
        ensureOwner(order.getSellerId());
        if (isStatus(order.getStatus(), SecondHandConstant.ORDER_DELIVERED_WAIT_CONFIRM) ||
                isStatus(order.getStatus(), SecondHandConstant.ORDER_TRANSFERING) ||
                isStatus(order.getStatus(), SecondHandConstant.ORDER_TRANSFER_SUCCESS) ||
                isStatus(order.getStatus(), SecondHandConstant.ORDER_TRANSFER_FAILED) ||
                isStatus(order.getStatus(), SecondHandConstant.ORDER_COMPLETED)) {
            return;
        }
        if (!isStatus(order.getStatus(), SecondHandConstant.ORDER_PAID_WAIT_DELIVERY)) {
            throw new SecondHandException("订单未付款或不可交付");
        }
        LocalDateTime now = LocalDateTime.now();
        int hours = getIntConfig("second_hand_auto_confirm_hours", SecondHandConstant.DEFAULT_AUTO_CONFIRM_HOURS);
        order.setStatus(SecondHandConstant.ORDER_DELIVERED_WAIT_CONFIRM);
        order.setDeliveredTime(now);
        order.setConfirmDeadline(now.plusHours(hours));
        order.setUpdateTime(now);
        orderMapper.update(order);
    }

    @Override
    @Transactional
    public void confirmOrder(Long id) {
        ensureAuthenticated();
        refreshUnpaidTimeouts();
        SecondHandOrder order = requireOrder(id);
        if (!order.getBuyerId().equals(BaseContext.getCurrentId())) {
            throw new SecondHandException("只有买家可以确认收货");
        }
        if (isStatus(order.getStatus(), SecondHandConstant.ORDER_TRANSFERING) ||
                isStatus(order.getStatus(), SecondHandConstant.ORDER_TRANSFER_SUCCESS) ||
                isStatus(order.getStatus(), SecondHandConstant.ORDER_TRANSFER_FAILED) ||
                isStatus(order.getStatus(), SecondHandConstant.ORDER_COMPLETED)) {
            return;
        }
        completeOrder(order);
    }

    @Override
    @Transactional
    public SecondHandMessageVO createMessage(SecondHandMessageDTO dto) {
        ensureAuthenticated();
        if (dto.getContent() == null || dto.getContent().isBlank()) {
            throw new ParamException("留言内容不能为空");
        }
        SecondHandProduct product = requireProduct(dto.getProductId());
        Long senderId = BaseContext.getCurrentId();
        Long receiverId = dto.getReceiverId();
        SecondHandOrder order = null;
        if (dto.getOrderId() != null) {
            order = requireOrder(dto.getOrderId());
            if (!order.getProductId().equals(product.getId())) {
                throw new SecondHandException("订单与商品不匹配");
            }
            boolean senderInOrder = order.getBuyerId().equals(senderId) || order.getSellerId().equals(senderId);
            boolean receiverInOrder = receiverId != null &&
                    (order.getBuyerId().equals(receiverId) || order.getSellerId().equals(receiverId));
            if (!senderInOrder || !receiverInOrder) {
                throw new SecondHandException("无权在该订单留言");
            }
        }
        if (receiverId == null) {
            receiverId = product.getSellerId().equals(senderId) ? null : product.getSellerId();
        }
        if (receiverId == null || receiverId.equals(senderId)) {
            throw new SecondHandException("请选择正确的留言对象");
        }
        if (!product.getSellerId().equals(senderId) && !product.getSellerId().equals(receiverId)) {
            throw new SecondHandException("只能向卖家留言");
        }
        if (product.getSellerId().equals(senderId) &&
                messageMapper.countProductParticipant(product.getId(), senderId, receiverId) <= 0) {
            throw new SecondHandException("只能回复已有互动的买家");
        }
        SecondHandMessage message = SecondHandMessage.builder()
                .productId(dto.getProductId())
                .orderId(dto.getOrderId())
                .senderId(senderId)
                .receiverId(receiverId)
                .content(dto.getContent())
                .deleted(DeleteConstant.UN_DELETED)
                .createTime(LocalDateTime.now())
                .build();
        messageMapper.insert(message);
        return messageMapper.listByProductForUser(dto.getProductId(), senderId).stream()
                .filter(item -> item.getId().equals(message.getId()))
                .findFirst()
                .orElseThrow(() -> new SecondHandException("留言创建失败"));
    }

    @Override
    public List<SecondHandMessageVO> listProductMessages(Long productId) {
        ensureAuthenticated();
        return messageMapper.listByProductForUser(productId, BaseContext.getCurrentId());
    }

    @Override
    public List<SecondHandProductVO> adminListProducts(SecondHandProductQueryDTO query) {
        if (query == null) {
            query = new SecondHandProductQueryDTO();
        }
        List<SecondHandProductVO> products = productMapper.list(query, null);
        products.forEach(this::resolveProductImages);
        return products;
    }

    @Override
    public SecondHandProductVO adminProductDetail(Long id) {
        SecondHandProductVO detail = productMapper.detail(id);
        if (detail == null) {
            throw new SecondHandException("商品不存在");
        }
        return resolveProductImages(detail);
    }

    @Override
    @Transactional
    public void adminDeleteProduct(Long id) {
        SecondHandProduct product = requireProduct(id);
        SecondHandOrder activeOrder = orderMapper.getActiveByProductId(id);
        if (activeOrder != null) {
            throw new SecondHandException("商品存在进行中的订单，暂不可删除");
        }
        SecondHandProduct update = new SecondHandProduct();
        update.setId(product.getId());
        update.setStatus(SecondHandConstant.PRODUCT_OFF_SHELF);
        update.setDeleted(DeleteConstant.DELETED);
        update.setUpdateTime(LocalDateTime.now());
        productMapper.update(update);
    }

    @Override
    public List<SecondHandOrderVO> adminListOrders(Integer status) {
        return enrichOrders(orderMapper.listAdmin(status));
    }

    @Override
    public SecondHandOrderVO adminOrderDetail(Long id) {
        SecondHandOrderVO detail = orderMapper.detail(id);
        if (detail == null) {
            throw new SecondHandException("二手订单不存在");
        }
        return enrichOrder(detail);
    }

    @Override
    @Transactional
    public void adminUpdateOrderStatus(Long id, SecondHandStatusDTO dto) {
        if (dto == null || dto.getStatus() == null) {
            throw new ParamException(MessageConstant.NOT_FOUND_PARAM);
        }
        validateOrderStatus(dto.getStatus());
        SecondHandOrder order = requireOrder(id);
        Integer status = dto.getStatus();
        String reason = trimToNull(dto.getReason());
        LocalDateTime now = LocalDateTime.now();
        order.setStatus(status);
        order.setUpdateTime(now);

        if (isStatus(status, SecondHandConstant.ORDER_CANCELED)) {
            order.setCancelTime(now);
            order.setCancelReason(reason == null ? "管理员关闭订单" : reason);
            productMapper.releaseLockedProduct(order.getProductId());
        } else if (isStatus(status, SecondHandConstant.ORDER_REFUNDING) ||
                isStatus(status, SecondHandConstant.ORDER_REFUND_ABNORMAL)) {
            order.setCancelTime(now);
            order.setCancelReason(reason == null ? "管理员处理退款" : reason);
        } else if (isStatus(status, SecondHandConstant.ORDER_REFUND_SUCCESS)) {
            order.setCancelTime(now);
            order.setCancelReason(reason == null ? "管理员确认退款完成" : reason);
            productMapper.releaseRefundedProduct(order.getProductId());
        } else if (isStatus(status, SecondHandConstant.ORDER_PAID_WAIT_DELIVERY) ||
                isStatus(status, SecondHandConstant.ORDER_DELIVERED_WAIT_CONFIRM)) {
            productMapper.markTrading(order.getProductId());
        } else if (isStatus(status, SecondHandConstant.ORDER_COMPLETED) ||
                isStatus(status, SecondHandConstant.ORDER_TRANSFERING) ||
                isStatus(status, SecondHandConstant.ORDER_TRANSFER_SUCCESS) ||
                isStatus(status, SecondHandConstant.ORDER_TRANSFER_FAILED)) {
            order.setFinishTime(order.getFinishTime() == null ? now : order.getFinishTime());
            productMapper.markSold(order.getProductId());
            if (isStatus(status, SecondHandConstant.ORDER_TRANSFER_SUCCESS)) {
                order.setTransferTime(now);
            }
            if (isStatus(status, SecondHandConstant.ORDER_TRANSFER_FAILED)) {
                order.setTransferFailReason(reason == null ? "管理员标记收款异常" : reason);
            }
        } else if (isStatus(status, SecondHandConstant.ORDER_DISPUTE)) {
            order.setCancelReason(reason == null ? "管理员标记协商中" : reason);
        }
        orderMapper.update(order);
    }

    @Override
    @Transactional
    public void adminRetryTransfer(Long id) {
        SecondHandOrder order = requireOrder(id);
        if (!isStatus(order.getStatus(), SecondHandConstant.ORDER_TRANSFER_FAILED) &&
                !isStatus(order.getStatus(), SecondHandConstant.ORDER_TRANSFERING)) {
            throw new SecondHandException("当前订单状态不可重试收款");
        }
        order.setStatus(SecondHandConstant.ORDER_TRANSFERING);
        order.setTransferFailReason("");
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.update(order);
        if (Boolean.TRUE.equals(mockPaymentEnabled)) {
            order.setStatus(SecondHandConstant.ORDER_TRANSFER_SUCCESS);
            order.setTransferTime(LocalDateTime.now());
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.update(order);
            return;
        }
        requestSellerTransfer(order);
    }

    @Override
    public List<SecondHandBargainVO> adminListBargains() {
        return bargainMapper.listAdmin();
    }

    @Override
    @Transactional
    public void adminUpdateBargainStatus(Long id, SecondHandStatusDTO dto) {
        if (dto == null || dto.getStatus() == null) {
            throw new ParamException(MessageConstant.NOT_FOUND_PARAM);
        }
        validateBargainStatus(dto.getStatus());
        SecondHandBargain bargain = requireBargain(id);
        bargain.setStatus(dto.getStatus());
        bargain.setUpdateTime(LocalDateTime.now());
        bargainMapper.update(bargain);
    }

    @Override
    public List<SecondHandMessageVO> adminListMessages(Long productId) {
        return messageMapper.listAdmin(productId);
    }

    @Override
    @Transactional
    public void processUnpaidTimeouts() {
        refreshUnpaidTimeouts();
    }

    private void refreshUnpaidTimeouts() {
        int minutes = getIntConfig("second_hand_payment_timeout_minutes", SecondHandConstant.DEFAULT_PAYMENT_TIMEOUT_MINUTES);
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(minutes);
        for (SecondHandOrder order : orderMapper.listUnpaidTimeout(cutoff)) {
            order.setStatus(SecondHandConstant.ORDER_CANCELED);
            order.setCancelReason("支付超时自动关闭");
            order.setCancelTime(LocalDateTime.now());
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.update(order);
            productMapper.releaseLockedProduct(order.getProductId());
        }
    }

    @Override
    @Transactional
    public void processAutoConfirm() {
        for (SecondHandOrder order : orderMapper.listAutoConfirm(LocalDateTime.now())) {
            completeOrder(order);
        }
    }

    @Override
    @Transactional
    public WeChatPrePayVO jsapiPay(Long orderId) throws Exception {
        ensureAuthenticated();
        refreshUnpaidTimeouts();
        SecondHandOrder order = requireOrder(orderId);
        if (!order.getBuyerId().equals(BaseContext.getCurrentId())) {
            throw new SecondHandException("只能支付自己的订单");
        }
        if (isStatus(order.getStatus(), SecondHandConstant.ORDER_CANCELED)) {
            throw new SecondHandException("订单已超时关闭，请重新购买");
        }
        if (!isStatus(order.getStatus(), SecondHandConstant.ORDER_PENDING_PAY)) {
            throw new SecondHandException("订单状态已更新，请刷新后查看");
        }

        String wxPayUrl = weChatProperties.getWxDomain().concat(WeChatPayConstant.JSAPI_PAY);
        String notifyPayUrl = weChatProperties.getNotifyUrl().concat("/api/second-hand/pay/notify");
        HttpPost httpPost = new HttpPost(wxPayUrl);

        Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put("appid", weChatProperties.getAppid());
        paramsMap.put("mchid", weChatProperties.getMchid());
        paramsMap.put("description", "校园二手-" + order.getOrderNumber());
        paramsMap.put("out_trade_no", order.getOrderNumber());
        paramsMap.put("notify_url", notifyPayUrl);

        Map<String, Object> amountMap = new HashMap<>();
        amountMap.put("total", order.getPayAmount().multiply(BigDecimal.valueOf(100)).intValue());
        amountMap.put("currency", "CNY");
        paramsMap.put("amount", amountMap);

        Map<String, String> payer = new HashMap<>();
        payer.put("openid", userMapper.getOpenidById(BaseContext.getCurrentId()));
        paramsMap.put("payer", payer);

        StringEntity entity = new StringEntity(JSONObject.toJSONString(paramsMap), "utf-8");
        entity.setContentType("application/json");
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");

        CloseableHttpResponse response = wxPayClient.execute(httpPost);
        try {
            String bodyAsString = EntityUtils.toString(response.getEntity());
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200 && statusCode != 204) {
                throw new IOException("second hand pay request failed " + bodyAsString);
            }
            Map<String, String> resultMap = JSONObject.parseObject(bodyAsString, HashMap.class);
            String prepayId = resultMap.get("prepay_id");
            long timeStamp = System.currentTimeMillis();
            String nonceStr = weChatPayUtil.getNonceStr();
            String pack = "prepay_id=" + prepayId;
            String paySign = weChatPayUtil.getSign(weChatProperties.getAppid(), timeStamp, nonceStr, pack);
            return WeChatPrePayVO.builder()
                    .timeStamp(String.valueOf(timeStamp))
                    .nonceStr(nonceStr)
                    .signType("RSA")
                    .paySign(paySign)
                    .prepayId(prepayId)
                    .build();
        } finally {
            response.close();
        }
    }

    @Override
    @Transactional
    public void processPayNotify(Map<String, Object> bodyMap) throws GeneralSecurityException {
        String plainText = decryptFromResource(bodyMap);
        Map plainTextMap = JSONObject.parseObject(plainText, HashMap.class);
        String orderNumber = (String) plainTextMap.get(WeChatPayConstant.OUT_TRADE_NO);
        if (!payNotifyLock.tryLock()) {
            return;
        }
        try {
            SecondHandOrder order = orderMapper.getByOrderNumber(orderNumber);
            if (order == null || !isStatus(order.getStatus(), SecondHandConstant.ORDER_PENDING_PAY)) {
                return;
            }
            markPaid(orderNumber);
            savePaymentLog(plainText, order);
        } finally {
            payNotifyLock.unlock();
        }
    }

    private SecondHandOrderVO createOrderInternal(SecondHandProduct product, Long buyerId, BigDecimal amount, SecondHandOrderCreateDTO dto) {
        if (dto == null) {
            dto = new SecondHandOrderCreateDTO();
        }
        if (!isSelfTradeAllowed() && product.getSellerId().equals(buyerId)) {
            throw new SecondHandException("不能购买自己的商品");
        }
        Integer deliveryMode = dto.getDeliveryMode() == null ? 0 : dto.getDeliveryMode();
        if (deliveryMode != 0 && deliveryMode != 1) {
            throw new ParamException("交付方式不正确");
        }
        if (deliveryMode == 1 && Integer.valueOf(1).equals(product.getPickupOnly())) {
            throw new SecondHandException("该商品仅支持自提");
        }
        String pickupSnapshot = trimToNull(product.getPickupAddressSnapshot());
        if (pickupSnapshot == null) {
            throw new SecondHandException("商品自提地址缺失");
        }
        String buyerDeliverySnapshot = trimToNull(dto.getBuyerDeliveryAddressSnapshot());
        if (deliveryMode == 1 && buyerDeliverySnapshot == null) {
            throw new ParamException("请选择配送地址");
        }
        if (!isStatus(product.getStatus(), SecondHandConstant.PRODUCT_ON_SALE)) {
            throw new SecondHandException("商品当前不可购买");
        }
        if (orderMapper.getActiveByProductId(product.getId()) != null) {
            throw new SecondHandException("商品已有进行中的订单");
        }
        if (productMapper.lockOnSaleProduct(product.getId()) == 0) {
            throw new SecondHandException("商品已被锁定或售出");
        }
        BigDecimal feeRate = getDecimalConfig("second_hand_service_fee_rate", new BigDecimal(SecondHandConstant.DEFAULT_SERVICE_FEE_RATE));
        BigDecimal serviceFee = amount.multiply(feeRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal sellerIncome = amount.subtract(serviceFee).setScale(2, RoundingMode.HALF_UP);
        LocalDateTime now = LocalDateTime.now();
        SecondHandOrder order = SecondHandOrder.builder()
                .orderNumber("SH" + System.currentTimeMillis())
                .productId(product.getId())
                .bargainId(dto.getBargainId())
                .buyerId(buyerId)
                .sellerId(product.getSellerId())
                .productAmount(amount)
                .payAmount(amount)
                .serviceFeeRate(feeRate)
                .serviceFee(serviceFee)
                .sellerIncome(sellerIncome)
                .deliveryMode(deliveryMode)
                .pickupAddressSnapshot(pickupSnapshot)
                .buyerDeliveryAddressId(deliveryMode == 1 ? dto.getBuyerDeliveryAddressId() : null)
                .buyerDeliveryAddressSnapshot(deliveryMode == 1 ? buyerDeliverySnapshot : null)
                .deliveryRemark(firstNotBlank(dto.getDeliveryRemark(), deliveryMode == 1 ? buyerDeliverySnapshot : pickupSnapshot))
                .status(SecondHandConstant.ORDER_PENDING_PAY)
                .deleted(DeleteConstant.UN_DELETED)
                .createTime(now)
                .updateTime(now)
                .build();
        orderMapper.insert(order);
        return orderMapper.detail(order.getId());
    }

    private void completeOrder(SecondHandOrder order) {
        if (!isStatus(order.getStatus(), SecondHandConstant.ORDER_DELIVERED_WAIT_CONFIRM)) {
            throw new SecondHandException("订单不可确认收货");
        }
        LocalDateTime now = LocalDateTime.now();
        order.setStatus(SecondHandConstant.ORDER_TRANSFERING);
        order.setFinishTime(now);
        order.setUpdateTime(now);
        orderMapper.update(order);
        productMapper.markSold(order.getProductId());

        if (Boolean.TRUE.equals(mockPaymentEnabled)) {
            log.info("开发模拟支付已开启，跳过二手订单真实微信转账请求, orderNumber={}", order.getOrderNumber());
            return;
        }
        requestSellerTransfer(order);
    }

    private void savePaymentLog(String plainText, SecondHandOrder order) {
        Map map = JSONObject.parseObject(plainText, HashMap.class);
        Map<String, String> payer = (Map) map.get(WeChatPayConstant.PAYER);
        Map<String, Object> amount = (Map<String, Object>) map.get(WeChatPayConstant.AMOUNT);
        Integer total = (Integer) amount.get(WeChatPayConstant.TOTAL);
        PaymentLog paymentLog = PaymentLog.builder()
                .orderNumber(order.getOrderNumber())
                .paymentType(WeChatPayConstant.PAYMENT_TYPE)
                .transactionId((String) map.get(WeChatPayConstant.TRANSACTION_ID))
                .tradeType((String) map.get(WeChatPayConstant.TRADE_TYPE))
                .tradeState((String) map.get(WeChatPayConstant.TRADE_STATE))
                .bankType((String) map.get(WeChatPayConstant.BANK_TYPE))
                .successTime((String) map.get(WeChatPayConstant.SUCCESS_TIME))
                .payerOpenid(payer == null ? null : payer.get(WeChatPayConstant.OPENID))
                .total(total == null ? order.getPayAmount().multiply(BigDecimal.valueOf(100)).longValue() : total.longValue())
                .serviceFeeRate(order.getServiceFeeRate())
                .serviceFee(order.getServiceFee().multiply(BigDecimal.valueOf(100)).longValue())
                .content(plainText)
                .deleted(DeleteConstant.UN_DELETED)
                .build();
        paymentLogMapper.insert(paymentLog);
    }

    private void requestSellerTransfer(SecondHandOrder order) {
        try {
            String transferUrl = weChatProperties.getWxDomain().concat(WeChatTransferConstant.WX_TRANSFER);
            String notifyUrl = weChatProperties.getNotifyUrl().concat("/api/second-hand/transfer/notify");
            String sellerOpenid = userMapper.getOpenidById(order.getSellerId());
            int transferAmount = order.getSellerIncome().multiply(BigDecimal.valueOf(100)).intValue();

            Map<String, Object> paramsMap = new HashMap<>();
            paramsMap.put("appid", weChatProperties.getAppid());
            paramsMap.put("out_bill_no", order.getOrderNumber());
            paramsMap.put("transfer_scene_id", weChatProperties.getTransferSceneId());
            paramsMap.put("openid", sellerOpenid);
            paramsMap.put("transfer_amount", transferAmount);
            paramsMap.put("transfer_remark", "校园二手交易结算");
            paramsMap.put("notify_url", notifyUrl);

            HashMap<String, String>[] sceneReportInfos = new HashMap[2];
            HashMap<String, String> info = new HashMap<>();
            info.put("info_type", "Transaction Type");
            info.put("info_content", "Campus Used Goods");
            sceneReportInfos[0] = info;
            info = new HashMap<>();
            info.put("info_type", "Settlement Description");
            info.put("info_content", "Second-hand goods seller settlement");
            sceneReportInfos[1] = info;
            paramsMap.put("transfer_scene_report_infos", sceneReportInfos);

            HttpPost httpPost = new HttpPost(transferUrl);
            StringEntity entity = new StringEntity(JSONObject.toJSONString(paramsMap), "utf-8");
            entity.setContentType("application/json");
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");

            CloseableHttpResponse response = wxPayClient.execute(httpPost);
            try {
                String bodyAsString = EntityUtils.toString(response.getEntity());
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200 && statusCode != 204) {
                    markTransferFailed(order, "微信转账请求失败: " + bodyAsString);
                    saveTransferLog(order, sellerOpenid, transferAmount, "FAIL", null, bodyAsString);
                    return;
                }
                Map<String, String> resultMap = JSONObject.parseObject(bodyAsString, HashMap.class);
                String state = resultMap.get(WeChatTransferConstant.STATE);
                String transferBillNo = resultMap.get(WeChatTransferConstant.TRANSFER_BILL_NO);
                saveTransferLog(order, sellerOpenid, transferAmount, state, transferBillNo, bodyAsString);
                if (WeChatTransferConstant.SUCCESS_TRAD.equals(state)) {
                    order.setStatus(SecondHandConstant.ORDER_TRANSFER_SUCCESS);
                    order.setTransferTime(LocalDateTime.now());
                    order.setUpdateTime(LocalDateTime.now());
                    orderMapper.update(order);
                } else if (WeChatTransferConstant.FAIL_TRAD.equals(state)) {
                    markTransferFailed(order, resultMap.get("fail_reason"));
                }
            } finally {
                response.close();
            }
        } catch (Exception e) {
            log.error("二手订单转账失败, orderNumber={}", order.getOrderNumber(), e);
            markTransferFailed(order, e.getMessage());
        }
    }

    private void markTransferFailed(SecondHandOrder order, String reason) {
        order.setStatus(SecondHandConstant.ORDER_TRANSFER_FAILED);
        order.setTransferFailReason(reason);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.update(order);
    }

    private void saveTransferLog(SecondHandOrder order, String openid, int amount, String state, String billNo, String content) {
        WxTransferLog log = WxTransferLog.builder()
                .orderNumber(order.getOrderNumber())
                .transferBillNo(billNo)
                .state(state)
                .mchId(weChatProperties.getMchid())
                .transferAmount(amount)
                .openid(openid)
                .content(content)
                .createTime(LocalDateTime.now().toString())
                .updateTime(LocalDateTime.now().toString())
                .deleted(DeleteConstant.UN_DELETED)
                .build();
        wxTransferLogMapper.insert(log);
    }

    private void requestRefund(SecondHandOrder order) {
        LocalDateTime now = LocalDateTime.now();
        String refundNumber = "SH_REFUND_" + System.currentTimeMillis();
        int totalFee = order.getPayAmount().multiply(BigDecimal.valueOf(100)).intValue();
        RefundInfo refundInfo = RefundInfo.builder()
                .orderNumber(order.getOrderNumber())
                .refundNumber(refundNumber)
                .totalFee(totalFee)
                .refund(totalFee)
                .reason(order.getCancelReason())
                .refundStatus("PROCESSING")
                .createTime(now)
                .updateTime(now)
                .build();
        refundInfoMapper.insert(refundInfo);

        try {
            String refundUrl = weChatProperties.getWxDomain().concat(WeChatPayConstant.REFUNDS_URL);
            Map<String, Object> paramsMap = new HashMap<>();
            paramsMap.put("out_trade_no", order.getOrderNumber());
            paramsMap.put("out_refund_no", refundNumber);
            paramsMap.put("reason", order.getCancelReason());
            paramsMap.put("notify_url", weChatProperties.getNotifyUrl().concat("/api/second-hand/refunds/notify"));

            Map<String, Object> amount = new HashMap<>();
            amount.put("refund", totalFee);
            amount.put("total", totalFee);
            amount.put("currency", "CNY");
            paramsMap.put("amount", amount);

            HttpPost httpPost = new HttpPost(refundUrl);
            StringEntity entity = new StringEntity(JSONObject.toJSONString(paramsMap), "utf-8");
            entity.setContentType("application/json");
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            CloseableHttpResponse response = wxPayClient.execute(httpPost);
            try {
                String bodyAsString = EntityUtils.toString(response.getEntity());
                int statusCode = response.getStatusLine().getStatusCode();
                RefundInfo update = RefundInfo.builder()
                        .refundNumber(refundNumber)
                        .updateTime(LocalDateTime.now())
                        .contentReturn(bodyAsString)
                        .build();
                if (statusCode != 200 && statusCode != 204) {
                    order.setStatus(SecondHandConstant.ORDER_REFUND_ABNORMAL);
                    order.setUpdateTime(LocalDateTime.now());
                    orderMapper.update(order);
                    update.setRefundStatus("ABNORMAL");
                }
                refundInfoMapper.update(update);
            } finally {
                response.close();
            }
        } catch (Exception e) {
            log.error("二手订单退款失败, orderNumber={}", order.getOrderNumber(), e);
            order.setStatus(SecondHandConstant.ORDER_REFUND_ABNORMAL);
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.update(order);
            RefundInfo update = RefundInfo.builder()
                    .refundNumber(refundNumber)
                    .refundStatus("ABNORMAL")
                    .contentReturn(e.getMessage())
                    .updateTime(LocalDateTime.now())
                    .build();
            refundInfoMapper.update(update);
        }
    }

    @Override
    @Transactional
    public void processRefundNotify(Map<String, Object> bodyMap) throws Exception {
        String plainText = decryptFromResource(bodyMap);
        Map plainTextMap = JSONObject.parseObject(plainText, HashMap.class);
        String orderNumber = (String) plainTextMap.get(WeChatPayConstant.OUT_TRADE_NO);
        if (!refundNotifyLock.tryLock()) {
            return;
        }
        try {
            SecondHandOrder order = orderMapper.getByOrderNumber(orderNumber);
            if (order == null || !isStatus(order.getStatus(), SecondHandConstant.ORDER_REFUNDING)) {
                return;
            }
            String refundStatus = (String) plainTextMap.get("refund_status");
            String refundNumber = (String) plainTextMap.get("out_refund_no");
            if ("SUCCESS".equals(refundStatus)) {
                order.setStatus(SecondHandConstant.ORDER_REFUND_SUCCESS);
                productMapper.releaseRefundedProduct(order.getProductId());
            } else {
                order.setStatus(SecondHandConstant.ORDER_REFUND_ABNORMAL);
            }
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.update(order);
            RefundInfo update = RefundInfo.builder()
                    .refundNumber(refundNumber)
                    .refundStatus(refundStatus)
                    .refundId((String) plainTextMap.get("refund_id"))
                    .contentNotify(plainText)
                    .updateTime(LocalDateTime.now())
                    .build();
            refundInfoMapper.update(update);
        } finally {
            refundNotifyLock.unlock();
        }
    }

    @Override
    @Transactional
    public void processTransferNotify(Map<String, Object> bodyMap) throws GeneralSecurityException {
        String plainText = decryptFromResource(bodyMap);
        Map plainTextMap = JSONObject.parseObject(plainText, HashMap.class);
        String orderNumber = (String) plainTextMap.get(WeChatTransferConstant.OUT_BILL_NO);
        if (!transferNotifyLock.tryLock()) {
            return;
        }
        try {
            SecondHandOrder order = orderMapper.getByOrderNumber(orderNumber);
            if (order == null || !isStatus(order.getStatus(), SecondHandConstant.ORDER_TRANSFERING)) {
                return;
            }
            String state = (String) plainTextMap.get(WeChatTransferConstant.STATE);
            if (WeChatTransferConstant.SUCCESS_TRAD.equals(state)) {
                order.setStatus(SecondHandConstant.ORDER_TRANSFER_SUCCESS);
                order.setTransferTime(LocalDateTime.now());
                order.setUpdateTime(LocalDateTime.now());
                orderMapper.update(order);
            } else if (WeChatTransferConstant.FAIL_TRAD.equals(state)) {
                markTransferFailed(order, (String) plainTextMap.get("fail_reason"));
            }
            WxTransferLog log = WxTransferLog.builder()
                    .orderNumber(orderNumber)
                    .transferBillNo((String) plainTextMap.get(WeChatTransferConstant.TRANSFER_BILL_NO))
                    .state(state)
                    .mchId((String) plainTextMap.get(WeChatTransferConstant.MCH_ID))
                    .content(plainText)
                    .updateTime(LocalDateTime.now().toString())
                    .build();
            wxTransferLogMapper.updateByOrderNumber(log);
        } finally {
            transferNotifyLock.unlock();
        }
    }

    private SecondHandProduct requireProduct(Long id) {
        if (id == null) {
            throw new ParamException(MessageConstant.NOT_FOUND_PARAM);
        }
        SecondHandProduct product = productMapper.getById(id);
        if (product == null) {
            throw new SecondHandException("商品不存在");
        }
        return product;
    }

    private SecondHandOrder requireOrder(Long id) {
        if (id == null) {
            throw new ParamException(MessageConstant.NOT_FOUND_PARAM);
        }
        SecondHandOrder order = orderMapper.getById(id);
        if (order == null) {
            throw new SecondHandException("二手订单不存在");
        }
        return order;
    }

    private SecondHandBargain requireBargain(Long id) {
        if (id == null) {
            throw new ParamException(MessageConstant.NOT_FOUND_PARAM);
        }
        SecondHandBargain bargain = bargainMapper.getById(id);
        if (bargain == null) {
            throw new SecondHandException("议价不存在");
        }
        return bargain;
    }

    private void ensureAuthenticated() {
        UserVO user = userMapper.getById(BaseContext.getCurrentId());
        if (user == null || !Integer.valueOf(1).equals(user.getAuthentication())) {
            throw new UserException(MessageConstant.USER_NOT_AUTHEN);
        }
    }

    private void ensureOwner(Long ownerId) {
        if (!ownerId.equals(BaseContext.getCurrentId())) {
            throw new SecondHandException("无权操作该资源");
        }
    }

    private void validateCategory(AdminSecondHandCategoryDTO dto, boolean requireImage) {
        if (dto == null || trimToNull(dto.getName()) == null) {
            throw new ParamException("请输入分类名称");
        }
        if (dto.getName().trim().length() > 50) {
            throw new ParamException("分类名称不能超过 50 个字");
        }
        if (dto.getSort() != null && dto.getSort() < 0) {
            throw new ParamException("排序不能小于 0");
        }
        if (requireImage && dto.getImageAssetId() == null) {
            throw new ParamException("请选择分类图标");
        }
    }

    private void resolveCategoryImage(SecondHandCategory category) {
        if (category == null) {
            return;
        }
        var images = mediaAssetService.resolvePublicBinding(
                MediaAssetConstant.BOUND_SECOND_HAND_CATEGORY,
                category.getId(),
                MediaPurpose.SECOND_HAND_CATEGORY_ICON.name());
        if (!images.isEmpty()) {
            category.setImageAssetId(images.get(0).getMediaId());
            category.setImage(images.get(0).getUrl());
        }
    }

    private void validateProduct(SecondHandProductDTO dto) {
        if (dto == null || dto.getTitle() == null || dto.getTitle().isBlank() ||
                dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ZERO) <= 0 ||
                dto.getCategoryId() == null ||
                trimToNull(dto.getPickupAddressSnapshot()) == null) {
            throw new ParamException(MessageConstant.NOT_FOUND_PARAM);
        }
        if (dto.getImageAssetIds() == null || dto.getImageAssetIds().isEmpty()) {
            throw new ParamException("请至少选择一张商品图片");
        }
        if (dto.getImageAssetIds() != null && dto.getImageAssetIds().size() > 6) {
            throw new ParamException("商品图片不能超过 6 张");
        }
    }

    private SecondHandProductVO resolveProductImages(SecondHandProductVO product) {
        if (product == null) {
            return null;
        }
        var images = mediaAssetService.resolvePublicBinding(
                MediaAssetConstant.BOUND_SECOND_HAND_PRODUCT,
                product.getId(),
                MediaPurpose.SECOND_HAND_PRODUCT_IMAGE.name());
        if (!images.isEmpty()) {
            product.setImageAssetIds(images.stream().map(item -> item.getMediaId()).toList());
            product.setImages(String.join(",", images.stream().map(item -> item.getUrl()).toList()));
        } else {
            product.setImages("");
        }
        return product;
    }

    private Integer resolvePickupOnly(SecondHandProductDTO dto) {
        if (dto.getPickupOnly() != null) {
            return dto.getPickupOnly() == 1 ? 1 : 0;
        }
        return 1;
    }

    private String resolvePickupSnapshot(SecondHandProductDTO dto) {
        return trimToNull(dto.getPickupAddressSnapshot());
    }

    private String firstNotBlank(String first, String second) {
        String value = trimToNull(first);
        return value == null ? trimToNull(second) : value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int defaultZero(Integer value) {
        return value == null ? 0 : value;
    }

    private boolean isStatus(Integer actual, int expected) {
        return Integer.valueOf(expected).equals(actual);
    }

    private void validateProductStatus(Integer status) {
        if (status == null ||
                status < SecondHandConstant.PRODUCT_ON_SALE ||
                status > SecondHandConstant.PRODUCT_OFF_SHELF) {
            throw new SecondHandException("商品状态不正确");
        }
    }

    private void validateOrderStatus(Integer status) {
        if (status == null ||
                status < SecondHandConstant.ORDER_PENDING_PAY ||
                status > SecondHandConstant.ORDER_DISPUTE) {
            throw new SecondHandException("订单状态不正确");
        }
    }

    private void validateBargainStatus(Integer status) {
        if (status == null ||
                status < SecondHandConstant.BARGAIN_PENDING ||
                status > SecondHandConstant.BARGAIN_EXPIRED) {
            throw new SecondHandException("议价状态不正确");
        }
    }

    private int defaultOne(Integer value) {
        return value == null ? 1 : value;
    }

    private int getIntConfig(String key, int defaultValue) {
        SystemConfig config = configMapper.getByConfigKey(key);
        if (config == null || config.getConfigValue() == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(config.getConfigValue());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private BigDecimal getDecimalConfig(String key, BigDecimal defaultValue) {
        SystemConfig config = configMapper.getByConfigKey(key);
        if (config == null || config.getConfigValue() == null) {
            return defaultValue;
        }
        try {
            return new BigDecimal(config.getConfigValue());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private boolean isSelfTradeAllowed() {
        return getBooleanConfig("second_hand_allow_self_trade", SecondHandConstant.DEFAULT_ALLOW_SELF_TRADE);
    }

    private List<SecondHandOrderVO> enrichOrders(List<SecondHandOrderVO> orders) {
        return orders.stream().map(this::enrichOrder).toList();
    }

    private SecondHandOrderVO enrichOrder(SecondHandOrderVO order) {
        if (order == null) {
            return null;
        }
        var images = mediaAssetService.resolvePublicBinding(
                MediaAssetConstant.BOUND_SECOND_HAND_PRODUCT,
                order.getProductId(),
                MediaPurpose.SECOND_HAND_PRODUCT_IMAGE.name());
        if (!images.isEmpty()) {
            order.setProductImageAssetIds(images.stream().map(item -> item.getMediaId()).toList());
            order.setProductImages(String.join(",", images.stream().map(item -> item.getUrl()).toList()));
        } else {
            order.setProductImages("");
        }
        if (isStatus(order.getStatus(), SecondHandConstant.ORDER_PENDING_PAY) && order.getCreateTime() != null) {
            int minutes = getIntConfig("second_hand_payment_timeout_minutes", SecondHandConstant.DEFAULT_PAYMENT_TIMEOUT_MINUTES);
            LocalDateTime deadline = order.getCreateTime().plusMinutes(minutes);
            long remainSeconds = Math.max(0, Duration.between(LocalDateTime.now(), deadline).getSeconds());
            order.setPayDeadline(deadline);
            order.setPayRemainSeconds(remainSeconds);
        } else {
            order.setPayRemainSeconds(0L);
        }
        return order;
    }

    private boolean getBooleanConfig(String key, boolean defaultValue) {
        SystemConfig config = configMapper.getByConfigKey(key);
        if (config == null || config.getConfigValue() == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(config.getConfigValue());
    }

    private String decryptFromResource(Map<String, Object> bodyMap) throws GeneralSecurityException {
        Map<String, String> resource = (Map<String, String>) bodyMap.get("resource");
        AesUtil aesUtil = new AesUtil(weChatProperties.getApiV3Key().getBytes(StandardCharsets.UTF_8));
        return aesUtil.decryptToString(
                resource.get(WeChatPayConstant.ASSOCIATED_DATA).getBytes(StandardCharsets.UTF_8),
                resource.get(WeChatPayConstant.NONCE).getBytes(StandardCharsets.UTF_8),
                resource.get(WeChatPayConstant.CIPHERTEXT));
    }
}
