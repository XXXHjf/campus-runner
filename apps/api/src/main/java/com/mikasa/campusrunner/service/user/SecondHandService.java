package com.mikasa.campusrunner.service.user;

import com.mikasa.campusrunner.pojo.dto.*;
import com.mikasa.campusrunner.pojo.dto.admin.AdminSecondHandCategoryDTO;
import com.mikasa.campusrunner.pojo.entity.SecondHandCategory;
import com.mikasa.campusrunner.pojo.entity.SecondHandOrder;
import com.mikasa.campusrunner.pojo.vo.*;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;

public interface SecondHandService {
    List<SecondHandCategory> listCategories();
    List<SecondHandCategory> listCategories(com.mikasa.campusrunner.pojo.dto.ListPageDTO page);

    SecondHandCategory saveCategory(AdminSecondHandCategoryDTO category);

    void updateCategory(Long id, AdminSecondHandCategoryDTO category);

    void deleteCategory(Long id);

    SecondHandProductVO publishProduct(SecondHandProductDTO dto);

    void updateProduct(Long id, SecondHandProductDTO dto);

    void updateProductStatus(Long id, Integer status);

    void adminUpdateProductStatus(Long id, Integer status);

    List<SecondHandProductVO> listProducts(SecondHandProductQueryDTO query);

    List<SecondHandProductVO> listMyProducts();
    List<SecondHandProductVO> listMyProducts(SecondHandProductQueryDTO query);

    SecondHandProductVO productDetail(Long id);

    List<SecondHandProductVO> listFavoriteProducts();
    List<SecondHandProductVO> listFavoriteProducts(SecondHandProductQueryDTO query);

    void favoriteProduct(Long id);

    void unfavoriteProduct(Long id);

    SecondHandBargainVO createBargain(SecondHandBargainDTO dto);

    void acceptBargain(Long bargainId, SecondHandOrderCreateDTO dto);

    void rejectBargain(Long bargainId);
    void rejectBargain(Long bargainId, com.mikasa.campusrunner.pojo.dto.SecondHandBargainRejectDTO dto);

    void withdrawBargain(Long bargainId);

    List<SecondHandBargainVO> listMyBargains();
    List<SecondHandBargainVO> listMyBargains(com.mikasa.campusrunner.pojo.dto.BargainPageDTO query);

    List<SecondHandBargainVO> listProductBargains(Long productId);
    List<SecondHandBargainVO> listProductBargains(Long productId, com.mikasa.campusrunner.pojo.dto.BargainPageDTO query);

    SecondHandOrderVO createOrder(SecondHandOrderCreateDTO dto);

    List<SecondHandOrderVO> listBuyerOrders();
    List<SecondHandOrderVO> listBuyerOrders(SecondHandOrderQueryDTO query);

    List<SecondHandOrderVO> listSellerOrders();
    List<SecondHandOrderVO> listSellerOrders(SecondHandOrderQueryDTO query);

    SecondHandOrderVO orderDetail(Long id);

    void markPaid(String orderNumber);

    void cancelOrder(Long id, String reason);

    void markDelivered(Long id);

    void confirmOrder(Long id);

    SecondHandMessageVO createMessage(SecondHandMessageDTO dto);

    List<SecondHandMessageVO> listProductMessages(Long productId);
    List<SecondHandMessageVO> listProductMessages(Long productId, com.mikasa.campusrunner.pojo.dto.MessagePageDTO query);

    List<SecondHandConversationVO> listConversations();
    List<SecondHandConversationVO> listConversations(com.mikasa.campusrunner.pojo.dto.ListPageDTO query);

    List<SecondHandMessageVO> listConversationMessages(Long productId, Long counterpartyId);
    List<SecondHandMessageVO> listConversationMessages(Long productId, Long counterpartyId,
            com.mikasa.campusrunner.pojo.dto.MessagePageDTO query);

    SecondHandTransferClaimVO getTransferClaim(Long orderId);

    com.mikasa.campusrunner.pojo.vo.AdminSecondHandProductPageVO adminListProducts(SecondHandProductQueryDTO query);

    SecondHandProductVO adminProductDetail(Long id);

    void adminDeleteProduct(Long id);

    com.mikasa.campusrunner.pojo.vo.AdminSecondHandOrderPageVO adminListOrders(com.mikasa.campusrunner.pojo.dto.AdminSecondHandOrderQueryDTO query);

    SecondHandOrderVO adminOrderDetail(Long id);

    void adminUpdateOrderStatus(Long id, SecondHandStatusDTO dto);

    void adminRetryTransfer(Long id);

    com.mikasa.campusrunner.pojo.vo.AdminSecondHandBargainPageVO adminListBargains(com.mikasa.campusrunner.pojo.dto.ListPageDTO query);

    void adminUpdateBargainStatus(Long id, SecondHandStatusDTO dto);

    com.mikasa.campusrunner.pojo.dto.PageResult<SecondHandMessageVO> adminListMessages(com.mikasa.campusrunner.pojo.dto.AdminSecondHandMessageQueryDTO query);

    void processUnpaidTimeouts();

    void processAutoConfirm();

    void processTransferQueries();

    WeChatPrePayVO jsapiPay(Long orderId) throws Exception;

    void processPayNotify(Map<String, Object> bodyMap) throws GeneralSecurityException;

    void processTransferNotify(Map<String, Object> bodyMap) throws GeneralSecurityException;

    void processRefundNotify(Map<String, Object> bodyMap) throws Exception;
}
