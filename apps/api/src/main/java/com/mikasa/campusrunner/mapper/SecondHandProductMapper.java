package com.mikasa.campusrunner.mapper;

import com.mikasa.campusrunner.pojo.dto.SecondHandProductQueryDTO;
import com.mikasa.campusrunner.pojo.entity.SecondHandProduct;
import com.mikasa.campusrunner.pojo.vo.SecondHandProductVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SecondHandProductMapper {
    @org.apache.ibatis.annotations.Update("""
            update tb_second_hand_product set status = 0, update_time = now()
            where id = #{productId} and deleted = 0 and status in (1, 2)
            and not exists (select 1 from tb_second_hand_order o where o.product_id = #{productId}
                and o.id != #{orderId} and o.status not in (4, 6))
            """)
    int releaseAfterRefund(@org.apache.ibatis.annotations.Param("productId") Long productId,
            @org.apache.ibatis.annotations.Param("orderId") Long orderId);
    void insert(SecondHandProduct product);

    void update(SecondHandProduct product);

    SecondHandProduct getByIdForUpdate(@Param("id") Long id);

    SecondHandProduct getById(@Param("id") Long id);

    SecondHandProductVO detail(@Param("id") Long id,
                               @Param("userId") Long userId);

    List<SecondHandProductVO> list(@Param("query") SecondHandProductQueryDTO query,
                                   @Param("schoolId") Long schoolId);

    long countProducts(@Param("query") SecondHandProductQueryDTO query, @Param("onSaleOnly") boolean onSaleOnly, @Param("schoolId") Long schoolId);

    List<SecondHandProductVO> listPage(@Param("query") SecondHandProductQueryDTO query,
                                      @Param("schoolId") Long schoolId);

    List<SecondHandProductVO> listBySeller(@Param("sellerId") Long sellerId,
                                         @Param("query") SecondHandProductQueryDTO query);

    List<SecondHandProductVO> listFavorites(@Param("userId") Long userId,
                                          @Param("query") SecondHandProductQueryDTO query);

    int lockOnSaleProduct(@Param("id") Long id);

    int markTrading(@Param("id") Long id);

    int releaseLockedProduct(@Param("id") Long id);

    int releaseRefundedProduct(@Param("id") Long id);

    int markSold(@Param("id") Long id);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    void increaseViewCount(@Param("id") Long id);

    int changeFavoriteCount(@Param("id") Long id,
                            @Param("delta") int delta);
}
