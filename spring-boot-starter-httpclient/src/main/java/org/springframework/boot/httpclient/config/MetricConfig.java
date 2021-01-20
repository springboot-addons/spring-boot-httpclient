package org.springframework.boot.httpclient.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.codahale.metrics.MetricRegistry;

@Configuration
public class MetricConfig {

    @Bean("legacyMetricRegistry")
    public MetricRegistry getMetricsRegistry(){
        return new MetricRegistry();
    }

}
