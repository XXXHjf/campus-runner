package com.mikasa.campusrunner.interceptor;

import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.properties.JWTProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.method.HandlerMethod;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminContextIsolationTest {
    @AfterEach void clearIdentity() { BaseContext.removeCurrentId(); }
    @Test void failedAuthenticationCannotRetainPreviousAdminIdentity() throws Exception {
        JwtTokenAdminUserInterceptor interceptor = new JwtTokenAdminUserInterceptor();
        JWTProperties properties = mock(JWTProperties.class);
        when(properties.getUserTokenName()).thenReturn("token");
        ReflectionTestUtils.setField(interceptor, "jwtProperties", properties);
        BaseContext.setCurrentId(42L);
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(new MockHttpServletRequest(), response, mock(HandlerMethod.class)));
        assertEquals(401, response.getStatus());
        assertNull(BaseContext.getCurrentId());
    }
    @Test void completedAdminRequestReleasesThreadIdentity() {
        BaseContext.setCurrentId(42L);
        new JwtTokenAdminUserInterceptor().afterCompletion(null, null, null, null);
        assertNull(BaseContext.getCurrentId());
    }
}
