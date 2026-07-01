package com.mikasa.campusrunner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.filter.CharacterEncodingFilter;

@SpringBootApplication
@EnableTransactionManagement //开启事务注解处理
//@EnableAspectJAutoProxy
@EnableScheduling//启动定时任务
public class CampusRunnerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusRunnerApplication.class, args);
    }
//    CharacterEncodingFilter
}
