package com.mikasa.campusrunner.mapper;

import com.mikasa.campusrunner.pojo.entity.SecondHandBargain;
import com.mikasa.campusrunner.pojo.vo.SecondHandBargainVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SecondHandBargainMapper {
    void expireAcceptedWithoutOrder(@org.apache.ibatis.annotations.Param("productId") Long productId);
    void insert(SecondHandBargain bargain);

    void update(SecondHandBargain bargain);

    int updatePendingStatus(@Param("id") Long id, @Param("status") Integer status);
    int rejectPending(@Param("id") Long id, @Param("reasonCode") String reasonCode, @Param("note") String note);
    List<SecondHandBargain> listExpirableForUpdate(@Param("productId") Long productId);
    boolean hasOrderSinceQuote(@Param("productId") Long productId,
            @Param("createTime") java.time.LocalDateTime createTime);

    SecondHandBargain getById(@Param("id") Long id);

    SecondHandBargain getByIdForUpdate(@Param("id") Long id);

    int countByBuyerAndProduct(@Param("buyerId") Long buyerId, @Param("productId") Long productId);

    List<SecondHandBargain> listActiveByBuyerAndProductForUpdate(@Param("buyerId") Long buyerId,
            @Param("productId") Long productId);

    void expirePendingByBuyerAndProduct(@Param("buyerId") Long buyerId, @Param("productId") Long productId);

    void expirePendingByProduct(@Param("productId") Long productId);

    List<SecondHandBargainVO> listByProduct(@Param("productId") Long productId);

    List<SecondHandBargainVO> listMine(@Param("userId") Long userId);

    List<SecondHandBargainVO> listMinePage(@Param("userId") Long userId,
            @Param("query") com.mikasa.campusrunner.pojo.dto.BargainPageDTO query);
    List<SecondHandBargainVO> listByProductPage(@Param("productId") Long productId,
            @Param("buyerId") Long buyerId,
            @Param("query") com.mikasa.campusrunner.pojo.dto.BargainPageDTO query);

    List<SecondHandBargainVO> listAdmin();
    List<SecondHandBargainVO> listAdminPage(@Param("query") com.mikasa.campusrunner.pojo.dto.ListPageDTO query);
    com.mikasa.campusrunner.pojo.vo.AdminSecondHandBargainStatsVO summarizeAdmin();
}
