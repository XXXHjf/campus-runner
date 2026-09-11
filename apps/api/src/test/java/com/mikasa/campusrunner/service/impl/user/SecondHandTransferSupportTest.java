package com.mikasa.campusrunner.service.impl.user;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecondHandTransferSupportTest {

    @Test
    void buildsWechatSecondHandSceneReport() {
        List<Map<String, String>> reportInfos =
                SecondHandTransferSupport.buildSceneReportInfos("九成新计算器");

        assertEquals(1, reportInfos.size());
        assertEquals("回收商品名称", reportInfos.get(0).get("info_type"));
        assertEquals("九成新计算器", reportInfos.get(0).get("info_content"));
    }

    @Test
    void truncatesLongSceneReportContent() {
        String title = "很长的二手商品名称".repeat(8);

        String content = SecondHandTransferSupport.buildSceneReportInfos(title)
                .get(0)
                .get("info_content");

        assertEquals(32, content.length());
    }

    @Test
    void createsWechatCompatibleOutBillNumberPerAttempt() {
        String outBillNo = SecondHandTransferSupport.nextOutBillNo(
                "SH-20260803-123456789012345678901234567890", 2);

        assertTrue(outBillNo.matches("[0-9A-Za-z]+"));
        assertTrue(outBillNo.endsWith("T2"));
        assertTrue(outBillNo.length() <= 32);
    }

    @Test
    void classifiesTerminalStates() {
        assertTrue(SecondHandTransferSupport.isSuccess("SUCCESS"));
        assertTrue(SecondHandTransferSupport.isFailure("FAIL"));
        assertTrue(SecondHandTransferSupport.isFailure("CANCELLED"));
        assertFalse(SecondHandTransferSupport.isFailure("WAIT_USER_CONFIRM"));
    }
}
