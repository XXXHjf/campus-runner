package com.mikasa.campusrunner.filter;

import com.mikasa.campusrunner.common.properties.JWTProperties;
import com.mikasa.campusrunner.common.utils.EncryptMD5Util;
import com.mikasa.campusrunner.common.utils.EncryptSHA256Util;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * author  Edith
 * created  2026/3/9 07:48
 */
@Slf4j
public class SystemConfigFilter implements Filter {


    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        log.info("Executing system config filter...");
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String token = httpRequest.getHeader("token");
        if (token == null) token = "";

        //SHA-256加密
        String sha256Str = EncryptSHA256Util.encrypt(EncryptMD5Util.encrypt(token));
        httpRequest.setAttribute("systemConfigJudge", sha256Str);
        chain.doFilter(request, response);
    }



}
