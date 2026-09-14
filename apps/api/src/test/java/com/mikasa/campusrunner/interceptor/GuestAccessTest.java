package com.mikasa.campusrunner.interceptor;

import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.constant.JWTClaimConstant;
import com.mikasa.campusrunner.common.properties.JWTProperties;
import com.mikasa.campusrunner.common.utils.JWTUtil;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.method.HandlerMethod;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class GuestAccessTest {
    public void endpoint() {}

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return request;
    }

    @Test
    void onlyExplicitPublicReadsAreAnonymous() {
        for (String path : new String[]{"/api/second-hand/categories", "/api/second-hand/products",
                "/api/second-hand/products/1", "/api/order/public", "/api/address/three"}) {
            assertTrue(JwtTokenUserInterceptor.isPublicRead(request("GET", path)));
            for (String method : new String[]{"POST", "PUT", "DELETE"}) {
                assertFalse(JwtTokenUserInterceptor.isPublicRead(request(method, path)));
            }
        }
        for (String path : new String[]{"/api/user", "/api/second-hand/products/my",
                "/api/second-hand/products/1/messages", "/api/second-hand/orders/1",
                "/api/order/detail/1", "/api/address/show"}) {
            assertFalse(JwtTokenUserInterceptor.isPublicRead(request("GET", path)));
        }
    }

    @Test
    void optionalIdentityNeverLeaksAcrossRequests() throws Exception {
        String key = "guest-access-test-key-not-for-production-123456";
        JWTProperties props = new JWTProperties();
        props.setUserTokenName("token");
        props.setUserSecretKey(key);
        JwtTokenUserInterceptor interceptor = new JwtTokenUserInterceptor();
        ReflectionTestUtils.setField(interceptor, "jwtProperties", props);
        HandlerMethod handler = new HandlerMethod(this, getClass().getMethod("endpoint"));
        MockHttpServletRequest loggedIn = request("GET", "/api/second-hand/products");
        loggedIn.addHeader("token", JWTUtil.createJWT(key, 60000, Map.of(JWTClaimConstant.USER_ID, 7L)));
        assertTrue(interceptor.preHandle(loggedIn, new MockHttpServletResponse(), handler));
        assertEquals(7L, BaseContext.getCurrentId());
        interceptor.afterCompletion(loggedIn, new MockHttpServletResponse(), handler, null);
        assertNull(BaseContext.getCurrentId());
        BaseContext.setCurrentId(99L);
        MockHttpServletRequest guest = request("GET", "/api/second-hand/products/1");
        guest.addHeader("token", "expired");
        assertTrue(interceptor.preHandle(guest, new MockHttpServletResponse(), handler));
        assertNull(BaseContext.getCurrentId());
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(request("POST", "/api/second-hand/products"), response, handler));
        assertEquals(401, response.getStatus());
    }
}
