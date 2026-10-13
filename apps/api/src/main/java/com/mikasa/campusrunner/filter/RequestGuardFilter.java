package com.mikasa.campusrunner.filter;

import com.mikasa.campusrunner.common.constant.JWTClaimConstant;
import com.mikasa.campusrunner.common.exception.RequestLimitException;
import com.mikasa.campusrunner.common.properties.JWTProperties;
import com.mikasa.campusrunner.common.utils.JWTUtil;
import com.mikasa.campusrunner.service.RequestGuardService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE+20)
@RequiredArgsConstructor
@Slf4j
public class RequestGuardFilter extends OncePerRequestFilter {
    public static final Set<String> NOTIFICATIONS=Set.of("/api/wx-pay/jsapi/notify","/api/wx-pay/refunds/notify",
            "/api/wx-transfer/notify","/api/second-hand/pay/notify","/api/second-hand/refunds/notify","/api/second-hand/transfer/notify");
    private final RequestGuardService guard;
    private final JWTProperties jwt;
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path=request.getRequestURI().substring(request.getContextPath().length());
        return !guard.getProperties().isEnabled() || "OPTIONS".equals(request.getMethod()) || NOTIFICATIONS.contains(path)
                || !(path.startsWith("/api/") || path.startsWith("/admin/api/"));
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        String lease=null;
        try {
            String path=request.getRequestURI().substring(request.getContextPath().length());
            // Reject alternate encodings/matrix paths before MVC normalizes them to an upload route.
            if(path.contains("%") || path.contains(";") || path.contains("//") || path.contains("\\") || path.contains("/../") || path.contains("/./")) {
                reject(response,400,"请求有误，请重新尝试",1);return;
            }
            String ip=hash(clientAddress(request));
            guard.checkIp("IP:"+ip);
            if ("POST".equals(request.getMethod()) && Set.of("/api/media/images","/admin/api/media/images").contains(path)) {
                String actor=uploadActor(request,path);
                if(actor==null) { reject(response,401,"登录已失效，请重新登录",1);return; }
                var purpose=uploadPurpose(request);
                if(purpose==null || !purpose.allowsOwnerType(actor.startsWith("ADMIN:")?"ADMIN":"USER")) {
                    reject(response,400,"请更新后重新上传图片",1);return;
                }
                String group=actor.startsWith("ADMIN:")?"ADMIN":(purpose==com.mikasa.campusrunner.common.constant.MediaPurpose.DELIVERY_PROOF
                        || purpose==com.mikasa.campusrunner.common.constant.MediaPurpose.PURCHASE_PROOF)?"EVIDENCE":"CONTENT";
                guard.check("IP_UPLOAD:"+ip,60,guard.getProperties().getIpUploadPerMinute());
                guard.check(actor+":UPLOAD:"+group,60,guard.getProperties().getUploadPerMinute());
                lease=guard.acquireUpload(actor);
                guard.reserveInput(actor,purpose,request.getContentLengthLong());
            }
        } catch(RequestLimitException e) { release(lease);reject(response,429,e.getMessage(),e.getRetryAfter());return; }
        catch(com.mikasa.campusrunner.common.exception.RequestGuardUnavailableException e) { release(lease);reject(response,503,e.getMessage(),5);return; }
        catch(org.springframework.dao.DataAccessException e) { release(lease);reject(response,503,"服务繁忙，请稍后重试",5);return; }
        catch(org.springframework.transaction.TransactionException e) { release(lease);reject(response,503,"服务繁忙，请稍后重试",5);return; }
        try { chain.doFilter(request,response); }
        finally {
            release(lease);
        }
    }
    private void release(String lease) {
        guard.clearInputReservation();
        if(lease!=null) try {guard.releaseUpload(lease);} catch(RuntimeException e) {log.warn("Upload lease release deferred, failureType={}",e.getClass().getSimpleName());}
    }
    private com.mikasa.campusrunner.common.constant.MediaPurpose uploadPurpose(HttpServletRequest request) {
        // Never call getParameter(): it can trigger multipart parsing before admission.
        String raw=request.getQueryString(), value=null;
        if(raw==null || raw.length()>1024) return null;
        try {
            for(String pair:raw.split("&")) {
                String[] part=pair.split("=",2);
                if("purpose".equals(java.net.URLDecoder.decode(part[0],StandardCharsets.UTF_8))) {
                    if(value!=null || part.length!=2) return null;
                    value=java.net.URLDecoder.decode(part[1],StandardCharsets.UTF_8);
                }
            }
            return com.mikasa.campusrunner.common.constant.MediaPurpose.from(value);
        } catch(RuntimeException error) {return null;}
    }
    private String uploadActor(HttpServletRequest request,String path) {
        try {
            var claims=JWTUtil.parseJWT(jwt.getUserSecretKey(),request.getHeader(jwt.getUserTokenName()));
            boolean admin=path.startsWith("/admin/");
            Long id=Long.valueOf(claims.get(admin?JWTClaimConstant.ADMIN_USER_ID:JWTClaimConstant.USER_ID).toString());
            return id>0 ? (admin?"ADMIN:":"USER:")+id : null;
        } catch(RuntimeException e) {return null;}
    }
    public String clientAddress(HttpServletRequest request) {
        String remote=numericAddress(request.getRemoteAddr());
        boolean trusted=!remote.equals("invalid") && guard.getProperties().getTrustedProxies().stream().map(RequestGuardFilter::numericAddress).anyMatch(remote::equals);
        // Nginx overwrites X-Real-IP with its connection source. Never parse arbitrary X-Forwarded-For.
        String forwarded=request.getHeader("X-Real-IP");
        if(trusted && forwarded!=null) {
            String parsed=numericAddress(forwarded);
            if(!parsed.equals("invalid")) return parsed;
        }
        return remote;
    }
    private static String numericAddress(String value) {
        if(value==null || !value.matches("[0-9a-fA-F:.]+") || value.length()>45
                || (!value.contains(":") && !value.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}"))) return "invalid";
        try {return InetAddress.getByName(value).getHostAddress();} catch(Exception e) {return "invalid";}
    }
    private static String hash(String address) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(address.getBytes(StandardCharsets.UTF_8)));}
        catch(Exception e) {throw new IllegalStateException(e);}
    }
    private static void reject(HttpServletResponse response,int status,String message,int wait) throws IOException {
        response.setStatus(status);response.setHeader("Retry-After",String.valueOf(wait));
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":0,\"msg\":\""+message+"\"}");
    }
}
