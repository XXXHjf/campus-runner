package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.controller.user.WeChatTransferController;
import com.mikasa.campusrunner.service.user.WeChatTransferService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransferTestRoutesTest {
    @Test void formerQueryAndCancellationRoutesCannotReachFundsService() throws Exception {
        var controller = new WeChatTransferController();
        var service = mock(WeChatTransferService.class);
        ReflectionTestUtils.setField(controller, "weChatTransferService", service);
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(get("/api/wx-transfer/O1")).andExpect(status().isNotFound());
        mvc.perform(post("/api/wx-transfer/close/O1")).andExpect(status().isNotFound());
        verifyNoInteractions(service);
    }
}
