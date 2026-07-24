package com.mikasa.campusrunner.mapper;

import com.mikasa.campusrunner.pojo.dto.SecondHandProductQueryDTO;
import com.mikasa.campusrunner.pojo.entity.SecondHandProduct;
import com.mikasa.campusrunner.pojo.vo.SecondHandProductVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SecondHandProductMapper {
    void insert(SecondHandProduct product);

    void update(SecondHandProduct product);

    SecondHandProduct getById(@Param("id") Long id);

    SecondHandProductVO detail(@Param("id") Long id);

    List<SecondHandProductVO> list(@Param("query") SecondHandProductQueryDTO query,
                                   @Param("schoolId") Long schoolId);

    List<SecondHandProductVO> listBySeller(@Param("sellerId") Long sellerId);

    int lockOnSaleProduct(@Param("id") Long id);

    int markTrading(@Param("id") Long id);

    int releaseLockedProduct(@Param("id") Long id);

    int releaseRefundedProduct(@Param("id") Long id);

    int markSold(@Param("id") Long id);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    void increaseViewCount(@Param("id") Long id);
}
