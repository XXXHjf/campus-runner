package com.mikasa.campusrunner.interceptor;

import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.filter.RequestGuardFilter;
import com.mikasa.campusrunner.service.RequestGuardService;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class RequestGuardInterceptor implements HandlerInterceptor {
    private final RequestGuardService guard;
    private final com.mikasa.campusrunner.common.properties.JWTProperties jwt;
    @Override public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler) {
        if(!(handler instanceof HandlerMethod) || !guard.getProperties().isEnabled()) return true;
        String path=request.getRequestURI().substring(request.getContextPath().length());
        if(RequestGuardFilter.NOTIFICATIONS.contains(path) || path.equals("/api/media/images") || path.equals("/admin/api/media/images")) return true;
        boolean read="GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod());
        Long id=BaseContext.getCurrentId();
        String namespace=path.startsWith("/admin/")?"ADMIN:":"USER:";
        // The existing config identity interceptor validates JWT but returns before setting BaseContext.
        if(id==null && path.startsWith("/admin/api/config/") && request.getAttribute("systemConfigJudge")!=null) {
            try {
                var claims=com.mikasa.campusrunner.common.utils.JWTUtil.parseJWT(jwt.getUserSecretKey(),request.getHeader(jwt.getUserTokenName()));
                Object actor=claims.get(com.mikasa.campusrunner.common.constant.JWTClaimConstant.ADMIN_USER_ID);
                if(actor==null) { actor=claims.get(com.mikasa.campusrunner.common.constant.JWTClaimConstant.USER_ID);namespace="USER:"; }
                if(actor!=null) id=Long.valueOf(actor.toString());
            } catch(RuntimeException error) {return true;}
        }
        if(id==null || id<=0) return true;
        guard.check(namespace+id+(read?":READ":":WRITE"),
                60,read?guard.getProperties().getReadPerMinute():guard.getProperties().getWritePerMinute());
        return true;
    }
}
