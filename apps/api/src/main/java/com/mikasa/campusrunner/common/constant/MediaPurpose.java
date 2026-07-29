package com.mikasa.campusrunner.common.constant;

import com.mikasa.campusrunner.common.exception.UploadException;

import java.util.Set;

public enum MediaPurpose {
    ORDER_CATEGORY_ICON(
            "order/category",
            MediaAssetConstant.VISIBILITY_PUBLIC,
            2L * 1024 * 1024,
            4096,
            Set.of("image/jpeg", "image/png", "image/webp")),
    SECOND_HAND_CATEGORY_ICON(
            "second-hand/category",
            MediaAssetConstant.VISIBILITY_PUBLIC,
            2L * 1024 * 1024,
            4096,
            Set.of("image/jpeg", "image/png", "image/webp")),
    SECOND_HAND_PRODUCT_IMAGE(
            "second-hand/product",
            MediaAssetConstant.VISIBILITY_PUBLIC,
            2L * 1024 * 1024,
            4096,
            Set.of("image/jpeg", "image/png", "image/webp")),
    ORDER_IMAGE(
            "order/content",
            MediaAssetConstant.VISIBILITY_PRIVATE,
            2L * 1024 * 1024,
            4096,
            Set.of("image/jpeg", "image/png", "image/webp")),
    DELIVERY_PROOF(
            "order/delivery-proof",
            MediaAssetConstant.VISIBILITY_PRIVATE,
            2L * 1024 * 1024,
            4096,
            Set.of("image/jpeg", "image/png", "image/webp")),
    AVATAR(
            "user/avatar",
            MediaAssetConstant.VISIBILITY_PUBLIC,
            2L * 1024 * 1024,
            4096,
            Set.of("image/jpeg", "image/png", "image/webp")),
    STUDENT_CARD(
            "user/student-card",
            MediaAssetConstant.VISIBILITY_PRIVATE,
            2L * 1024 * 1024,
            4096,
            Set.of("image/jpeg", "image/png", "image/webp")),
    PAYMENT_QR(
            "user/payment-qr",
            MediaAssetConstant.VISIBILITY_PRIVATE,
            2L * 1024 * 1024,
            4096,
            Set.of("image/jpeg", "image/png", "image/webp")),
    BANNER(
            "banner",
            MediaAssetConstant.VISIBILITY_PUBLIC,
            2L * 1024 * 1024,
            4096,
            Set.of("image/jpeg", "image/png", "image/webp"));

    private final String objectPrefix;
    private final String visibility;
    private final long maxBytes;
    private final int maxDimension;
    private final Set<String> allowedMimeTypes;

    MediaPurpose(
            String objectPrefix,
            String visibility,
            long maxBytes,
            int maxDimension,
            Set<String> allowedMimeTypes) {
        this.objectPrefix = objectPrefix;
        this.visibility = visibility;
        this.maxBytes = maxBytes;
        this.maxDimension = maxDimension;
        this.allowedMimeTypes = allowedMimeTypes;
    }

    public static MediaPurpose from(String value) {
        if (value == null || value.isBlank()) {
            throw new UploadException("缺少图片用途");
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new UploadException("不支持的图片用途");
        }
    }

    public String getObjectPrefix() {
        return objectPrefix;
    }

    public String getVisibility() {
        return visibility;
    }

    public long getMaxBytes() {
        return maxBytes;
    }

    public int getMaxDimension() {
        return maxDimension;
    }

    public boolean allows(String mimeType) {
        return allowedMimeTypes.contains(mimeType);
    }

    public boolean allowsOwnerType(String ownerType) {
        if (MediaAssetConstant.OWNER_ADMIN.equals(ownerType)) {
            return this == ORDER_CATEGORY_ICON || this == SECOND_HAND_CATEGORY_ICON || this == BANNER;
        }
        if (MediaAssetConstant.OWNER_USER.equals(ownerType)) {
            return this != ORDER_CATEGORY_ICON && this != SECOND_HAND_CATEGORY_ICON && this != BANNER;
        }
        return false;
    }
}
