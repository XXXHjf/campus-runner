package com.mikasa.campusrunner.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * author  Edith
 * created  2024/4/20 13:14
 */
@Configuration
@Slf4j
public class SwaggerConfiguration{
    @Bean
    public OpenAPI springShopOpenAPI() {
        log.info("Starting Swagger configuration...");
        return new OpenAPI()
                .info(new Info().title("Campus Runner API")
                        .description("Campus Runner API Documentation")
                        .version("v1")
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")))
                .externalDocs(new ExternalDocumentation()
                        .description("External Docs")
                        .url("https://springshop.wiki.github.org/docs"));
    }

//    @Override
//    public void addResourceHandlers(ResourceHandlerRegistry registry) {
//        log.info("Setting up static resource mapping...");
//        registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
//        registry.addResourceHandler("/favicon.ico").addResourceLocations("classpath:/static/");
////            WebMvcConfigurer.super.addResourceHandlers(registry);
//
//    }


//    @Override
//    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
//        log.info("Setting up static resource mapping...");
//        registry.addResourceHandler("/doc.html").addResourceLocations("classpath:/META-INF/resources/");
//        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
//    }
}
