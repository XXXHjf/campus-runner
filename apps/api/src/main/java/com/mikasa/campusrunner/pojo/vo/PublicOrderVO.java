package com.mikasa.campusrunner.pojo.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Public preview deliberately excludes contacts, free text, photos and address details. */
public record PublicOrderVO(Long id, BigDecimal price, LocalDateTime createTime, Integer gap,
                            Long categoryId, String categoryName,
                            String pickUpAddress, String reciveAddress) {}
