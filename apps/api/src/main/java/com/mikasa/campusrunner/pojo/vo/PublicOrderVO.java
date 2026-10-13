package com.mikasa.campusrunner.pojo.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Browsable task information; contacts and address-book details remain excluded. */
public record PublicOrderVO(Long id, BigDecimal price, BigDecimal productAmount, String businessType,
                            LocalDateTime createTime, LocalDateTime expectedDeliveryTime, Integer gap, Long categoryId, String categoryName,
                            String pickUpAddress, String reciveAddress,
                            Long userId, String username, String note, Integer doorAccess) {}
