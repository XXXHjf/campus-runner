package com.mikasa.campusrunner.service.impl.user;

import java.util.List;
import java.util.Map;

final class SecondHandTransferSupport {
    private static final int MAX_REPORT_CONTENT_LENGTH = 32;
    private static final int MAX_OUT_BILL_NO_LENGTH = 32;

    private SecondHandTransferSupport() {
    }

    static List<Map<String, String>> buildSceneReportInfos(String productTitle) {
        return List.of(Map.of(
                "info_type", "回收商品名称",
                "info_content", normalizedProductTitle(productTitle)
        ));
    }

    static String nextOutBillNo(String orderNumber, int attempt) {
        String normalized = orderNumber == null ? "SH" : orderNumber.replaceAll("[^0-9A-Za-z]", "");
        String suffix = "T" + Math.max(1, attempt);
        int baseLength = Math.max(1, MAX_OUT_BILL_NO_LENGTH - suffix.length());
        if (normalized.length() > baseLength) {
            normalized = normalized.substring(0, baseLength);
        }
        return normalized + suffix;
    }

    static boolean isSuccess(String state) {
        return "SUCCESS".equals(state);
    }

    static boolean isFailure(String state) {
        return "FAIL".equals(state) || "CANCELLED".equals(state);
    }

    private static String normalizedProductTitle(String productTitle) {
        String value = productTitle == null ? "二手商品" : productTitle.trim();
        if (value.isEmpty()) {
            value = "二手商品";
        }
        return value.length() <= MAX_REPORT_CONTENT_LENGTH
                ? value
                : value.substring(0, MAX_REPORT_CONTENT_LENGTH);
    }
}
