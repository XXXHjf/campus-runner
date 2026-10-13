package com.mikasa.campusrunner.mapper;

import com.mikasa.campusrunner.pojo.entity.MediaAsset;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MediaAssetMapper {
    @org.apache.ibatis.annotations.Update("update tb_media_asset set file_size=#{fileSize}, mime_type=#{mimeType}, width=#{width}, height=#{height}, update_time=now() where id=#{id} and status='UPLOADING' and delete_after>now()")
    int updateUploadingMetadata(MediaAsset asset);
    void insert(MediaAsset asset);

    MediaAsset getById(@Param("id") Long id);

    MediaAsset getByIdForUpdate(@Param("id") Long id);

    int countActiveTemporary(
            @Param("ownerType") String ownerType,
            @Param("ownerId") Long ownerId);

    int markTemporary(
            @Param("id") Long id,
            @Param("expiresAt") LocalDateTime expiresAt,
            @Param("updateTime") LocalDateTime updateTime);

    int markUploadFailed(
            @Param("id") Long id,
            @Param("lastError") String lastError,
            @Param("deleteAfter") LocalDateTime deleteAfter,
            @Param("updateTime") LocalDateTime updateTime);

    int bind(
            @Param("id") Long id,
            @Param("boundType") String boundType,
            @Param("boundId") Long boundId,
            @Param("sortOrder") Integer sortOrder,
            @Param("boundAt") LocalDateTime boundAt);

    int updateBoundSort(
            @Param("id") Long id,
            @Param("boundType") String boundType,
            @Param("boundId") Long boundId,
            @Param("sortOrder") Integer sortOrder,
            @Param("updateTime") LocalDateTime updateTime);

    List<MediaAsset> listBoundAssets(
            @Param("boundType") String boundType,
            @Param("boundId") Long boundId,
            @Param("purpose") String purpose);

    List<MediaAsset> listPublicBoundAssetsBatch(
            @Param("boundType") String boundType,
            @Param("boundIds") List<Long> boundIds,
            @Param("purpose") String purpose);

    List<MediaAsset> listBoundAssetsBatch(
            @Param("boundType") String boundType,
            @Param("boundIds") List<Long> boundIds,
            @Param("purpose") String purpose);

    List<MediaAsset> listOrderPreviewAssetsBatch(@Param("orderIds") List<Long> orderIds);

    int scheduleTemporaryDeletion(
            @Param("id") Long id,
            @Param("ownerType") String ownerType,
            @Param("ownerId") Long ownerId,
            @Param("deleteAfter") LocalDateTime deleteAfter,
            @Param("updateTime") LocalDateTime updateTime);

    int scheduleBoundDeletion(
            @Param("id") Long id,
            @Param("deleteAfter") LocalDateTime deleteAfter,
            @Param("updateTime") LocalDateTime updateTime);

    List<Long> findDeletionCandidates(
            @Param("now") LocalDateTime now,
            @Param("limit") int limit);

    int claimForDeletion(
            @Param("id") Long id,
            @Param("now") LocalDateTime now);

    int markDeleted(
            @Param("id") Long id,
            @Param("updateTime") LocalDateTime updateTime);

    int postponeDeletion(
            @Param("id") Long id,
            @Param("lastError") String lastError,
            @Param("deleteAfter") LocalDateTime deleteAfter,
            @Param("updateTime") LocalDateTime updateTime);
}
