package com.mikasa.campusrunner.config;

import org.apache.ibatis.logging.nologging.NoLoggingImpl;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** SQL parameters include phone numbers, private addresses and payment payloads. */
@Configuration
public class DiagnosticLoggingConfiguration {
    @Bean
    public ConfigurationCustomizer disableSqlParameterLogging() {
        return configuration -> configuration.setLogImpl(NoLoggingImpl.class);
    }
}
