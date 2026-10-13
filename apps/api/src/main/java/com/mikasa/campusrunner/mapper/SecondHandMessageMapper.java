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

    List<com.mikasa.campusrunner.pojo.vo.SecondHandConversationVO> listConversationsPage(@Param("userId") Long userId,
            @Param("query") com.mikasa.campusrunner.pojo.dto.ListPageDTO query);
    SecondHandMessageVO getForUser(@Param("id") Long id, @Param("userId") Long userId);
    SecondHandMessageVO getCursorForUser(@Param("id") Long id, @Param("userId") Long userId);
    List<SecondHandMessageVO> listMessagePage(@Param("productId") Long productId,
            @Param("userId") Long userId, @Param("counterpartyId") Long counterpartyId,
            @Param("query") com.mikasa.campusrunner.pojo.dto.MessagePageDTO query,
            @Param("cursor") SecondHandMessageVO cursor);
    void markMessagesRead(@Param("userId") Long userId, @Param("ids") List<Long> ids);
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
    List<SecondHandMessageVO> listAdminPage(@Param("query") com.mikasa.campusrunner.pojo.dto.AdminSecondHandMessageQueryDTO query);
    long countAdmin(@Param("query") com.mikasa.campusrunner.pojo.dto.AdminSecondHandMessageQueryDTO query);
}
