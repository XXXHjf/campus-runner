package com.mikasa.campusrunner.migration.media;

import com.mikasa.campusrunner.common.constant.MediaAssetConstant;
import com.mikasa.campusrunner.common.constant.MediaPurpose;

public enum LegacyMediaSource {
    ORDER_CATEGORY(
            "tb_category",
            "image",
            MediaPurpose.ORDER_CATEGORY_ICON,
            MediaAssetConstant.BOUND_ORDER_CATEGORY,
            MediaAssetConstant.OWNER_ADMIN,
            1),
    SECOND_HAND_CATEGORY(
            "tb_second_hand_category",
            "image",
            MediaPurpose.SECOND_HAND_CATEGORY_ICON,
            MediaAssetConstant.BOUND_SECOND_HAND_CATEGORY,
            MediaAssetConstant.OWNER_ADMIN,
            1),
    SECOND_HAND_CATEGORY_ASSET(
            "tb_second_hand_category",
            "image_asset_id",
            MediaPurpose.SECOND_HAND_CATEGORY_ICON,
            MediaAssetConstant.BOUND_SECOND_HAND_CATEGORY,
            MediaAssetConstant.OWNER_ADMIN,
            1),
    SECOND_HAND_PRODUCT(
            "tb_second_hand_product",
            "images",
            MediaPurpose.SECOND_HAND_PRODUCT_IMAGE,
            MediaAssetConstant.BOUND_SECOND_HAND_PRODUCT,
            MediaAssetConstant.OWNER_USER,
            6),
    ORDER(
            "tb_orders",
            "image",
            MediaPurpose.ORDER_IMAGE,
            MediaAssetConstant.BOUND_ORDER,
            MediaAssetConstant.OWNER_USER,
            1),
    TAKE_ORDER(
            "tb_take_orders",
            "image",
            MediaPurpose.DELIVERY_PROOF,
            MediaAssetConstant.BOUND_TAKE_ORDER,
            MediaAssetConstant.OWNER_USER,
            1),
    USER_AVATAR(
            "tb_user",
            "head_img",
            MediaPurpose.AVATAR,
            MediaAssetConstant.BOUND_USER_AVATAR,
            MediaAssetConstant.OWNER_USER,
            1),
    USER_STUDENT_CARD(
            "tb_user",
            "student_id_card",
            MediaPurpose.STUDENT_CARD,
            MediaAssetConstant.BOUND_USER_STUDENT_CARD,
            MediaAssetConstant.OWNER_USER,
            1),
    USER_ALIPAY_PAYMENT(
            "tb_user",
            "alipay_payment_code",
            MediaPurpose.PAYMENT_QR,
            MediaAssetConstant.BOUND_USER_ALIPAY_PAYMENT,
            MediaAssetConstant.OWNER_USER,
            1),
    USER_WECHAT_PAYMENT(
            "tb_user",
            "wechat_payment_code",
            MediaPurpose.PAYMENT_QR,
            MediaAssetConstant.BOUND_USER_WECHAT_PAYMENT,
            MediaAssetConstant.OWNER_USER,
            1),
    BANNER(
            "tb_banner",
            "img_url",
            MediaPurpose.BANNER,
            MediaAssetConstant.BOUND_BANNER,
            MediaAssetConstant.OWNER_ADMIN,
            1);

    private final String sourceTable;
    private final String sourceColumn;
    private final MediaPurpose purpose;
    private final String boundType;
    private final String ownerType;
    private final int maxCount;

    LegacyMediaSource(
            String sourceTable,
            String sourceColumn,
            MediaPurpose purpose,
            String boundType,
            String ownerType,
            int maxCount) {
        this.sourceTable = sourceTable;
        this.sourceColumn = sourceColumn;
        this.purpose = purpose;
        this.boundType = boundType;
        this.ownerType = ownerType;
        this.maxCount = maxCount;
    }

    public String getSourceTable() {
        return sourceTable;
    }

    public String getSourceColumn() {
        return sourceColumn;
    }

    public MediaPurpose getPurpose() {
        return purpose;
    }

    public String getBoundType() {
        return boundType;
    }

    public String getOwnerType() {
        return ownerType;
    }

    public int getMaxCount() {
        return maxCount;
    }
}
