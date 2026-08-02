package com.mikasa.campusrunner.common.constant;

import com.mikasa.campusrunner.common.exception.UploadException;

import java.util.Set;

public enum MediaPurpose {
    ORDER_CATEGORY_ICON(
            "order/category",
            MediaAssetConstant.VISIBILITY_PUBLIC,
            ImageOutputFormat.PNG,
            512,
            512,
            1.0f,
            512L * 1024,
            Set.of("image/jpeg", "image/png", "image/webp")),
    SECOND_HAND_CATEGORY_ICON(
            "second-hand/category",
            MediaAssetConstant.VISIBILITY_PUBLIC,
            ImageOutputFormat.PNG,
            512,
            512,
            1.0f,
            512L * 1024,
            Set.of("image/jpeg", "image/png", "image/webp")),
    SECOND_HAND_PRODUCT_IMAGE(
            "second-hand/product",
            MediaAssetConstant.VISIBILITY_PUBLIC,
            ImageOutputFormat.JPEG,
            1600,
            1600,
            0.84f,
            1L * 1024 * 1024,
            Set.of("image/jpeg", "image/png", "image/webp")),
    ORDER_IMAGE(
            "order/content",
            MediaAssetConstant.VISIBILITY_PRIVATE,
            ImageOutputFormat.JPEG,
            1920,
            1920,
            0.85f,
            1200L * 1024,
            Set.of("image/jpeg", "image/png", "image/webp")),
    DELIVERY_PROOF(
            "order/delivery-proof",
            MediaAssetConstant.VISIBILITY_PRIVATE,
            ImageOutputFormat.JPEG,
            1920,
            1920,
            0.88f,
            1400L * 1024,
            Set.of("image/jpeg", "image/png", "image/webp")),
    AVATAR(
            "user/avatar",
            MediaAssetConstant.VISIBILITY_PUBLIC,
            ImageOutputFormat.JPEG,
            512,
            512,
            0.85f,
            400L * 1024,
            Set.of("image/jpeg", "image/png", "image/webp")),
    STUDENT_CARD(
            "user/student-card",
            MediaAssetConstant.VISIBILITY_PRIVATE,
            ImageOutputFormat.JPEG,
            2400,
            2400,
            0.90f,
            1800L * 1024,
            Set.of("image/jpeg", "image/png", "image/webp")),
    BANNER(
            "banner",
            MediaAssetConstant.VISIBILITY_PUBLIC,
            ImageOutputFormat.JPEG,
            1404,
            440,
            0.88f,
            800L * 1024,
            Set.of("image/jpeg", "image/png", "image/webp"));

    private static final long MAX_INPUT_BYTES = 10L * 1024 * 1024;
    private static final int MAX_INPUT_DIMENSION = 12_000;
    private static final long MAX_INPUT_PIXELS = 40_000_000L;

    private final String objectPrefix;
    private final String visibility;
    private final ImageOutputFormat outputFormat;
    private final int targetMaxWidth;
    private final int targetMaxHeight;
    private final float outputQuality;
    private final long maxOutputBytes;
    private final Set<String> allowedMimeTypes;

    MediaPurpose(
            String objectPrefix,
            String visibility,
            ImageOutputFormat outputFormat,
            int targetMaxWidth,
            int targetMaxHeight,
            float outputQuality,
            long maxOutputBytes,
            Set<String> allowedMimeTypes) {
        this.objectPrefix = objectPrefix;
        this.visibility = visibility;
        this.outputFormat = outputFormat;
        this.targetMaxWidth = targetMaxWidth;
        this.targetMaxHeight = targetMaxHeight;
        this.outputQuality = outputQuality;
        this.maxOutputBytes = maxOutputBytes;
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

    public long getMaxInputBytes() {
        return MAX_INPUT_BYTES;
    }

    public int getMaxInputDimension() {
        return MAX_INPUT_DIMENSION;
    }

    public long getMaxInputPixels() {
        return MAX_INPUT_PIXELS;
    }

    public ImageOutputFormat getOutputFormat() {
        return outputFormat;
    }

    public int getTargetMaxWidth() {
        return targetMaxWidth;
    }

    public int getTargetMaxHeight() {
        return targetMaxHeight;
    }

    public float getOutputQuality() {
        return outputQuality;
    }

    public long getMaxOutputBytes() {
        return maxOutputBytes;
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

    public enum ImageOutputFormat {
        JPEG("image/jpeg", "jpg", "jpg"),
        PNG("image/png", "png", "png");

        private final String mimeType;
        private final String extension;
        private final String imageIoFormat;

        ImageOutputFormat(String mimeType, String extension, String imageIoFormat) {
            this.mimeType = mimeType;
            this.extension = extension;
            this.imageIoFormat = imageIoFormat;
        }

        public String getMimeType() {
            return mimeType;
        }

        public String getExtension() {
            return extension;
        }

        public String getImageIoFormat() {
            return imageIoFormat;
        }
    }
}
