package io.github.springboot.httpclient5.core;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "spring.httpclient5.core.enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan("io.github.springboot.httpclient5.core")
public class HttpClient5AutoConfiguration {

}