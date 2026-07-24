package com.mikasa.campusrunner.mapper;

import com.mikasa.campusrunner.pojo.entity.SecondHandBargain;
import com.mikasa.campusrunner.pojo.vo.SecondHandBargainVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SecondHandBargainMapper {
    void insert(SecondHandBargain bargain);

    void update(SecondHandBargain bargain);

    SecondHandBargain getById(@Param("id") Long id);

    int countByBuyerAndProduct(@Param("buyerId") Long buyerId, @Param("productId") Long productId);

    void expirePendingByProduct(@Param("productId") Long productId);

    List<SecondHandBargainVO> listByProduct(@Param("productId") Long productId);

    List<SecondHandBargainVO> listMine(@Param("userId") Long userId);

    List<SecondHandBargainVO> listAdmin();
}
