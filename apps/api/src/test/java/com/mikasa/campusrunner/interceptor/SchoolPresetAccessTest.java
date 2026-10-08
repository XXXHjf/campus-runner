package com.mikasa.campusrunner.interceptor;

import com.mikasa.campusrunner.controller.user.SchoolController;
import com.mikasa.campusrunner.service.user.SchoolService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.*;

class SchoolPresetAccessTest {
    @Test void userRouteCannotWriteGlobalCampusPresetAddresses() throws Exception {
        SchoolService service = mock(SchoolService.class);
        SchoolController controller = new SchoolController();
        ReflectionTestUtils.setField(controller, "schoolService", service);
        MockMvcBuilders.standaloneSetup(controller).build().perform(post("/api/school")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(service);
    }
}
