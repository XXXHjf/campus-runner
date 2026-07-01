package com.mikasa.campusrunner.interceptor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mikasa.campusrunner.common.constant.JWTClaimConstant;
import com.mikasa.campusrunner.common.properties.JWTProperties;
import com.mikasa.campusrunner.common.result.Result;
import com.mikasa.campusrunner.common.utils.JWTUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * author  Edith
 * created  2025/12/20 21:42
 * 请求接口upload的专门拦截器
 * 用以拦截针对此请求的请求，检验是否存在合法的token
 */
@Component
@Slf4j
public class UploadFileInterceptor implements HandlerInterceptor {

    @Autowired
    private JWTProperties jwtProperties;

    /**
     * 请求接口upload的专门拦截器
     * 用以拦截针对此请求的请求，检验是否存在合法的token
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断当前拦截到的是Controller的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            //当前拦截到的不是动态方法，直接放行
            return true;
        }

        log.info("Upload interceptor starting, checking token validity...");
        String token = request.getHeader(jwtProperties.getUserTokenName());

        if (token == null) {
            log.info("Request missing token...");

            //返回结果
            Result<String> result = Result.error("NO_TOKEN: Missing token in request");
            //构造json
            String json = JSONObject.toJSONString(result);

            //返回json数据
//            response.setStatus(401);
            returnJsonData(response, json, 401);

            return false;
        }


        Claims claims = JWTUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
        //检验是否是用户的token
        Object userId = claims.get(JWTClaimConstant.USER_ID);
        //检验是否是管理员用户id
        Object adminId = claims.get(JWTClaimConstant.ADMIN_USER_ID);
        if (userId == null && adminId == null) {
            //说明当前token不合法
            //返回结果
            log.info("Illegal token, request denied!");
            Result<String> result = Result.error("ILLEGAL_TOKEN: Token is invalid, access denied");
            String json = JSONObject.toJSONString(result);
            //返回json数据
//            response.setStatus(401);
            returnJsonData(response, json, 401);

            return false;
        }else {
            return true;
        }

    }



    private void returnJsonData(HttpServletResponse response, String json, Integer status) {
        //设置参数
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(status);

        // 获取PrintWriter对象
        PrintWriter out = null;
        try {
            out = response.getWriter();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        out.print(json);
        // 释放PrintWriter对象
        out.flush();
        out.close();

    }

}
