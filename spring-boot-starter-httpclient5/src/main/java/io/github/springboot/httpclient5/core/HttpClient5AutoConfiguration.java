package io.github.springboot.httpclient5.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import io.github.springboot.httpclient5.core.utils.ThreadFactoryUtils;

@Configuration
@ConditionalOnProperty(name = "spring.httpclient5.core.enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan("io.github.springboot.httpclient5.core")
public class HttpClient5AutoConfiguration {
	
    @Bean
    public static ThreadFactoryUtils threadFactoryUtils(@Value("${spring.threads.virtual.enabled:false}") boolean enabled) {
    	return new ThreadFactoryUtils(enabled) ;
    }
    
}