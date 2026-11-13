package io.github.springboot.httpclient5.core.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.apache.hc.client5.http.config.RequestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-example.yml")
class HttpClient5ConfigTest {

    @Autowired
    private HttpClient5Config httpClient5Config;

    @Test
    void testConfigurationProperties() {
        assertNotNull(httpClient5Config);
        
        // Test basic properties
        assertEquals("HttpClient5/ExampleApp", httpClient5Config.getUserAgent());
        
        // Test pool configuration
        assertNotNull(httpClient5Config.getPool());
        assertEquals(256, httpClient5Config.getPool().getMaxConnTotal());
        assertEquals(40, httpClient5Config.getPool().getMaxConnPerRoute());
        
        // Test async pool configuration
        assertNotNull(httpClient5Config.getAsyncPool());
        assertEquals(25, httpClient5Config.getAsyncPool().getMaxConnPerRoute());
        
        // Test request configurations
        Map<String, RequestConfigProperties> requestConfig = httpClient5Config.getRequestConfig();
        assertFalse(requestConfig.isEmpty());
        
        // Test default request config
        RequestConfig defaultRequestConfig = httpClient5Config.getDefaultRequestConfig().build();
        assertNotNull(defaultRequestConfig);
        
        // Test specific request configs
        RequestConfig testGetConfig = httpClient5Config.getRequestConfig("GET", "http://nas.capsi-informatique.fr:9999/test").build();
        assertNotNull(testGetConfig);
    }
}