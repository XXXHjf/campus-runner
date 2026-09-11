package com.mikasa.campusrunner.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface SecondHandFavoriteMapper {
    int insertIgnore(@Param("userId") Long userId,
                     @Param("productId") Long productId,
                     @Param("createTime") LocalDateTime createTime);

    int delete(@Param("userId") Long userId,
               @Param("productId") Long productId);
}
