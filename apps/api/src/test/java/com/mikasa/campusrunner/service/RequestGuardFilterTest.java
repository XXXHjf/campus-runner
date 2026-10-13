package com.mikasa.campusrunner.service;

import com.mikasa.campusrunner.common.exception.RequestLimitException;
import com.mikasa.campusrunner.common.properties.*;
import com.mikasa.campusrunner.common.utils.JWTUtil;
import com.mikasa.campusrunner.common.constant.JWTClaimConstant;
import com.mikasa.campusrunner.filter.RequestGuardFilter;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RequestGuardFilterTest {
    RequestGuardService guard; RequestGuardProperties p; JWTProperties jwt; RequestGuardFilter filter;
    @BeforeEach void setup() {
        guard=mock(RequestGuardService.class);p=new RequestGuardProperties();p.setEnabled(true);when(guard.getProperties()).thenReturn(p);
        jwt=new JWTProperties();jwt.setUserTokenName("token");jwt.setUserSecretKey("test-secret-only-012345678901234567890123456789");
        filter=new RequestGuardFilter(guard,jwt);
    }
    @AfterEach void clearIdentity() { com.mikasa.campusrunner.common.context.BaseContext.removeCurrentId(); }
    public void endpoint() {}
    @Test void onlyLoopbackProxyCanSupplyAValidRealIpAndXffNeverOverrides() {
        var req=new MockHttpServletRequest();req.setRemoteAddr("192.0.2.5");req.addHeader("X-Real-IP","198.51.100.7");
        assertEquals("192.0.2.5",filter.clientAddress(req));req.setRemoteAddr("127.0.0.1");
        assertEquals("198.51.100.7",filter.clientAddress(req));req.removeHeader("X-Real-IP");req.addHeader("X-Forwarded-For","198.51.100.7");
        assertEquals("127.0.0.1",filter.clientAddress(req));req.addHeader("X-Real-IP","1");assertEquals("127.0.0.1",filter.clientAddress(req));
    }
    @Test void notificationExemptionIsExactAndAllSixCallbacksReachTheirVerifier() throws Exception {
        for(String path:RequestGuardFilter.NOTIFICATIONS){var req=new MockHttpServletRequest("POST",path);var chain=new MockFilterChain();filter.doFilter(req,new MockHttpServletResponse(),chain);assertNotNull(chain.getRequest());}
        verify(guard,never()).checkIp(anyString());
        filter.doFilter(new MockHttpServletRequest("POST","/api/second-hand/pay/notify/evil"),new MockHttpServletResponse(),new MockFilterChain());
        verify(guard).checkIp(anyString());
    }
    @Test void rejectedUploadNeverReachesMultipartParsingAndSetsRetryAfter() throws Exception {
        var req=new MockHttpServletRequest("POST","/api/media/images");
        req.setQueryString("purpose=ORDER_IMAGE");
        req.addHeader("token",JWTUtil.createJWT(jwt.getUserSecretKey(),60000,Map.of(JWTClaimConstant.USER_ID,8L)));
        doThrow(new RequestLimitException(15)).when(guard).check(eq("USER:8:UPLOAD:CONTENT"),eq(60),anyInt());
        var res=new MockHttpServletResponse();var chain=new MockFilterChain();filter.doFilter(req,res,chain);
        assertEquals(429,res.getStatus());assertEquals("15",res.getHeader("Retry-After"));assertNull(chain.getRequest());verify(guard,never()).acquireUpload(anyString());
    }
    @Test void userJwtCannotConsumeAdminUploadBudgetAndValidAdminLeaseReleasesOnFailure() throws Exception {
        var req=new MockHttpServletRequest("POST","/admin/api/media/images");
        req.addHeader("token",JWTUtil.createJWT(jwt.getUserSecretKey(),60000,Map.of(JWTClaimConstant.USER_ID,8L)));
        var res=new MockHttpServletResponse();filter.doFilter(req,res,new MockFilterChain());assertEquals(401,res.getStatus());
        req.removeHeader("token");req.addHeader("token",JWTUtil.createJWT(jwt.getUserSecretKey(),60000,Map.of(JWTClaimConstant.ADMIN_USER_ID,8L)));
        when(guard.acquireUpload("ADMIN:8")).thenReturn("lease");
        req.setQueryString("purpose=BANNER");
        assertThrows(jakarta.servlet.ServletException.class,()->filter.doFilter(req,new MockHttpServletResponse(),(a,b)->{throw new jakarta.servlet.ServletException("test");}));
        verify(guard).releaseUpload("lease");
    }
    @Test void alternateUploadPathsAreRejectedBeforeMvcNormalization() throws Exception {
        for(String path:java.util.List.of("/api/media/images;x=1", "/api/media/%69mages", "/api//media/images")) {
            var response=new MockHttpServletResponse();var chain=new MockFilterChain();
            filter.doFilter(new MockHttpServletRequest("POST",path),response,chain);
            assertEquals(400,response.getStatus());assertNull(chain.getRequest());
        }
        verify(guard,never()).acquireUpload(anyString());
    }
    @Test void dailyAdmissionAndPurposeValidationRunBeforeMultipartParsing() throws Exception {
        var req=new MockHttpServletRequest("POST","/api/media/images");req.setQueryString("purpose=DELIVERY_PROOF");
        req.addHeader("token",JWTUtil.createJWT(jwt.getUserSecretKey(),60000,Map.of(JWTClaimConstant.USER_ID,8L)));
        when(guard.acquireUpload("USER:8")).thenReturn("lease");
        doThrow(new RequestLimitException(100)).when(guard).reserveInput(eq("USER:8"),eq(com.mikasa.campusrunner.common.constant.MediaPurpose.DELIVERY_PROOF),anyLong());
        var res=new MockHttpServletResponse();var chain=new MockFilterChain();filter.doFilter(req,res,chain);
        assertEquals(429,res.getStatus());assertNull(chain.getRequest());verify(guard).releaseUpload("lease");
    }
    @Test void accountInterceptorSeparatesVerifiedReadWriteAndAdminBudgets() throws Exception {
        var interceptor=new com.mikasa.campusrunner.interceptor.RequestGuardInterceptor(guard,jwt);
        var handler=new org.springframework.web.method.HandlerMethod(this,getClass().getMethod("endpoint"));
        com.mikasa.campusrunner.common.context.BaseContext.setCurrentId(7L);
        interceptor.preHandle(new MockHttpServletRequest("GET","/api/address/show"),new MockHttpServletResponse(),handler);
        interceptor.preHandle(new MockHttpServletRequest("POST","/admin/api/address"),new MockHttpServletResponse(),handler);
        verify(guard).check("USER:7:READ",60,120);verify(guard).check("ADMIN:7:WRITE",60,30);
    }
    @Test void alreadyAuthorizedConfigReadsWithNoThreadIdentityStillUseVerifiedClaimId() throws Exception {
        var interceptor=new com.mikasa.campusrunner.interceptor.RequestGuardInterceptor(guard,jwt);
        var handler=new org.springframework.web.method.HandlerMethod(this,getClass().getMethod("endpoint"));
        var request=new MockHttpServletRequest("GET","/admin/api/config/service_fee_rate");request.setAttribute("systemConfigJudge","validated-by-existing-interceptor");
        request.addHeader("token",JWTUtil.createJWT(jwt.getUserSecretKey(),60000,Map.of(JWTClaimConstant.ADMIN_USER_ID,8L)));
        interceptor.preHandle(request,new MockHttpServletResponse(),handler);verify(guard).check("ADMIN:8:READ",60,120);
        assertNull(com.mikasa.campusrunner.common.context.BaseContext.getCurrentId());
    }
}
