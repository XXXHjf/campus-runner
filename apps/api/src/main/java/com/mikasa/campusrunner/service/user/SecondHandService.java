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

    SecondHandCategory saveCategory(AdminSecondHandCategoryDTO category);

    void updateCategory(Long id, AdminSecondHandCategoryDTO category);

    void deleteCategory(Long id);

    SecondHandProductVO publishProduct(SecondHandProductDTO dto);

    void updateProduct(Long id, SecondHandProductDTO dto);

    void updateProductStatus(Long id, Integer status);

    void adminUpdateProductStatus(Long id, Integer status);

    List<SecondHandProductVO> listProducts(SecondHandProductQueryDTO query);

    List<SecondHandProductVO> listMyProducts();

    SecondHandProductVO productDetail(Long id);

    SecondHandBargainVO createBargain(SecondHandBargainDTO dto);

    SecondHandOrderVO acceptBargain(Long bargainId, SecondHandOrderCreateDTO dto);

    void rejectBargain(Long bargainId);

    List<SecondHandBargainVO> listMyBargains();

    List<SecondHandBargainVO> listProductBargains(Long productId);

    SecondHandOrderVO createOrder(SecondHandOrderCreateDTO dto);

    List<SecondHandOrderVO> listBuyerOrders();

    List<SecondHandOrderVO> listSellerOrders();

    SecondHandOrderVO orderDetail(Long id);

    void markPaid(String orderNumber);

    void cancelOrder(Long id, String reason);

    void markDelivered(Long id);

    void confirmOrder(Long id);

    SecondHandMessageVO createMessage(SecondHandMessageDTO dto);

    List<SecondHandMessageVO> listProductMessages(Long productId);

    List<SecondHandProductVO> adminListProducts(SecondHandProductQueryDTO query);

    SecondHandProductVO adminProductDetail(Long id);

    void adminDeleteProduct(Long id);

    List<SecondHandOrderVO> adminListOrders(Integer status);

    SecondHandOrderVO adminOrderDetail(Long id);

    void adminUpdateOrderStatus(Long id, SecondHandStatusDTO dto);

    void adminRetryTransfer(Long id);

    List<SecondHandBargainVO> adminListBargains();

    void adminUpdateBargainStatus(Long id, SecondHandStatusDTO dto);

    List<SecondHandMessageVO> adminListMessages(Long productId);

    void processUnpaidTimeouts();

    void processAutoConfirm();

    WeChatPrePayVO jsapiPay(Long orderId) throws Exception;

    void processPayNotify(Map<String, Object> bodyMap) throws GeneralSecurityException;

    void processTransferNotify(Map<String, Object> bodyMap) throws GeneralSecurityException;

    void processRefundNotify(Map<String, Object> bodyMap) throws Exception;
}
