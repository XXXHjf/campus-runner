package com.mikasa.campusrunner.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SecondHandConversationVO {
    private Long productId;
    private String productTitle;
    private String productImages;
    private Long counterpartyId;
    private String counterpartyName;
    private Long orderId;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private Integer unreadCount;
}
