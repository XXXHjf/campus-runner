package com.mikasa.campusrunner.service;

import com.mikasa.campusrunner.common.exception.ParamException;
import com.mikasa.campusrunner.common.exception.SubmissionPendingException;
import com.mikasa.campusrunner.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SubmissionResponseStatusTest {
    @RestController static class Failures {
        @GetMapping("/pending") void pending() { throw new SubmissionPendingException(); }
        @GetMapping("/unknown") void unknown() { throw new IllegalStateException("test"); }
        @GetMapping("/rejected") void rejected() { throw new ParamException("请重新填写"); }
    }
    @Test void onlyDefiniteBusinessRejectionUsesHttp200() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new Failures()).setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/pending")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(0));
        mvc.perform(get("/unknown")).andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value(0));
        mvc.perform(get("/rejected")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
    }
}
