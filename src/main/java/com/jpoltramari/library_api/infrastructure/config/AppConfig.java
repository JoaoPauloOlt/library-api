package com.jpoltramari.library_api.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ErrorProperties.class)
public class AppConfig {
}
