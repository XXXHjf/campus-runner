package com.mikasa.campusrunner.pojo.vo;

import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    @JsonIgnore private Long accessUserId;
    @JsonIgnore private Long mediaProductId;
}
