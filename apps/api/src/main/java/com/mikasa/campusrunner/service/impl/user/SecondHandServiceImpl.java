package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.utils.PaymentAmount;
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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SecondHandServiceImpl implements SecondHandService {
    @Autowired
    private SecondHandCategoryMapper categoryMapper;
    @Autowired
    private SecondHandSubscriptionService subscriptions;
    @Autowired
    private SecondHandProductMapper productMapper;
    @Autowired
    private SecondHandFavoriteMapper favoriteMapper;
    @Autowired
    private SecondHandOrderMapper orderMapper;
    @Autowired
    private SecondHandBargainMapper bargainMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
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
    @Autowired
    private com.mikasa.campusrunner.service.user.WeChatPayService paymentQueries;
    @Autowired
    private SecondHandRefundRecovery refundRecovery;
    @Autowired
    private org.springframework.transaction.PlatformTransactionManager transactionManager;


    @Override
    public List<SecondHandCategory> listCategories() {
        List<SecondHandCategory> categories = categoryMapper.list();
        if (!categories.isEmpty()) {
            var images = mediaAssetService.resolvePublicBindings(MediaAssetConstant.BOUND_SECOND_HAND_CATEGORY,
                    categories.stream().map(SecondHandCategory::getId).distinct().toList(),
                    MediaPurpose.SECOND_HAND_CATEGORY_ICON.name(), 0);
            for (var category : categories) {
                var bound = images.getOrDefault(category.getId(), List.of());
                if (!bound.isEmpty()) {
                    category.setImageAssetId(bound.get(0).getMediaId());
                    category.setImage(bound.get(0).getUrl());
                }
            }
        }
        return categories;
    }

    @Override
    public List<SecondHandCategory> listCategories(com.mikasa.campusrunner.pojo.dto.ListPageDTO page) {
        page.getLimit(); page.getOffset();
        List<SecondHandCategory> categories = categoryMapper.listPage(page);
        if (!categories.isEmpty()) {
            var images = mediaAssetService.resolvePublicBindings(MediaAssetConstant.BOUND_SECOND_HAND_CATEGORY,
                    categories.stream().map(SecondHandCategory::getId).distinct().toList(),
                    MediaPurpose.SECOND_HAND_CATEGORY_ICON.name(), 0);
            for (var category : categories) {
                var bound = images.getOrDefault(category.getId(), List.of());
                if (!bound.isEmpty()) {
                    category.setImageAssetId(bound.get(0).getMediaId());
                    category.setImage(bound.get(0).getUrl());
                }
            }
        }
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
        return resolveProductImages(productMapper.detail(product.getId(), BaseContext.getCurrentId()));
    }

    @Override
    @Transactional
    public void updateProduct(Long id, SecondHandProductDTO dto) {
        ensureAuthenticated();
        if (dto == null) throw new ParamException("请填写商品信息");
        SecondHandProduct product = requireProductForUpdate(id);
        ensureOwner(product.getSellerId());
        if (!isStatus(product.getStatus(), SecondHandConstant.PRODUCT_ON_SALE) &&
                !isStatus(product.getStatus(), SecondHandConstant.PRODUCT_OFF_SHELF)) {
            throw new SecondHandException("交易中的商品不可编辑");
        }
        validateProductFields(dto);
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
        SecondHandProduct product = requireProductForUpdate(id);
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
        if (status == SecondHandConstant.PRODUCT_OFF_SHELF) {
            expireBargains(product, "已下架", "商品已下架，报价已失效");
        }
    }

    @Override
    @Transactional
    public void adminUpdateProductStatus(Long id, Integer status) {
        validateProductStatus(status);
        SecondHandProduct product = requireProductForUpdate(id);
        if (java.util.Objects.equals(product.getStatus(), status)) return;
        if (orderMapper.getActiveByProductId(id) != null) {
            throw new SecondHandException("商品存在进行中的订单，暂不可修改状态");
        }
        if (status == SecondHandConstant.PRODUCT_LOCKED || status == SecondHandConstant.PRODUCT_TRADING) {
            throw new SecondHandException("请通过订单处理交易状态");
        }
        productMapper.updateStatus(id, status);
        if (status != SecondHandConstant.PRODUCT_ON_SALE) {
            expireBargains(product, status == SecondHandConstant.PRODUCT_SOLD ? "已售出" : "已下架",
                    status == SecondHandConstant.PRODUCT_SOLD ? "商品已售出，报价已失效" : "商品已下架，报价已失效");
        }
    }

    @Override
    public List<SecondHandProductVO> listProducts(SecondHandProductQueryDTO query) {
        Long userId = BaseContext.getCurrentId();
        Long schoolId = userId == null ? null : userMapper.getSchoolId(userId);
        if (query == null) {
            query = new SecondHandProductQueryDTO();
        }
        if (query.getStatus() == null) {
            query.setStatus(SecondHandConstant.PRODUCT_ON_SALE);
        }
        query.getOffset(); // Validate pagination before any product/media query.
        List<SecondHandProductVO> products = productMapper.listPage(query, schoolId);
        resolveProductImagesBatch(products, 480);
        resolveAcceptedOfferPrices(products);
        return products;
    }

    @Override
    public List<SecondHandProductVO> listMyProducts() {
        return listMyProducts(new SecondHandProductQueryDTO());
    }

    @Override
    public List<SecondHandProductVO> listMyProducts(SecondHandProductQueryDTO query) {
        ensureAuthenticated();
        if (query == null) query = new SecondHandProductQueryDTO();
        query.getOffset();
        if (query.getStatus() != null && (query.getStatus() < 0 || query.getStatus() > 4)) {
            throw new ParamException("筛选条件有误，请重新选择");
        }
        List<SecondHandProductVO> products =
                productMapper.listBySeller(BaseContext.getCurrentId(), query);
        resolveProductImagesBatch(products, 0);
        return products;
    }

    @Override
    public SecondHandProductVO productDetail(Long id) {
        productMapper.increaseViewCount(id);
        SecondHandProductVO detail = productMapper.detail(id, BaseContext.getCurrentId());
        if (detail == null) {
            throw new SecondHandException("商品不存在");
        }
        var avatars = mediaAssetService.resolvePublicBinding(
                MediaAssetConstant.BOUND_USER_AVATAR,
                detail.getSellerId(),
                MediaPurpose.AVATAR.name());
        if (!avatars.isEmpty()) {
            detail.setSellerAvatar(avatars.get(0).getUrl());
        }
        resolveAcceptedOfferPrices(List.of(detail));
        return resolveProductImages(detail);
    }

    @Override
    public List<SecondHandProductVO> listFavoriteProducts() {
        return listFavoriteProducts(new SecondHandProductQueryDTO());
    }

    @Override
    public List<SecondHandProductVO> listFavoriteProducts(SecondHandProductQueryDTO query) {
        ensureAuthenticated();
        if (query == null) query = new SecondHandProductQueryDTO();
        query.getOffset();
        List<SecondHandProductVO> products = productMapper.listFavorites(BaseContext.getCurrentId(), query);
        resolveProductImagesBatch(products, 0);
        resolveAcceptedOfferPrices(products);
        return products;
    }

    @Override
    @Transactional
    public void favoriteProduct(Long id) {
        ensureAuthenticated();
        SecondHandProduct product = requireProduct(id);
        Long userId = BaseContext.getCurrentId();
        if (product.getSellerId().equals(userId)) {
            throw new SecondHandException("不能收藏自己的商品");
        }
        if (favoriteMapper.insertIgnore(userId, id, LocalDateTime.now()) > 0) {
            productMapper.changeFavoriteCount(id, 1);
        }
    }

    @Override
    @Transactional
    public void unfavoriteProduct(Long id) {
        ensureAuthenticated();
        requireProduct(id);
        if (favoriteMapper.delete(BaseContext.getCurrentId(), id) > 0) {
            productMapper.changeFavoriteCount(id, -1);
        }
    }

    @Override
    @Transactional
    public SecondHandBargainVO createBargain(SecondHandBargainDTO dto) {
        ensureAuthenticated();
        SecondHandProduct product = requireProductForUpdate(dto.getProductId());
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
        ensureBargainProductAvailable(product);
        var active = bargainMapper.listActiveByBuyerAndProductForUpdate(buyerId, product.getId());
        if (active.stream().anyMatch(item -> isStatus(item.getStatus(), SecondHandConstant.BARGAIN_ACCEPTED))) {
            throw new SecondHandException("卖家已接受报价，请前往下单");
        }
        // 同一待回复报价重试时复用原记录，不重复消耗次数或发送通知。
        if (!active.isEmpty()) {
            var latest = active.get(0);
            if (latest.getOfferPrice().compareTo(dto.getOfferPrice()) == 0
                    && java.util.Objects.equals(latest.getMessage(), dto.getMessage())) {
                var query = new com.mikasa.campusrunner.pojo.dto.BargainPageDTO();
                query.setBargainId(latest.getId());
                return bargainMapper.listByProductPage(product.getId(), buyerId, query).stream().findFirst()
                        .orElseThrow(() -> new SecondHandException("议价暂不可用，请刷新后重试"));
            }
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
        // 商品锁串行化新报价与接受操作；旧报价失效与新增同事务提交或回滚。
        bargainMapper.expirePendingByBuyerAndProduct(buyerId, product.getId());
        bargainMapper.insert(bargain);
        subscriptions.bargain(product.getSellerId(), product.getId(), "买家出价 ¥" + bargain.getOfferPrice().stripTrailingZeros().toPlainString() + " 元", now);
        return bargainMapper.listByProduct(product.getId()).stream()
                .filter(item -> item.getId().equals(bargain.getId()))
                .findFirst()
                .orElseThrow(() -> new SecondHandException("议价创建失败"));
    }

    @Override
    @Transactional
    public void acceptBargain(Long bargainId, SecondHandOrderCreateDTO dto) {
        ensureAuthenticated();
        SecondHandBargain bargain = requireBargain(bargainId);
        ensureOwner(bargain.getSellerId());
        if (!isStatus(bargain.getStatus(), SecondHandConstant.BARGAIN_PENDING)) {
            throw new SecondHandException("该议价已处理或已失效，请刷新后查看");
        }
        SecondHandProduct product = requireProductForUpdate(bargain.getProductId());
        ensureBargainProductAvailable(product);
        ensureQuoteHasNotBeenSuperseded(bargain);
        if (bargainMapper.updatePendingStatus(bargain.getId(), SecondHandConstant.BARGAIN_ACCEPTED) == 0) {
            throw new SecondHandException("该议价已处理或已失效，请刷新后查看");
        }
        subscriptions.bargainAccepted(bargain.getBuyerId(), product.getId(), bargain.getId(),
                "报价已接受，请选择交付方式下单", LocalDateTime.now());
    }

    @Override
    @Transactional
    public void rejectBargain(Long bargainId) {
        rejectBargain(bargainId, null);
    }

    @Override
    @Transactional
    public void rejectBargain(Long bargainId, SecondHandBargainRejectDTO dto) {
        ensureAuthenticated();
        String code = null;
        String note = null;
        if (dto != null) {
            code = dto.getReasonCode();
            if (code == null || !java.util.Set.of("NOT_SELLING_NOW", "PRICE_NOT_SUITABLE", "CUSTOM").contains(code)) {
                throw new ParamException("请选择拒绝原因");
            }
            if ("CUSTOM".equals(code) && dto.getNote() != null) {
                note = dto.getNote().strip();
                if (note.codePointCount(0, note.length()) > 40) throw new ParamException("原因请控制在40字以内");
                if (note.isBlank()) note = null;
            }
        }
        SecondHandBargain bargain = requireBargain(bargainId);
        ensureOwner(bargain.getSellerId());
        SecondHandProduct product = requireProductForUpdate(bargain.getProductId());
        ensureBargainProductAvailable(product);
        ensureQuoteHasNotBeenSuperseded(bargain);
        int updated = dto == null
                ? bargainMapper.updatePendingStatus(bargain.getId(), SecondHandConstant.BARGAIN_REJECTED)
                : bargainMapper.rejectPending(bargain.getId(), code, note);
        if (updated == 0) {
            throw new SecondHandException("该议价已处理或已失效，请刷新后查看");
        }
    }

    @Override
    @Transactional
    public void withdrawBargain(Long bargainId) {
        ensureAuthenticated();
        SecondHandBargain bargain = requireBargainForUpdate(bargainId);
        ensureOwner(bargain.getBuyerId());
        if (isStatus(bargain.getStatus(), SecondHandConstant.BARGAIN_WITHDRAWN)) {
            return;
        }
        if (isStatus(bargain.getStatus(), SecondHandConstant.BARGAIN_ACCEPTED)) {
            throw new SecondHandException("卖家已接受，请查看议价记录");
        }
        if (bargainMapper.updatePendingStatus(bargainId, SecondHandConstant.BARGAIN_WITHDRAWN) == 0) {
            throw new SecondHandException("报价已处理或失效，请刷新查看");
        }
    }

    @Override
    public List<SecondHandBargainVO> listMyBargains() {
        return listMyBargains(new com.mikasa.campusrunner.pojo.dto.BargainPageDTO());
    }

    @Override
    public List<SecondHandBargainVO> listMyBargains(com.mikasa.campusrunner.pojo.dto.BargainPageDTO query) {
        ensureAuthenticated();
        if (query == null) query = new com.mikasa.campusrunner.pojo.dto.BargainPageDTO();
        query.getOffset();
        var bargains = bargainMapper.listMinePage(BaseContext.getCurrentId(), query);
        var covers = mediaAssetService.resolvePublicBindings(MediaAssetConstant.BOUND_SECOND_HAND_PRODUCT,
                bargains.stream().map(SecondHandBargainVO::getProductId).filter(java.util.Objects::nonNull).toList(),
                MediaPurpose.SECOND_HAND_PRODUCT_IMAGE.name(), 480);
        for (var bargain : bargains) {
            var images = bargain.getProductId() == null ? List.<com.mikasa.campusrunner.pojo.vo.BoundMediaVO>of()
                    : covers.getOrDefault(bargain.getProductId(), List.of());
            bargain.setProductCoverImage(images.isEmpty() ? "" : images.get(0).getUrl());
        }
        return bargains;
    }

    @Override
    public List<SecondHandBargainVO> listProductBargains(Long productId) {
        return listProductBargains(productId, new com.mikasa.campusrunner.pojo.dto.BargainPageDTO());
    }

    @Override
    public List<SecondHandBargainVO> listProductBargains(Long productId, com.mikasa.campusrunner.pojo.dto.BargainPageDTO query) {
        ensureAuthenticated();
        if (query == null) query = new com.mikasa.campusrunner.pojo.dto.BargainPageDTO();
        query.getOffset();
        SecondHandProduct product = requireProduct(productId);
        Long userId = BaseContext.getCurrentId();
        return bargainMapper.listByProductPage(productId, product.getSellerId().equals(userId) ? null : userId, query);
    }

    @Override
    @Transactional
    public SecondHandOrderVO createOrder(SecondHandOrderCreateDTO dto) {
        ensureAuthenticated();
        if (dto == null || dto.getProductId() == null) {
            throw new ParamException("请选择商品");
        }
        if (dto.getBargainId() != null) {
            // 先锁商品再读报价，与接受和商品占用保持相同顺序。
            SecondHandProduct product = requireProductForUpdate(dto.getProductId());
            SecondHandBargain bargain = requireBargainForUpdate(dto.getBargainId());
            ensureOwner(bargain.getBuyerId());
            if (!product.getId().equals(bargain.getProductId())
                    || !product.getSellerId().equals(bargain.getSellerId())
                    || !isStatus(bargain.getStatus(), SecondHandConstant.BARGAIN_ACCEPTED)) {
                throw new SecondHandException("该报价当前不可下单，请刷新查看");
            }
            if (orderMapper.getByBargainId(bargain.getId()) != null) {
                throw new SecondHandException("该报价已生成订单，请查看订单");
            }
            ensureQuoteHasNotBeenSuperseded(bargain);
            return createOrderInternal(product, bargain.getBuyerId(), bargain.getOfferPrice(), dto, true);
        }
        SecondHandProduct product = requireProductForUpdate(dto.getProductId());
        return createOrderInternal(product, BaseContext.getCurrentId(), product.getPrice(), dto, false);
    }

    @Override
    @Transactional
    public List<SecondHandOrderVO> listBuyerOrders() {
        return listBuyerOrders(new SecondHandOrderQueryDTO());
    }

    @Override
    @Transactional
    public List<SecondHandOrderVO> listBuyerOrders(SecondHandOrderQueryDTO query) {
        ensureAuthenticated();
        if (query == null) query = new SecondHandOrderQueryDTO();
        query.getOffset();
        refreshUnpaidTimeouts(BaseContext.getCurrentId());
        List<SecondHandOrderVO> orders = enrichOrders(orderMapper.listByBuyer(BaseContext.getCurrentId(), query));
        resolveCounterpartyAvatars(orders, false);
        return orders;
    }

    @Override
    @Transactional
    public List<SecondHandOrderVO> listSellerOrders() {
        return listSellerOrders(new SecondHandOrderQueryDTO());
    }

    @Override
    @Transactional
    public List<SecondHandOrderVO> listSellerOrders(SecondHandOrderQueryDTO query) {
        ensureAuthenticated();
        if (query == null) query = new SecondHandOrderQueryDTO();
        query.getOffset();
        refreshUnpaidTimeouts(BaseContext.getCurrentId());
        List<SecondHandOrderVO> orders = enrichOrders(orderMapper.listBySeller(BaseContext.getCurrentId(), query));
        resolveCounterpartyAvatars(orders, true);
        return orders;
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
        SecondHandOrderVO enriched = enrichOrder(orderMapper.detail(id));
        if (isOfflineTradeMode(enriched.getTradeMode()) || enriched.getPayTime() != null) {
            Long counterpartyId = enriched.getBuyerId().equals(userId)
                    ? enriched.getSellerId()
                    : enriched.getBuyerId();
            UserVO counterparty = userMapper.getById(counterpartyId);
            if (counterparty != null) {
                enriched.setCounterpartyPhone(trimToNull(counterparty.getPhone()));
            }
        }
        return enriched;
    }

    @Override
    @Transactional
    public void markPaid(String orderNumber) {
        SecondHandOrder order = SecondHandOrderLocks.byNumber(orderMapper, productMapper, orderNumber);
        if (order == null || isOfflineOrder(order) ||
                !isStatus(order.getStatus(), SecondHandConstant.ORDER_PENDING_PAY)) {
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
        if (isOfflineOrder(order)) {
            cancelOfflineOrder(order, reason);
            return;
        }
        if (!order.getBuyerId().equals(BaseContext.getCurrentId())) {
            throw new SecondHandException("只有买家可以取消订单");
        }
        if (isStatus(order.getStatus(), SecondHandConstant.ORDER_CANCELED) ||
                isStatus(order.getStatus(), SecondHandConstant.ORDER_REFUNDING) ||
                isStatus(order.getStatus(), SecondHandConstant.ORDER_REFUND_SUCCESS) ||
                isStatus(order.getStatus(), SecondHandConstant.ORDER_REFUND_ABNORMAL)) {
            return;
        }
        if (isStatus(order.getStatus(), SecondHandConstant.ORDER_PENDING_PAY)
                || isStatus(order.getStatus(), SecondHandConstant.ORDER_PAID_WAIT_DELIVERY)) {
            cancelOnlineOrder(order, reason == null ? "买家取消订单" : reason);
            return;
        }
        throw new SecondHandException("当前订单无法取消，请先与卖家协商");
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
            throw new SecondHandException("订单当前不可标记交付");
        }
        LocalDateTime now = LocalDateTime.now();
        order.setStatus(SecondHandConstant.ORDER_DELIVERED_WAIT_CONFIRM);
        order.setDeliveredTime(now);
        if (!isOfflineOrder(order)) {
            int hours = getIntConfig("second_hand_auto_confirm_hours", SecondHandConstant.DEFAULT_AUTO_CONFIRM_HOURS);
            order.setConfirmDeadline(now.plusHours(hours));
        }
        order.setUpdateTime(now);
        orderMapper.update(order);
        if (isOfflineOrder(order)) subscriptions.order(order, order.getBuyerId(), "待确认", "卖家已交付，请确认收货", now);
    }

    @Override
    @Transactional
    public void confirmOrder(Long id) {
        ensureAuthenticated();
        refreshUnpaidTimeouts();
        SecondHandOrder order = requireOrder(id);
        if (!order.getBuyerId().equals(BaseContext.getCurrentId())) {
            throw new SecondHandException(isOfflineOrder(order)
                    ? "只有买家可以确认交易完成"
                    : "只有买家可以确认收货");
        }
        if (isStatus(order.getStatus(), SecondHandConstant.ORDER_TRANSFERING) ||
                isStatus(order.getStatus(), SecondHandConstant.ORDER_TRANSFER_SUCCESS) ||
                isStatus(order.getStatus(), SecondHandConstant.ORDER_TRANSFER_FAILED) ||
                isStatus(order.getStatus(), SecondHandConstant.ORDER_COMPLETED)) {
            return;
        }
        completeOrder(order);
        if (isOfflineOrder(order)) subscriptions.order(order, order.getSellerId(), "已完成", "交易已完成，感谢使用", order.getFinishTime());
    }

    @Override
    @Transactional
    public SecondHandTransferClaimVO getTransferClaim(Long orderId) {
        ensureAuthenticated();
        SecondHandOrder order = requireOrder(orderId);
        if (!order.getSellerId().equals(BaseContext.getCurrentId())) {
            throw new SecondHandException("只有卖家可以确认收款");
        }
        if (isOfflineOrder(order)) {
            throw new SecondHandException("线下交易无需在平台确认收款");
        }
        if (isStatus(order.getStatus(), SecondHandConstant.ORDER_TRANSFERING)) {
            querySellerTransfer(order);
            order = requireOrder(orderId);
        }
        if (isStatus(order.getStatus(), SecondHandConstant.ORDER_TRANSFER_SUCCESS)) {
            return SecondHandTransferClaimVO.builder().state("SUCCESS").build();
        }
        if (!isStatus(order.getStatus(), SecondHandConstant.ORDER_TRANSFERING)) {
            throw new SecondHandException("当前无需确认收款");
        }
        if (!"WAIT_USER_CONFIRM".equals(order.getTransferState()) ||
                trimToNull(order.getTransferPackageInfo()) == null) {
            throw new SecondHandException("收款正在处理中，请稍后刷新");
        }
        return SecondHandTransferClaimVO.builder()
                .state(order.getTransferState())
                .mchId(weChatProperties.getMchid())
                .appId(weChatProperties.getAppid())
                .packageInfo(order.getTransferPackageInfo())
                .build();
    }

    @Override
    @Transactional
    public SecondHandMessageVO createMessage(SecondHandMessageDTO dto) {
        ensureAuthenticated();
        if (dto.getContent() == null || dto.getContent().isBlank()) {
            throw new ParamException("消息内容不能为空");
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
                throw new SecondHandException("无法在该订单中发送消息");
            }
        }
        if (receiverId == null) {
            receiverId = product.getSellerId().equals(senderId) ? null : product.getSellerId();
        }
        if (receiverId == null || receiverId.equals(senderId)) {
            throw new SecondHandException("不能给自己发送消息");
        }
        if (!product.getSellerId().equals(senderId) && !product.getSellerId().equals(receiverId)) {
            throw new SecondHandException("该商品暂不支持向此用户发送消息");
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
        subscriptions.message(message);
        var created = messageMapper.getForUser(message.getId(), senderId);
        if (created == null) throw new SecondHandException("消息发送失败，请重试");
        return created;
    }

    @Override
    public List<SecondHandMessageVO> listProductMessages(Long productId) {
        return listProductMessages(productId, new com.mikasa.campusrunner.pojo.dto.MessagePageDTO());
    }

    @Override
    public List<SecondHandMessageVO> listProductMessages(Long productId,
            com.mikasa.campusrunner.pojo.dto.MessagePageDTO query) {
        if (query == null) query = new com.mikasa.campusrunner.pojo.dto.MessagePageDTO();
        query.getLimit();
        ensureAuthenticated();
        return readMessagePage(productId, null, query);
    }

    @Override
    public List<SecondHandConversationVO> listConversations() {
        return listConversations(new com.mikasa.campusrunner.pojo.dto.ListPageDTO());
    }

    @Override
    public List<SecondHandConversationVO> listConversations(com.mikasa.campusrunner.pojo.dto.ListPageDTO query) {
        if (query == null) query = new com.mikasa.campusrunner.pojo.dto.ListPageDTO();
        query.getOffset();
        ensureAuthenticated();
        Long userId = BaseContext.getCurrentId();
        var conversations = messageMapper.listConversationsPage(userId, query);
        if (conversations.stream().anyMatch(row -> row == null || !userId.equals(row.getAccessUserId())
                || row.getProductId() == null || row.getProductId() <= 0
                || row.getCounterpartyId() == null || row.getCounterpartyId() <= 0
                || (row.getMediaProductId() != null && !row.getProductId().equals(row.getMediaProductId()))
                || row.getUnreadCount() == null || row.getUnreadCount() < 0)) {
            throw new SecondHandException("私信加载失败，请重试");
        }
        var images = mediaAssetService.resolvePublicBindings(MediaAssetConstant.BOUND_SECOND_HAND_PRODUCT,
                conversations.stream().map(SecondHandConversationVO::getMediaProductId)
                        .filter(java.util.Objects::nonNull).distinct().toList(),
                MediaPurpose.SECOND_HAND_PRODUCT_IMAGE.name(), 0);
        for (var row : conversations) {
            row.setProductTitle(firstNotBlank(row.getProductTitle(), "商品"));
            row.setCounterpartyName(firstNotBlank(row.getCounterpartyName(), "同学"));
            var bound = row.getMediaProductId() == null ? List.<com.mikasa.campusrunner.pojo.vo.BoundMediaVO>of()
                    : images.getOrDefault(row.getMediaProductId(), List.of());
            row.setProductImages(String.join(",", bound.stream().map(item -> item.getUrl()).toList()));
        }
        return conversations;
    }

    @Override
    @Transactional
    public List<SecondHandMessageVO> listConversationMessages(Long productId, Long counterpartyId) {
        return listConversationMessages(productId, counterpartyId, new com.mikasa.campusrunner.pojo.dto.MessagePageDTO());
    }

    @Override
    @Transactional
    public List<SecondHandMessageVO> listConversationMessages(Long productId, Long counterpartyId,
            com.mikasa.campusrunner.pojo.dto.MessagePageDTO query) {
        if (query == null) query = new com.mikasa.campusrunner.pojo.dto.MessagePageDTO();
        query.getLimit();
        ensureAuthenticated();
        SecondHandProduct product = requireProduct(productId);
        Long userId = BaseContext.getCurrentId();
        validateConversationCounterparty(product, userId, counterpartyId);
        var rows = readMessagePage(productId, counterpartyId, query);
        var unreadIds = rows.stream().filter(row -> userId.equals(row.getReceiverId()) && row.getReadTime() == null)
                .map(SecondHandMessageVO::getId).toList();
        if (!unreadIds.isEmpty()) {
            messageMapper.markMessagesRead(userId, unreadIds);
            var readTime = LocalDateTime.now();
            rows.stream().filter(row -> unreadIds.contains(row.getId())).forEach(row -> row.setReadTime(readTime));
        }
        return rows;
    }

    private List<SecondHandMessageVO> readMessagePage(Long productId, Long counterpartyId,
            com.mikasa.campusrunner.pojo.dto.MessagePageDTO query) {
        Long userId = BaseContext.getCurrentId();
        Long cursorId = query.getBeforeId() != null ? query.getBeforeId() : query.getAfterId();
        SecondHandMessageVO cursor = cursorId == null ? null : messageMapper.getCursorForUser(cursorId, userId);
        if (cursorId != null && (cursor == null || !java.util.Objects.equals(productId, cursor.getProductId())
                || !messageBelongsTo(cursor, userId, counterpartyId))) {
            throw new ParamException("加载范围有误，请重新尝试");
        }
        var rows = new java.util.ArrayList<>(messageMapper.listMessagePage(productId, userId, counterpartyId, query, cursor));
        if (rows.stream().anyMatch(row -> row == null || row.getId() == null
                || !java.util.Objects.equals(productId, row.getProductId()) || !messageBelongsTo(row, userId, counterpartyId))) {
            throw new SecondHandException("私信加载失败，请重试");
        }
        if (query.getAfterId() == null) java.util.Collections.reverse(rows);
        return rows;
    }

    private boolean messageBelongsTo(SecondHandMessageVO row, Long userId, Long counterpartyId) {
        return (userId.equals(row.getSenderId()) && (counterpartyId == null || counterpartyId.equals(row.getReceiverId())))
                || (userId.equals(row.getReceiverId()) && (counterpartyId == null || counterpartyId.equals(row.getSenderId())));
    }

    @Override
    public com.mikasa.campusrunner.pojo.vo.AdminSecondHandProductPageVO adminListProducts(SecondHandProductQueryDTO query) {
        if (query == null) query = new SecondHandProductQueryDTO();
        query.getOffset();
        if (query.getStatus() != null && (query.getStatus() < 0 || query.getStatus() > 4)) {
            throw new ParamException("筛选条件有误，请重新选择");
        }
        List<SecondHandProductVO> products = productMapper.listPage(query, null);
        long total = productMapper.countProducts(query, false, null);
        long activeProducts = productMapper.countProducts(query, true, null);
        resolveProductImagesBatch(products, 0);
        return new com.mikasa.campusrunner.pojo.vo.AdminSecondHandProductPageVO(total,
                query.getPage() == null ? 1 : query.getPage(), query.getLimit(), products, activeProducts);
    }

    @Override
    public SecondHandProductVO adminProductDetail(Long id) {
        SecondHandProductVO detail = productMapper.detail(id, null);
        if (detail == null) {
            throw new SecondHandException("商品不存在");
        }
        return resolveProductImages(detail);
    }

    @Override
    @Transactional
    public void adminDeleteProduct(Long id) {
        SecondHandProduct product = requireProductForUpdate(id);
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
        expireBargains(product, "不可购买", "商品已不可购买，报价已失效");
    }

    @Override
    public com.mikasa.campusrunner.pojo.vo.AdminSecondHandOrderPageVO adminListOrders(com.mikasa.campusrunner.pojo.dto.AdminSecondHandOrderQueryDTO query) {
        if (query == null) query = new com.mikasa.campusrunner.pojo.dto.AdminSecondHandOrderQueryDTO();
        query.getOffset();
        var orders = enrichOrders(orderMapper.listAdminPage(query));
        var stats = orderMapper.summarizeAdmin(query);
        return new com.mikasa.campusrunner.pojo.vo.AdminSecondHandOrderPageVO(stats,
                query.getPage() == null ? 1 : query.getPage(), query.getLimit(), orders);
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
        if (java.util.Objects.equals(order.getStatus(), status)) return;
        validateAdminTransition(order, status);
        String reason = trimToNull(dto.getReason());
        LocalDateTime now = LocalDateTime.now();
        if (isOfflineOrder(order)) {
            adminUpdateOfflineOrderStatus(order, status, reason, now);
            return;
        }
        if (isStatus(status, SecondHandConstant.ORDER_CANCELED)) {
            if (isStatus(order.getStatus(), SecondHandConstant.ORDER_CANCELED)
                    || isStatus(order.getStatus(), SecondHandConstant.ORDER_REFUNDING)
                    || isStatus(order.getStatus(), SecondHandConstant.ORDER_REFUND_ABNORMAL)
                    || isStatus(order.getStatus(), SecondHandConstant.ORDER_REFUND_SUCCESS)) return;
            if (!isStatus(order.getStatus(), SecondHandConstant.ORDER_PENDING_PAY)
                    && !isStatus(order.getStatus(), SecondHandConstant.ORDER_PAID_WAIT_DELIVERY)) {
                throw new SecondHandException("当前订单状态不可取消，请先协商处理");
            }
            cancelOnlineOrder(order, reason == null ? "管理员关闭订单" : reason);
            return;
        }
        if (isStatus(status, SecondHandConstant.ORDER_REFUNDING)
                || isStatus(status, SecondHandConstant.ORDER_REFUND_ABNORMAL)) {
            if (isStatus(order.getStatus(), SecondHandConstant.ORDER_PENDING_PAY)
                    || isStatus(order.getStatus(), SecondHandConstant.ORDER_PAID_WAIT_DELIVERY)) {
                cancelOnlineOrder(order, reason == null ? "管理员处理退款" : reason);
                return;
            }
            if (!isStatus(order.getStatus(), SecondHandConstant.ORDER_REFUNDING)
                    && !isStatus(order.getStatus(), SecondHandConstant.ORDER_REFUND_ABNORMAL)) {
                throw new SecondHandException("当前订单状态不能直接退款，请先核对资金并协商处理");
            }
            requestRefund(order);
            return;
        }
        if (isStatus(status, SecondHandConstant.ORDER_REFUND_SUCCESS)) {
            if (!java.util.Set.of(SecondHandConstant.ORDER_CANCELED, SecondHandConstant.ORDER_REFUNDING,
                    SecondHandConstant.ORDER_REFUND_ABNORMAL, SecondHandConstant.ORDER_REFUND_SUCCESS).contains(order.getStatus())) {
                throw new SecondHandException("当前订单状态与退款记录不一致，请先核对资金");
            }
            RefundInfo refund = refundInfoMapper.getLatestByOrderNumber(order.getOrderNumber());
            if (refund == null || !"SUCCESS".equals(refund.getRefundStatus())) {
                throw new SecondHandException("尚未确认退款成功，请先核对退款结果");
            }
            requestRefund(order);
            return;
        }
        if (isStatus(status, SecondHandConstant.ORDER_TRANSFERING)) {
            completeOrder(order);
            return;
        }
        if (isStatus(status, SecondHandConstant.ORDER_DELIVERED_WAIT_CONFIRM)) {
            order.setDeliveredTime(now);
            order.setConfirmDeadline(now.plusHours(getIntConfig("second_hand_auto_confirm_hours", SecondHandConstant.DEFAULT_AUTO_CONFIRM_HOURS)));
        }
        order.setStatus(status);
        order.setUpdateTime(now);

        if (isStatus(status, SecondHandConstant.ORDER_PAID_WAIT_DELIVERY) ||
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
        if (isOfflineOrder(order)) {
            throw new SecondHandException("线下交易订单没有平台收款流程");
        }
        if (!isStatus(order.getStatus(), SecondHandConstant.ORDER_TRANSFER_FAILED) &&
                !isStatus(order.getStatus(), SecondHandConstant.ORDER_TRANSFERING)) {
            throw new SecondHandException("当前订单状态不可重试收款");
        }
        RealPaymentGuard.require(paymentLogMapper.getByOrderNumber(order.getOrderNumber()),
                order.getOrderNumber(), order.getPayAmount());
        if (isStatus(order.getStatus(), SecondHandConstant.ORDER_TRANSFERING)) {
            querySellerTransfer(order);
            return;
        }
        int nextAttempt = (order.getTransferAttempt() == null ? 0 : order.getTransferAttempt()) + 1;
        order.setStatus(SecondHandConstant.ORDER_TRANSFERING);
        order.setTransferAttempt(nextAttempt);
        order.setTransferOutBillNo(SecondHandTransferSupport.nextOutBillNo(order.getOrderNumber(), nextAttempt));
        order.setTransferBillNo("");
        order.setTransferState("CREATED");
        order.setTransferPackageInfo("");
        order.setTransferFailReason("");
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.update(order);
        requestSellerTransfer(order);
    }

    @Override
    public com.mikasa.campusrunner.pojo.vo.AdminSecondHandBargainPageVO adminListBargains(com.mikasa.campusrunner.pojo.dto.ListPageDTO query) {
        if (query == null) query = new com.mikasa.campusrunner.pojo.dto.ListPageDTO();
        query.getOffset();
        var bargains = bargainMapper.listAdminPage(query);
        return new com.mikasa.campusrunner.pojo.vo.AdminSecondHandBargainPageVO(bargainMapper.summarizeAdmin(),
                query.getPage() == null ? 1 : query.getPage(), query.getLimit(), bargains);
    }

    @Override
    @Transactional
    public void adminUpdateBargainStatus(Long id, SecondHandStatusDTO dto) {
        if (dto == null || dto.getStatus() == null) {
            throw new ParamException(MessageConstant.NOT_FOUND_PARAM);
        }
        validateBargainStatus(dto.getStatus());
        SecondHandBargain bargain = requireBargainForUpdate(id);
        if (isStatus(bargain.getStatus(), SecondHandConstant.BARGAIN_WITHDRAWN)) {
            throw new SecondHandException("已撤回的报价不可修改");
        }
        bargain.setStatus(dto.getStatus());
        bargain.setUpdateTime(LocalDateTime.now());
        bargainMapper.update(bargain);
    }

    @Override
    public com.mikasa.campusrunner.pojo.dto.PageResult<SecondHandMessageVO> adminListMessages(com.mikasa.campusrunner.pojo.dto.AdminSecondHandMessageQueryDTO query) {
        if (query == null) query = new com.mikasa.campusrunner.pojo.dto.AdminSecondHandMessageQueryDTO();
        query.getOffset();
        var messages = messageMapper.listAdminPage(query);
        return new com.mikasa.campusrunner.pojo.dto.PageResult<>(messageMapper.countAdmin(query),
                query.getPage() == null ? 1 : query.getPage(), query.getLimit(), messages);
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public void processUnpaidTimeouts() {
        refreshUnpaidTimeouts();
        for (SecondHandOrder candidate : orderMapper.listCanceledPaymentRecovery(LocalDateTime.now().minusMinutes(20))) {
            inOrderTransaction(() -> {
                SecondHandOrder order = SecondHandOrderLocks.byId(orderMapper, productMapper, candidate.getId());
                if (order == null || isOfflineOrder(order) || !isStatus(order.getStatus(), SecondHandConstant.ORDER_CANCELED)) return;
                order.setUpdateTime(LocalDateTime.now());
                orderMapper.update(order);
                try {
                    String result = paymentQueries.weChatQueryOrder(order.getOrderNumber());
                    if ("SUCCESS".equals(JSONObject.parseObject(result).getString("trade_state"))) {
                        recordPaymentOnce(result, order);
                        order.setPayTime(LocalDateTime.now());
                        requestRefund(order);
                    }
                } catch (Exception e) {
                    log.warn("二手已取消订单支付状态待核对, orderId={}", order.getId());
                }
            });
        }
    }

    private void refreshUnpaidTimeouts() {
        refreshUnpaidTimeouts(null);
    }

    private void refreshUnpaidTimeouts(Long ownerId) {
        int minutes = getIntConfig("second_hand_payment_timeout_minutes", SecondHandConstant.DEFAULT_PAYMENT_TIMEOUT_MINUTES);
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(minutes);
        long afterId = 0;
        while (true) {
            var candidates = ownerId == null ? orderMapper.listUnpaidTimeoutBatch(cutoff, afterId, 100)
                    : orderMapper.listUnpaidTimeoutForOwner(cutoff, ownerId, 20);
            for (SecondHandOrder candidate : candidates) {
                inOrderTransaction(() -> {
                    SecondHandOrder order = SecondHandOrderLocks.byId(orderMapper, productMapper, candidate.getId());
                    if (order == null || isOfflineOrder(order) || !isStatus(order.getStatus(), SecondHandConstant.ORDER_PENDING_PAY)
                            || order.getCreateTime() == null || order.getCreateTime().isAfter(cutoff)) return;
                    try {
                        cancelOnlineOrder(order, "支付超时自动关闭");
                    } catch (SecondHandException e) {
                        log.warn("二手超时订单支付状态待核对, orderId={}", order.getId());
                    }
                });
            }
            if (ownerId != null || candidates.size() < 100) break;
            long nextId = candidates.stream().mapToLong(SecondHandOrder::getId).max().orElse(afterId);
            if (nextId <= afterId) throw new IllegalStateException("Timeout batch cursor did not advance");
            afterId = nextId;
        }
    }

    private void inOrderTransaction(Runnable operation) {
        var tx = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.executeWithoutResult(status -> operation.run());
    }

    private void cancelOnlineOrder(SecondHandOrder order, String reason) {
        boolean paid = isStatus(order.getStatus(), SecondHandConstant.ORDER_PAID_WAIT_DELIVERY);
        if (!paid) {
            String state = "CLOSED";
            try {
                weChatPayUtil.closeOrder(order.getOrderNumber());
            } catch (Exception e) {
                String result;
                try {
                    result = paymentQueries.weChatQueryOrder(order.getOrderNumber());
                    JSONObject payment = JSONObject.parseObject(result);
                    state = payment.getString("trade_state");
                    if ("SUCCESS".equals(state)) {
                        recordPaymentOnce(result, order);
                    }
                } catch (Exception queryFailure) {
                    throw new SecondHandException("支付状态暂未确认，请稍后重试取消");
                }
            }
            paid = "SUCCESS".equals(state);
            if (!paid && !"CLOSED".equals(state)) {
                throw new SecondHandException("支付状态暂未确认，请稍后重试取消");
            }
        }
        order.setCancelReason(reason);
        order.setCancelTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        if (paid) {
            if (order.getPayTime() == null) order.setPayTime(LocalDateTime.now());
            requestRefund(order);
        } else {
            order.setStatus(SecondHandConstant.ORDER_CANCELED);
            orderMapper.update(order);
            productMapper.releaseAfterRefund(order.getProductId(), order.getId());
        }
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public void processAutoConfirm() {
        for (SecondHandOrder candidate : orderMapper.listAutoConfirm(LocalDateTime.now())) {
            try {
                inOrderTransaction(() -> {
                    SecondHandOrder order = SecondHandOrderLocks.byId(orderMapper, productMapper, candidate.getId());
                    if (order == null || isOfflineOrder(order)
                            || !isStatus(order.getStatus(), SecondHandConstant.ORDER_DELIVERED_WAIT_CONFIRM)
                            || order.getConfirmDeadline() == null || order.getConfirmDeadline().isAfter(LocalDateTime.now())) return;
                    completeOrder(order);
                });
            } catch (com.mikasa.campusrunner.common.exception.OrderException e) {
                log.warn("二手自动确认资金来源待核对, orderId={}", candidate.getId());
            }
        }
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public void processTransferQueries() {
        for (SecondHandOrder candidate : orderMapper.listTransferring()) {
            inOrderTransaction(() -> {
                SecondHandOrder order = SecondHandOrderLocks.byId(orderMapper, productMapper, candidate.getId());
                if (order == null || !isStatus(order.getStatus(), SecondHandConstant.ORDER_TRANSFERING)) return;
                querySellerTransfer(order);
            });
        }
    }

    @Override
    @Transactional
    public WeChatPrePayVO jsapiPay(Long orderId) throws Exception {
        ensureAuthenticated();
        if (!SecondHandConstant.TRADE_MODE_ONLINE.equals(currentTradeMode())) {
            throw new SecondHandException("当前仅支持线下交易，请与卖家协商完成付款");
        }
        refreshUnpaidTimeouts();
        SecondHandOrder order = requireOrder(orderId);
        if (isOfflineOrder(order)) {
            throw new SecondHandException("线下交易订单无需在平台支付");
        }
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
        amountMap.put("total", PaymentAmount.cents(order.getPayAmount()));
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
                throw new IOException("WeChat request rejected");
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
    @Transactional(rollbackFor = Exception.class)
    public void processPayNotify(Map<String, Object> bodyMap) throws GeneralSecurityException {
        String plainText = decryptFromResource(bodyMap);
        Map plainTextMap = JSONObject.parseObject(plainText, HashMap.class);
        String orderNumber = (String) plainTextMap.get(WeChatPayConstant.OUT_TRADE_NO);
        SecondHandOrder order = SecondHandOrderLocks.byNumber(orderMapper, productMapper, orderNumber);
        if (order == null) throw new SecondHandException("订单尚未确认，请稍后重试");
        if (isOfflineOrder(order)) return;
        if (!WeChatPayConstant.TRADE_SUCCESS.equals(plainTextMap.get(WeChatPayConstant.TRADE_STATE))) return;
        recordPaymentOnce(plainText, order);
        if (isStatus(order.getStatus(), SecondHandConstant.ORDER_CANCELED)) {
            order.setPayTime(LocalDateTime.now());
            requestRefund(order);
        } else if (isStatus(order.getStatus(), SecondHandConstant.ORDER_PENDING_PAY)) {
            order.setStatus(SecondHandConstant.ORDER_PAID_WAIT_DELIVERY);
            order.setPayTime(LocalDateTime.now());
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.update(order);
            productMapper.markTrading(order.getProductId());
        } else if (isStatus(order.getStatus(), SecondHandConstant.ORDER_REFUNDING)
                || isStatus(order.getStatus(), SecondHandConstant.ORDER_REFUND_ABNORMAL)) {
            requestRefund(order);
        }
    }

    private SecondHandOrderVO createOrderInternal(SecondHandProduct product, Long buyerId, BigDecimal amount, SecondHandOrderCreateDTO dto, boolean negotiated) {
        if (dto == null) {
            dto = new SecondHandOrderCreateDTO();
        }
        if (!isSelfTradeAllowed() && product.getSellerId().equals(buyerId)) {
            throw new SecondHandException("不能给自己发布的商品下单");
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
        String buyerDeliverySnapshot = null;
        if (deliveryMode == 1) {
            if (dto.getBuyerDeliveryAddressId() == null) {
                throw new ParamException("请选择配送地址");
            }
            AddressBook query = new AddressBook();
            Long addressId = dto.getBuyerDeliveryAddressId();
            query.setId(addressId);
            query.setUserId(buyerId);
            var addresses = addressBookMapper.query(query);
            var address = addresses.stream().filter(item -> addressId.equals(item.getId())
                    && buyerId.equals(item.getUserId()) && Integer.valueOf(0).equals(item.getDeleted()))
                    .findFirst().orElseThrow(() -> new SecondHandException("配送地址不可用，请重新选择"));
            buyerDeliverySnapshot = trimToNull(java.util.stream.Stream.of(address.getCompusName(), address.getBuildCategoryName(),
                    address.getBuildingName(), address.getDetails())
                    .filter(value -> trimToNull(value) != null).map(String::trim)
                    .collect(java.util.stream.Collectors.joining(" ")));
        }
        if (deliveryMode == 1 && buyerDeliverySnapshot == null) {
            throw new ParamException("请选择配送地址");
        }
        if (!isStatus(product.getStatus(), SecondHandConstant.PRODUCT_ON_SALE)) {
            throw new SecondHandException("商品当前不可下单");
        }
        if (orderMapper.getActiveByProductId(product.getId()) != null) {
            throw new SecondHandException("商品已有进行中的订单");
        }
        if (productMapper.lockOnSaleProduct(product.getId()) == 0) {
            throw new SecondHandException("商品已被锁定或售出");
        }
        String tradeMode = currentTradeMode();
        boolean offline = SecondHandConstant.TRADE_MODE_OFFLINE.equals(tradeMode);
        BigDecimal feeRate = offline
                ? BigDecimal.ZERO
                : getDecimalConfig(
                        SecondHandConstant.CONFIG_SERVICE_FEE_RATE,
                        new BigDecimal(SecondHandConstant.DEFAULT_SERVICE_FEE_RATE));
        BigDecimal serviceFee = offline
                ? BigDecimal.ZERO.setScale(2)
                : amount.multiply(feeRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal sellerIncome = amount.subtract(serviceFee).setScale(2, RoundingMode.HALF_UP);
        LocalDateTime now = LocalDateTime.now();
        SecondHandOrder order = SecondHandOrder.builder()
                .productId(product.getId())
                .bargainId(negotiated ? dto.getBargainId() : null)
                .buyerId(buyerId)
                .sellerId(product.getSellerId())
                .tradeMode(tradeMode)
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
                .status(offline
                        ? SecondHandConstant.ORDER_OFFLINE_WAIT_DELIVERY
                        : SecondHandConstant.ORDER_PENDING_PAY)
                .transferAttempt(0)
                .deleted(DeleteConstant.UN_DELETED)
                .createTime(now)
                .updateTime(now)
                .build();
        com.mikasa.campusrunner.common.utils.OrderNumberGenerator.persist("SH", number -> {
            order.setId(null);
            order.setOrderNumber(number);
            orderMapper.insert(order);
            return order.getId();
        }, number -> orderMapper.getByOrderNumberForUpdate(number) != null);
        // 已下单的成交报价保留，其余待回复及已接受但未下单的报价失效。
        expireBargains(product, "已被下单", "商品已被下单，报价已失效", order.getBuyerId());
        if (offline) {
            subscriptions.order(order, product.getSellerId(), "新订单", "买家已下单，请及时处理", now);
        }
        if (offline) {
            productMapper.markTrading(product.getId());
        }
        return orderMapper.detail(order.getId());
    }

    private void completeOrder(SecondHandOrder order) {
        if (!isStatus(order.getStatus(), SecondHandConstant.ORDER_DELIVERED_WAIT_CONFIRM)) {
            throw new SecondHandException(isOfflineOrder(order)
                    ? "订单当前不可确认完成"
                    : "订单不可确认收货");
        }
        LocalDateTime now = LocalDateTime.now();
        if (isOfflineOrder(order)) {
            order.setStatus(SecondHandConstant.ORDER_COMPLETED);
            order.setFinishTime(now);
            order.setUpdateTime(now);
            orderMapper.update(order);
            productMapper.markSold(order.getProductId());
            return;
        }
        RealPaymentGuard.require(paymentLogMapper.getByOrderNumber(order.getOrderNumber()),
                order.getOrderNumber(), order.getPayAmount());
        order.setStatus(SecondHandConstant.ORDER_TRANSFERING);
        order.setFinishTime(now);
        order.setUpdateTime(now);
        orderMapper.update(order);
        productMapper.markSold(order.getProductId());

        int attempt = order.getTransferAttempt() == null ? 1 : Math.max(1, order.getTransferAttempt());
        if (trimToNull(order.getTransferOutBillNo()) == null) {
            order.setTransferAttempt(attempt);
            order.setTransferOutBillNo(SecondHandTransferSupport.nextOutBillNo(order.getOrderNumber(), attempt));
            order.setTransferState("CREATED");
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.update(order);
        }
        requestSellerTransfer(order);
    }

    private void savePaymentLog(String plainText, SecondHandOrder order) {
        Map map = JSONObject.parseObject(plainText, HashMap.class);
        Map<String, String> payer = (Map) map.get(WeChatPayConstant.PAYER);
        int total = PaymentAmount.verifyResult(map, order.getOrderNumber(), order.getPayAmount());
        PaymentLog paymentLog = PaymentLog.builder()
                .orderNumber(order.getOrderNumber())
                .paymentType(WeChatPayConstant.PAYMENT_TYPE)
                .transactionId((String) map.get(WeChatPayConstant.TRANSACTION_ID))
                .tradeType((String) map.get(WeChatPayConstant.TRADE_TYPE))
                .tradeState((String) map.get(WeChatPayConstant.TRADE_STATE))
                .bankType((String) map.get(WeChatPayConstant.BANK_TYPE))
                .successTime((String) map.get(WeChatPayConstant.SUCCESS_TIME))
                .payerOpenid(payer == null ? null : payer.get(WeChatPayConstant.OPENID))
                .total((long) total)
                .serviceFeeRate(order.getServiceFeeRate())
                .serviceFee((long) PaymentAmount.nonNegativeCents(order.getServiceFee()))
                .content(plainText)
                .deleted(DeleteConstant.UN_DELETED)
                .build();
        RealPaymentGuard.require(paymentLog, order.getOrderNumber(), order.getPayAmount());
        paymentLogMapper.insert(paymentLog);
    }

    private void requestSellerTransfer(SecondHandOrder order) {
        String sellerOpenid = userMapper.getOpenidById(order.getSellerId());
        int transferAmount;
        try {
            PaymentLog payment = RealPaymentGuard.require(paymentLogMapper.getByOrderNumber(order.getOrderNumber()),
                    order.getOrderNumber(), order.getPayAmount());
            transferAmount = RealPaymentGuard.transfer(payment, order.getSellerIncome(), order.getServiceFee());
        } catch (com.mikasa.campusrunner.common.exception.OrderException e) {
            markTransferFailed(order, e.getMessage());
            return;
        }
        if (trimToNull(sellerOpenid) == null) {
            markTransferFailed(order, "卖家尚未完成微信账号绑定");
            return;
        }
        if (transferAmount <= 0) {
            markTransferFailed(order, "卖家实收金额必须大于0");
            return;
        }
        try {
            String transferUrl = weChatProperties.getWxDomain().concat(WeChatTransferConstant.WX_TRANSFER);
            String notifyUrl = weChatProperties.getNotifyUrl().concat("/api/second-hand/transfer/notify");
            SecondHandProduct product = requireProduct(order.getProductId());

            Map<String, Object> paramsMap = new HashMap<>();
            paramsMap.put("appid", weChatProperties.getAppid());
            paramsMap.put("out_bill_no", order.getTransferOutBillNo());
            paramsMap.put("transfer_scene_id", firstNotBlank(
                    weChatProperties.getSecondHandTransferSceneId(),
                    SecondHandConstant.DEFAULT_TRANSFER_SCENE_ID));
            paramsMap.put("openid", sellerOpenid);
            paramsMap.put("transfer_amount", transferAmount);
            paramsMap.put("transfer_remark", "校园二手交易结算");
            paramsMap.put("notify_url", notifyUrl);
            paramsMap.put("user_recv_perception", "二手回收货款");
            paramsMap.put("transfer_scene_report_infos",
                    SecondHandTransferSupport.buildSceneReportInfos(product.getTitle()));

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
                    order.setTransferState("QUERY_REQUIRED");
                    order.setTransferFailReason(readWechatError(bodyAsString));
                    order.setUpdateTime(LocalDateTime.now());
                    orderMapper.update(order);
                    saveTransferLog(order, sellerOpenid, transferAmount, "QUERY_REQUIRED", null, bodyAsString);
                    return;
                }
                Map<String, Object> resultMap = JSONObject.parseObject(bodyAsString, HashMap.class);
                if (resultMap == null) {
                    resultMap = new HashMap<>();
                }
                String state = (String) resultMap.get(WeChatTransferConstant.STATE);
                String transferBillNo = (String) resultMap.get(WeChatTransferConstant.TRANSFER_BILL_NO);
                saveTransferLog(order, sellerOpenid, transferAmount, state, transferBillNo, bodyAsString);
                applyTransferResult(order, resultMap);
            } finally {
                response.close();
            }
        } catch (Exception e) {
            log.error("二手订单收款请求结果不确定, orderNumber={}", order.getOrderNumber());
            order.setTransferState("QUERY_REQUIRED");
            order.setTransferFailReason("收款结果待同步");
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.update(order);
        }
    }

    private void querySellerTransfer(SecondHandOrder order) {
        if (trimToNull(order.getTransferOutBillNo()) == null) {
            return;
        }
        try {
            RealPaymentGuard.require(paymentLogMapper.getByOrderNumber(order.getOrderNumber()),
                    order.getOrderNumber(), order.getPayAmount());
        } catch (com.mikasa.campusrunner.common.exception.OrderException e) {
            log.warn("二手收款来源待核对, orderId={}", order.getId());
            return;
        }
        String queryPath = String.format(
                WeChatTransferConstant.WX_QUERY_TRANSFER_BY_NO,
                order.getTransferOutBillNo());
        HttpGet httpGet = new HttpGet(weChatProperties.getWxDomain().concat(queryPath));
        httpGet.setHeader("Accept", "application/json");
        try (CloseableHttpResponse response = wxPayClient.execute(httpGet)) {
            String body = response.getEntity() == null ? "" : EntityUtils.toString(response.getEntity());
            if (response.getStatusLine().getStatusCode() != 200) {
                log.warn("二手订单收款状态同步未成功, orderNumber={}, statusCode={}",
                        order.getOrderNumber(), response.getStatusLine().getStatusCode());
                return;
            }
            Map<String, Object> result = JSONObject.parseObject(body, HashMap.class);
            if (result == null) {
                return;
            }
            applyTransferResult(order, result);
            String sellerOpenid = userMapper.getOpenidById(order.getSellerId());
            int amount = PaymentAmount.cents(order.getSellerIncome());
            saveTransferLog(order, sellerOpenid, amount,
                    (String) result.get(WeChatTransferConstant.STATE),
                    (String) result.get(WeChatTransferConstant.TRANSFER_BILL_NO), body);
        } catch (Exception e) {
            log.warn("二手订单收款状态同步异常, orderNumber={}", order.getOrderNumber());
        }
    }

    private void applyTransferResult(SecondHandOrder order, Map<String, Object> result) {
        String state = (String) result.get(WeChatTransferConstant.STATE);
        order.setTransferState(state);
        order.setTransferBillNo((String) result.get(WeChatTransferConstant.TRANSFER_BILL_NO));
        if (result.get("package_info") instanceof String packageInfo) {
            order.setTransferPackageInfo(packageInfo);
        }
        if (SecondHandTransferSupport.isSuccess(state)) {
            order.setStatus(SecondHandConstant.ORDER_TRANSFER_SUCCESS);
            order.setTransferTime(LocalDateTime.now());
            order.setTransferFailReason("");
        } else if (SecondHandTransferSupport.isFailure(state)) {
            order.setStatus(SecondHandConstant.ORDER_TRANSFER_FAILED);
            order.setTransferFailReason(friendlyTransferFailure((String) result.get("fail_reason")));
        } else {
            order.setStatus(SecondHandConstant.ORDER_TRANSFERING);
            order.setTransferFailReason("");
        }
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.update(order);
    }

    private void markTransferFailed(SecondHandOrder order, String reason) {
        order.setStatus(SecondHandConstant.ORDER_TRANSFER_FAILED);
        order.setTransferState("FAIL");
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
        if (wxTransferLogMapper.getByOrderNumber(order.getOrderNumber()) == null) {
            wxTransferLogMapper.insert(log);
        } else {
            wxTransferLogMapper.updateByOrderNumber(log);
        }
    }

    private String readWechatError(String body) {
        try {
            Map<String, Object> error = JSONObject.parseObject(body, HashMap.class);
            String message = error == null ? null : (String) error.get("message");
            String value = firstNotBlank(message, "微信暂未受理收款请求");
            return value.length() > 500 ? value.substring(0, 500) : value;
        } catch (Exception ignored) {
            return "微信收款请求未受理";
        }
    }

    private String friendlyTransferFailure(String reason) {
        if (reason == null) {
            return "卖家未完成收款，请同步状态后重试";
        }
        return switch (reason) {
            case "ACCOUNT_FROZEN" -> "卖家微信账户暂时无法收款";
            case "REAL_NAME_CHECK_FAIL", "NAME_NOT_CORRECT" -> "卖家微信实名信息校验未通过";
            case "OPENID_INVALID" -> "卖家微信账号信息已失效，请重新登录后再试";
            case "TRANSFER_QUOTA_EXCEED", "DAY_RECEIVED_QUOTA_EXCEED",
                 "MONTH_RECEIVED_QUOTA_EXCEED", "DAY_RECEIVED_COUNT_EXCEED" ->
                    "卖家微信收款额度已达上限，请稍后再试";
            case "PRODUCT_AUTH_CHECK_FAIL" -> "当前商户暂时无法完成该笔收款";
            default -> "卖家未完成收款，请同步状态后重试";
        };
    }

    private void recordPaymentOnce(String plainText, SecondHandOrder order) {
        Map payment = JSONObject.parseObject(plainText, HashMap.class);
        PaymentAmount.verifyResult(payment, order.getOrderNumber(), order.getPayAmount());
        PaymentLog existing = paymentLogMapper.getByOrderNumber(order.getOrderNumber());
        if (existing == null) savePaymentLog(plainText, order);
        else {
            RealPaymentGuard.require(existing, order.getOrderNumber(), order.getPayAmount());
            if (!existing.getTransactionId().equals(payment.get(WeChatPayConstant.TRANSACTION_ID))) {
                throw new SecondHandException("支付信息核对失败");
            }
        }
    }

    private void requestRefund(SecondHandOrder order) {
        refundRecovery.prepare(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processRefundNotify(Map<String, Object> bodyMap) throws Exception {
        String plainText = decryptFromResource(bodyMap);
        refundRecovery.notifyResult(JSONObject.parseObject(plainText), plainText);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processTransferNotify(Map<String, Object> bodyMap) throws GeneralSecurityException {
        String plainText = decryptFromResource(bodyMap);
        Map plainTextMap = JSONObject.parseObject(plainText, HashMap.class);
        String transferOutBillNo = (String) plainTextMap.get(WeChatTransferConstant.OUT_BILL_NO);
        SecondHandOrder order = orderMapper.getByTransferOutBillNo(transferOutBillNo);
        if (order != null) order = SecondHandOrderLocks.byId(orderMapper, productMapper, order.getId());
        if (order == null) throw new SecondHandException("收款记录尚未确认，请稍后重试");
        if (!isStatus(order.getStatus(), SecondHandConstant.ORDER_TRANSFERING)) {
            return;
        }
        if (!java.util.Objects.equals(transferOutBillNo, order.getTransferOutBillNo())) return;
        RealPaymentGuard.require(paymentLogMapper.getByOrderNumber(order.getOrderNumber()),
                order.getOrderNumber(), order.getPayAmount());
        applyTransferResult(order, plainTextMap);
        WxTransferLog log = WxTransferLog.builder()
                .orderNumber(order.getOrderNumber())
                .transferBillNo((String) plainTextMap.get(WeChatTransferConstant.TRANSFER_BILL_NO))
                .state((String) plainTextMap.get(WeChatTransferConstant.STATE))
                .mchId((String) plainTextMap.get(WeChatTransferConstant.MCH_ID))
                .content(plainText)
                .updateTime(LocalDateTime.now().toString())
                .build();
        wxTransferLogMapper.updateByOrderNumber(log);
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

    private SecondHandProduct requireProductForUpdate(Long id) {
        if (id == null) {
            throw new ParamException(MessageConstant.NOT_FOUND_PARAM);
        }
        // 发起/处理议价与下单统一先锁商品，避免下单后又插入待处理报价。
        SecondHandProduct product = productMapper.getByIdForUpdate(id);
        if (product == null) {
            throw new SecondHandException("商品不存在");
        }
        return product;
    }

    private void expireBargains(SecondHandProduct product, String status, String tip) {
        expireBargains(product, status, tip, null);
    }

    private void expireBargains(SecondHandProduct product, String status, String tip, Long orderingBuyer) {
        // The caller holds the product lock; lock quotes before deciding notification recipients.
        var expiring = bargainMapper.listExpirableForUpdate(product.getId());
        bargainMapper.expirePendingByProduct(product.getId());
        bargainMapper.expireAcceptedWithoutOrder(product.getId());
        Map<Long, Long> recipients = new LinkedHashMap<>();
        for (var quote : expiring) {
            if (quote.getBuyerId() != null && !quote.getBuyerId().equals(orderingBuyer)) {
                recipients.put(quote.getBuyerId(), quote.getId());
            }
        }
        LocalDateTime now = LocalDateTime.now();
        recipients.forEach((buyer, quoteId) -> subscriptions.bargainExpired(
                buyer, product.getId(), product.getTitle(), quoteId, status, tip, now));
    }

    private void ensureQuoteHasNotBeenSuperseded(SecondHandBargain bargain) {
        if (bargain.getCreateTime() != null
                && bargainMapper.hasOrderSinceQuote(bargain.getProductId(), bargain.getCreateTime())) {
            throw new SecondHandException("该报价已失效，请刷新查看");
        }
    }

    private void ensureBargainProductAvailable(SecondHandProduct product) {
        if (!isStatus(product.getStatus(), SecondHandConstant.PRODUCT_ON_SALE)
                || orderMapper.getActiveByProductId(product.getId()) != null) {
            throw new SecondHandException("商品当前不可议价，请刷新后查看");
        }
    }

    private SecondHandOrder requireOrder(Long id) {
        if (id == null) {
            throw new ParamException(MessageConstant.NOT_FOUND_PARAM);
        }
        SecondHandOrder order = SecondHandOrderLocks.byId(orderMapper, productMapper, id);
        if (order == null || Integer.valueOf(1).equals(order.getDeleted())) {
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

    private void validateConversationCounterparty(
            SecondHandProduct product,
            Long userId,
            Long counterpartyId) {
        if (counterpartyId == null || counterpartyId.equals(userId)) {
            throw new SecondHandException("请选择正确的联系人");
        }
        if (product.getSellerId().equals(userId)) {
            if (messageMapper.countProductParticipant(product.getId(), userId, counterpartyId) <= 0) {
                throw new SecondHandException("暂时无法联系该买家");
            }
            return;
        }
        if (!product.getSellerId().equals(counterpartyId)) {
            throw new SecondHandException("只能联系该商品卖家");
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
        validateProductFields(dto);
    }

    /** Shared by creation and partial editing; omitted edit fields keep their current values. */
    private void validateProductFields(SecondHandProductDTO dto) {
        if (dto.getTitle() != null && (dto.getTitle().trim().length() < 2 || dto.getTitle().length() > 80)) {
            throw new ParamException("标题请填写 2 至 80 个字");
        }
        if (dto.getDescription() != null && (dto.getDescription().trim().length() < 4 || dto.getDescription().length() > 600)) {
            throw new ParamException("描述请填写 4 至 600 个字");
        }
        if (dto.getPrice() != null) {
            if (dto.getPrice().signum() <= 0 || dto.getPrice().compareTo(new BigDecimal("99999")) > 0) {
                throw new ParamException("价格须大于 0 且不超过 99999 元");
            }
            try { dto.getPrice().setScale(2, java.math.RoundingMode.UNNECESSARY); }
            catch (ArithmeticException e) { throw new ParamException("价格最多保留两位小数"); }
        }
        if (dto.getCategoryId() != null) {
            SecondHandCategory category = categoryMapper.getByIdForUpdate(dto.getCategoryId());
            if (category == null || Integer.valueOf(1).equals(category.getDeleted())) {
                throw new ParamException("请选择有效的商品分类");
            }
        }
        if (dto.getConditionLevel() != null && !java.util.Set.of("全新", "几乎全新", "轻微使用", "明显使用")
                .contains(dto.getConditionLevel())) {
            throw new ParamException("请选择有效的商品成色");
        }
        if ((dto.getPickupOnly() != null && dto.getPickupOnly() != 0 && dto.getPickupOnly() != 1)
                || (dto.getNegotiable() != null && dto.getNegotiable() != 0 && dto.getNegotiable() != 1)) {
            throw new ParamException("请选择有效的交付与议价设置");
        }
        if (dto.getPickupAddressSnapshot() != null
                && (dto.getPickupAddressSnapshot().isBlank() || dto.getPickupAddressSnapshot().length() > 500)) {
            throw new ParamException("自提点请填写 1 至 500 个字");
        }
        if (dto.getImageAssetIds() != null && (dto.getImageAssetIds().isEmpty() || dto.getImageAssetIds().size() > 6
                || dto.getImageAssetIds().stream().anyMatch(id -> id == null || id <= 0)
                || new java.util.HashSet<>(dto.getImageAssetIds()).size() != dto.getImageAssetIds().size())) {
            throw new ParamException("请选择 1 至 6 张有效的商品图片");
        }
    }

    private void resolveAcceptedOfferPrices(List<SecondHandProductVO> products) {
        products.forEach(product -> product.setAcceptedOfferPrice(null));
        Long buyerId = BaseContext.getCurrentId();
        if (buyerId == null) return;
        var productIds = products.stream()
                .filter(product -> isStatus(product.getStatus(), SecondHandConstant.PRODUCT_ON_SALE)
                        && !buyerId.equals(product.getSellerId()))
                .map(SecondHandProductVO::getId).filter(java.util.Objects::nonNull).distinct().toList();
        if (productIds.isEmpty()) return;
        var prices = new java.util.HashMap<Long, BigDecimal>();
        for (var quote : bargainMapper.listAcceptedByBuyerAndProducts(buyerId, productIds)) {
            prices.putIfAbsent(quote.getProductId(), quote.getOfferPrice());
        }
        products.forEach(product -> product.setAcceptedOfferPrice(prices.get(product.getId())));
    }

    private void resolveProductImagesBatch(List<SecondHandProductVO> products, int maxWidth) {
        var images = mediaAssetService.resolvePublicBindings(MediaAssetConstant.BOUND_SECOND_HAND_PRODUCT,
                products.stream().map(SecondHandProductVO::getId).distinct().toList(),
                MediaPurpose.SECOND_HAND_PRODUCT_IMAGE.name(), maxWidth);
        for (var product : products) {
            var bound = images.getOrDefault(product.getId(), List.of());
            product.setImageAssetIds(bound.stream().map(item -> item.getMediaId()).toList());
            product.setImages(String.join(",", bound.stream().map(item -> item.getUrl()).toList()));
        }
    }

    private SecondHandProductVO resolveProductImages(SecondHandProductVO product) {
        return resolveProductImages(product, 0);
    }

    private SecondHandBargain requireBargainForUpdate(Long id) {
        if (id == null) {
            throw new ParamException(MessageConstant.NOT_FOUND_PARAM);
        }
        // 撤回只锁议价，不再申请商品锁；与接受/下单竞争议价行时不会形成反向锁序。
        SecondHandBargain bargain = bargainMapper.getByIdForUpdate(id);
        if (bargain == null) {
            throw new SecondHandException("议价不存在");
        }
        return bargain;
    }

    private SecondHandProductVO resolveProductImages(SecondHandProductVO product, int maxWidth) {
        if (product == null) {
            return null;
        }
        var images = mediaAssetService.resolvePublicBinding(
                MediaAssetConstant.BOUND_SECOND_HAND_PRODUCT,
                product.getId(),
                MediaPurpose.SECOND_HAND_PRODUCT_IMAGE.name(), maxWidth);
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

    private void cancelOfflineOrder(SecondHandOrder order, String reason) {
        Long userId = BaseContext.getCurrentId();
        boolean isBuyer = order.getBuyerId().equals(userId);
        boolean isSeller = order.getSellerId().equals(userId);
        if (!isBuyer && !isSeller) {
            throw new SecondHandException("只有交易双方可以取消订单");
        }
        if (isStatus(order.getStatus(), SecondHandConstant.ORDER_CANCELED)) {
            return;
        }
        if (!isStatus(order.getStatus(), SecondHandConstant.ORDER_OFFLINE_WAIT_DELIVERY)) {
            throw new SecondHandException("卖家标记交付后不可自行取消，请先与对方沟通");
        }
        LocalDateTime now = LocalDateTime.now();
        String defaultReason = isBuyer ? "买家取消线下交易" : "卖家取消线下交易";
        order.setStatus(SecondHandConstant.ORDER_CANCELED);
        order.setCancelReason(firstNotBlank(reason, defaultReason));
        order.setCancelTime(now);
        order.setUpdateTime(now);
        orderMapper.update(order);
        productMapper.releaseRefundedProduct(order.getProductId());
        subscriptions.order(order, isBuyer ? order.getSellerId() : order.getBuyerId(), "已取消",
                isBuyer ? "买家已取消订单" : "卖家已取消订单", now);
    }

    private void adminUpdateOfflineOrderStatus(
            SecondHandOrder order,
            Integer status,
            String reason,
            LocalDateTime now) {
        if (!isStatus(status, SecondHandConstant.ORDER_OFFLINE_WAIT_DELIVERY) &&
                !isStatus(status, SecondHandConstant.ORDER_DELIVERED_WAIT_CONFIRM) &&
                !isStatus(status, SecondHandConstant.ORDER_COMPLETED) &&
                !isStatus(status, SecondHandConstant.ORDER_CANCELED) &&
                !isStatus(status, SecondHandConstant.ORDER_DISPUTE)) {
            throw new SecondHandException("线下交易订单不支持支付、退款或收款状态");
        }
        order.setStatus(status);
        order.setUpdateTime(now);
        if (isStatus(status, SecondHandConstant.ORDER_OFFLINE_WAIT_DELIVERY)) {
            productMapper.markTrading(order.getProductId());
        } else if (isStatus(status, SecondHandConstant.ORDER_DELIVERED_WAIT_CONFIRM)) {
            order.setDeliveredTime(order.getDeliveredTime() == null ? now : order.getDeliveredTime());
            productMapper.markTrading(order.getProductId());
        } else if (isStatus(status, SecondHandConstant.ORDER_COMPLETED)) {
            order.setFinishTime(order.getFinishTime() == null ? now : order.getFinishTime());
            productMapper.markSold(order.getProductId());
        } else if (isStatus(status, SecondHandConstant.ORDER_CANCELED)) {
            order.setCancelTime(order.getCancelTime() == null ? now : order.getCancelTime());
            order.setCancelReason(reason == null ? "管理员取消线下交易" : reason);
            productMapper.releaseRefundedProduct(order.getProductId());
        } else {
            order.setCancelReason(reason == null ? "管理员标记协商中" : reason);
        }
        orderMapper.update(order);
    }

    private void validateAdminTransition(SecondHandOrder order, Integer target) {
        int source = order.getStatus();
        boolean allowed;
        if (isOfflineOrder(order)) {
            allowed = (source == SecondHandConstant.ORDER_OFFLINE_WAIT_DELIVERY
                    && java.util.Set.of(SecondHandConstant.ORDER_DELIVERED_WAIT_CONFIRM,
                    SecondHandConstant.ORDER_CANCELED, SecondHandConstant.ORDER_DISPUTE).contains(target))
                    || (source == SecondHandConstant.ORDER_DELIVERED_WAIT_CONFIRM
                    && java.util.Set.of(SecondHandConstant.ORDER_COMPLETED, SecondHandConstant.ORDER_DISPUTE).contains(target))
                    || (source == SecondHandConstant.ORDER_DISPUTE
                    && java.util.Set.of(SecondHandConstant.ORDER_DELIVERED_WAIT_CONFIRM,
                    SecondHandConstant.ORDER_COMPLETED, SecondHandConstant.ORDER_CANCELED).contains(target));
        } else {
            boolean refundTarget = java.util.Set.of(SecondHandConstant.ORDER_CANCELED,
                    SecondHandConstant.ORDER_REFUNDING, SecondHandConstant.ORDER_REFUND_ABNORMAL,
                    SecondHandConstant.ORDER_REFUND_SUCCESS).contains(target);
            allowed = (refundTarget && java.util.Set.of(SecondHandConstant.ORDER_PENDING_PAY,
                    SecondHandConstant.ORDER_PAID_WAIT_DELIVERY, SecondHandConstant.ORDER_CANCELED,
                    SecondHandConstant.ORDER_REFUNDING, SecondHandConstant.ORDER_REFUND_ABNORMAL).contains(source))
                    || (source == SecondHandConstant.ORDER_PAID_WAIT_DELIVERY
                    && target == SecondHandConstant.ORDER_DELIVERED_WAIT_CONFIRM)
                    || (source == SecondHandConstant.ORDER_DELIVERED_WAIT_CONFIRM
                    && target == SecondHandConstant.ORDER_TRANSFERING)
                    || (java.util.Set.of(SecondHandConstant.ORDER_PAID_WAIT_DELIVERY,
                    SecondHandConstant.ORDER_DELIVERED_WAIT_CONFIRM).contains(source)
                    && target == SecondHandConstant.ORDER_DISPUTE)
                    || (source == SecondHandConstant.ORDER_DISPUTE
                    && target == SecondHandConstant.ORDER_DELIVERED_WAIT_CONFIRM);
        }
        if (!allowed) throw new SecondHandException("订单状态已变化，当前操作不可用，请刷新后查看");
    }

    private String currentTradeMode() {
        SystemConfig config = configMapper.getByConfigKey(SecondHandConstant.CONFIG_TRADE_MODE);
        String configured = config == null ? null : trimToNull(config.getConfigValue());
        return SecondHandConstant.TRADE_MODE_ONLINE.equalsIgnoreCase(configured)
                ? SecondHandConstant.TRADE_MODE_ONLINE
                : SecondHandConstant.DEFAULT_TRADE_MODE;
    }

    private boolean isOfflineOrder(SecondHandOrder order) {
        return order != null && isOfflineTradeMode(order.getTradeMode());
    }

    private boolean isOfflineTradeMode(String tradeMode) {
        return SecondHandConstant.TRADE_MODE_OFFLINE.equalsIgnoreCase(tradeMode);
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
        var images = mediaAssetService.resolvePublicBindings(MediaAssetConstant.BOUND_SECOND_HAND_PRODUCT,
                orders.stream().map(SecondHandOrderVO::getProductId).filter(java.util.Objects::nonNull).distinct().toList(),
                MediaPurpose.SECOND_HAND_PRODUCT_IMAGE.name(), 0);
        Integer minutes = orders.stream().anyMatch(order -> !isOfflineTradeMode(order.getTradeMode())
                && isStatus(order.getStatus(), SecondHandConstant.ORDER_PENDING_PAY) && order.getCreateTime() != null)
                ? getIntConfig("second_hand_payment_timeout_minutes", SecondHandConstant.DEFAULT_PAYMENT_TIMEOUT_MINUTES) : null;
        return orders.stream().map(order -> enrichOrder(order,
                images.getOrDefault(order.getProductId(), List.of()), minutes)).toList();
    }

    private void resolveCounterpartyAvatars(List<SecondHandOrderVO> orders, boolean sellerView) {
        var avatars = mediaAssetService.resolvePublicBindings(MediaAssetConstant.BOUND_USER_AVATAR,
                orders.stream().map(order -> sellerView ? order.getBuyerId() : order.getSellerId())
                        .filter(java.util.Objects::nonNull).distinct().toList(), MediaPurpose.AVATAR.name(), 0);
        for (var order : orders) {
            Long id = sellerView ? order.getBuyerId() : order.getSellerId();
            var bound = avatars.getOrDefault(id, List.of());
            String avatar = bound.isEmpty() ? "" : bound.get(0).getUrl();
            if (sellerView) order.setBuyerAvatar(avatar);
            else order.setSellerAvatar(avatar);
        }
    }

    private SecondHandOrderVO enrichOrder(SecondHandOrderVO order) {
        if (order == null) {
            return null;
        }
        var images = mediaAssetService.resolvePublicBinding(
                MediaAssetConstant.BOUND_SECOND_HAND_PRODUCT,
                order.getProductId(),
                MediaPurpose.SECOND_HAND_PRODUCT_IMAGE.name());
        return enrichOrder(order, images, null);
    }

    private SecondHandOrderVO enrichOrder(SecondHandOrderVO order,
            List<com.mikasa.campusrunner.pojo.vo.BoundMediaVO> images, Integer timeoutMinutes) {
        if (!images.isEmpty()) {
            order.setProductImageAssetIds(images.stream().map(item -> item.getMediaId()).toList());
            order.setProductImages(String.join(",", images.stream().map(item -> item.getUrl()).toList()));
        } else {
            order.setProductImages("");
        }
        if (!isOfflineTradeMode(order.getTradeMode()) &&
                isStatus(order.getStatus(), SecondHandConstant.ORDER_PENDING_PAY) &&
                order.getCreateTime() != null) {
            int minutes = timeoutMinutes != null ? timeoutMinutes : getIntConfig("second_hand_payment_timeout_minutes", SecondHandConstant.DEFAULT_PAYMENT_TIMEOUT_MINUTES);
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
