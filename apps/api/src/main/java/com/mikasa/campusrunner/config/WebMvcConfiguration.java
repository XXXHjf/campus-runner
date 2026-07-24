package com.mikasa.campusrunner.config;

import com.mikasa.campusrunner.common.json.JacksonObjectMapper;
import com.mikasa.campusrunner.filter.SystemConfigFilter;
import com.mikasa.campusrunner.interceptor.JwtTokenAdminUserInterceptor;
import com.mikasa.campusrunner.interceptor.JwtTokenUserInterceptor;
import com.mikasa.campusrunner.interceptor.UploadFileInterceptor;
import jakarta.servlet.FilterRegistration;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Select;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * author  Edith
 * created  2024/4/20 20:35
 */
@Configuration
@Slf4j
//public class WebMvcConfiguration extends WebMvcConfigurationSupport {
//
//    @Autowired
//    private JwtTokenUserInterceptor jwtTokenUserInterceptor;
//
//    /**
//     * 自定义注册拦截器
//     * @param registry
//     */
//    @Override
//    protected void addInterceptors(InterceptorRegistry registry) {
//        log.info("Registering interceptors...");
//        registry.addInterceptor(jwtTokenUserInterceptor)
//                .addPathPatterns("/api/**")
//                .excludePathPatterns("/api/user/login");
//
//    }
//
//    @Override
//    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
//        log.info("Enabling static resource mapping...");
////        registry.addResourceHandler("/doc.html").addResourceLocations("classpath:/META-INF/resources/");
////        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
//        registry.addResourceHandler("/**").addResourceLocations("classpath:/META-INF/resources/", "classpath:/static/");
////        registry.addResourceHandler("/swagger-ui/**").addResourceLocations("classpath:/META-INF/resources/");
////        registry.addResourceHandler("/favicon.ico").addResourceLocations("classpath:/static/");
////        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
//    }
//
//
//    /**
//     * 设置消息转换器 使时间能正常显示
//     * @param converters
//     */
////    @Override
//    protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
//        log.info("Setting up message converters for proper time formatting...");
//        //创建一个消息转换器
//        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
//
//        //设置对象转换器
//        converter.setObjectMapper(new JacksonObjectMapper());
//
//        //将创建的消息转换器加入到MVC中 设置优先级
//        converters.add(1,converter);
//
//    }
//}
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Autowired
    private JwtTokenUserInterceptor jwtTokenUserInterceptor;
    @Autowired
    private JwtTokenAdminUserInterceptor jwtTokenAdminUserInterceptor;

    @Autowired
    private UploadFileInterceptor uploadFileInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("Registering interceptors...");
        //注册用户端拦截器
        registry.addInterceptor(jwtTokenUserInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/user/login",
                        "/api/wx-pay/jsapi/notify",
                        "/api/wx-pay/refunds/notify",
                        "/api/second-hand/pay/notify",
                        "/api/second-hand/refunds/notify",
                        "/api/second-hand/transfer/notify",
                        "/api/wx-transfer/notify",
                        "/api/upload");

        //注册管理员端拦截器
        registry.addInterceptor(jwtTokenAdminUserInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns(
                        "/admin/api/login",
                        "/admin/api/register",
                        "/admin/api/banner/getList/{schoolId}");

        //注册拦截upload请求的拦截器
        registry.addInterceptor(uploadFileInterceptor)
                .addPathPatterns("/api/upload");

    }

    @Bean
    public FilterRegistrationBean registration() {
        log.info("Setting up filter for system config requests...");
        FilterRegistrationBean registrationBean = new FilterRegistrationBean();
        registrationBean.setFilter(new SystemConfigFilter());
        Set<String> urlPatters = new TreeSet<>();
        urlPatters.add("/admin/api/config/*");
        registrationBean.setUrlPatterns(urlPatters);
        registrationBean.setBeanName("systemConfigFilter");
        return registrationBean;
    }


    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.info("Setting up message converters for proper time formatting...");
        //创建一个消息转换器
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();

        //设置对象转换器
        converter.setObjectMapper(new JacksonObjectMapper());

        //将创建的消息转换器加入到MVC中 设置优先级
        converters.add(1,converter);
    }
}
