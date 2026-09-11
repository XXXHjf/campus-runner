package com.mikasa.campusrunner.mapper;

import com.mikasa.campusrunner.pojo.entity.SecondHandMessage;
import com.mikasa.campusrunner.pojo.vo.SecondHandMessageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SecondHandMessageMapper {
    void insert(SecondHandMessage message);

    List<SecondHandMessageVO> listByProductForUser(@Param("productId") Long productId,
                                                   @Param("userId") Long userId);

    List<SecondHandMessageVO> listForUser(@Param("userId") Long userId);

    List<SecondHandMessageVO> listConversation(@Param("productId") Long productId,
                                               @Param("userId") Long userId,
                                               @Param("counterpartyId") Long counterpartyId);

    void markConversationRead(@Param("productId") Long productId,
                              @Param("userId") Long userId,
                              @Param("counterpartyId") Long counterpartyId);

    int countProductParticipant(@Param("productId") Long productId,
                                @Param("sellerId") Long sellerId,
                                @Param("userId") Long userId);

    List<SecondHandMessageVO> listAdmin(@Param("productId") Long productId);
}
