package com.mikasa.campusrunner.interceptor;

import com.mikasa.campusrunner.common.constant.JWTClaimConstant;
import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.properties.JWTProperties;
import com.mikasa.campusrunner.common.utils.EncryptMD5Util;
import com.mikasa.campusrunner.common.utils.EncryptSHA256Util;
import com.mikasa.campusrunner.common.utils.JWTUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * author  Edith
 * created  2024/4/20 20:26
 */
@Component
@Slf4j
public class JwtTokenUserInterceptor implements HandlerInterceptor {
    @Autowired
    private JWTProperties jwtProperties;

    /**
     * 校验jwt
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        BaseContext.removeCurrentId();
        //判断当前拦截到的是Controller的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            //当前拦截到的不是动态方法，直接放行
            return true;
        }

        String token = request.getHeader(jwtProperties.getUserTokenName());

        try {
            log.debug("Validating user JWT");
            Claims claims = JWTUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
            Long id = Long.valueOf(claims.get(JWTClaimConstant.USER_ID).toString());
            log.info("Current user ID: {}", id);
            BaseContext.setCurrentId(id);
            return true;
        } catch (Exception e){
            if (isPublicRead(request)) return true;
            response.setStatus(401);
            return false;
        }
    }

    // Keep the method check: POST/PUT/DELETE on the same paths still require identity.
    static boolean isPublicRead(HttpServletRequest request) {
        if (!"GET".equals(request.getMethod())) return false;
        String path = request.getServletPath();
        return path.equals("/api/second-hand/categories")
                || path.equals("/api/second-hand/products")
                || path.matches("/api/second-hand/products/[0-9]+")
                || path.equals("/api/order/public")
                || path.equals("/api/school")
                || path.matches("/api/school/[0-9]+")
                || path.equals("/api/category")
                || path.matches("/api/category/[0-9]+")
                || path.equals("/api/address/three");
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        BaseContext.removeCurrentId();
    }
}
