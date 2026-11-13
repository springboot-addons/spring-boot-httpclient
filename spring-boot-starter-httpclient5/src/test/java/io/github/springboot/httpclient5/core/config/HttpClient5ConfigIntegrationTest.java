package io.github.springboot.httpclient5.core.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-example.yml")
class HttpClient5ConfigIntegrationTest {

    @Autowired
    private HttpClient5Config httpClient5Config;

    @Test
    void testConfigurationLoaded() {
        assertNotNull(httpClient5Config);
        
        // Test basic properties
        assertEquals("HttpClient5/ExampleApp", httpClient5Config.getUserAgent());
        
        // Test pool configuration
        assertNotNull(httpClient5Config.getPool());
        assertEquals(256, httpClient5Config.getPool().getMaxConnTotal());
        assertEquals(40, httpClient5Config.getPool().getMaxConnPerRoute());
        assertEquals(PoolConcurrencyPolicy.STRICT, httpClient5Config.getPool().getPoolConcurrencyPolicy());
        
        // Test async pool configuration
        assertNotNull(httpClient5Config.getAsyncPool());
        assertEquals(25, httpClient5Config.getAsyncPool().getMaxConnPerRoute());
        assertEquals(PoolConcurrencyPolicy.LAX, httpClient5Config.getAsyncPool().getPoolConcurrencyPolicy());
        
        // Test request configurations
        Map<String, RequestConfigProperties> requestConfig = httpClient5Config.getRequestConfig();
        assertFalse(requestConfig.isEmpty());
        
        // Test that default config is present
        assertTrue(requestConfig.containsKey(HttpClient5Config.DEFAULT_HOST_KEY));
        
        // Test specific host configs are loaded
        assertNotNull(httpClient5Config.getPool().getHostConfig());
        assertFalse(httpClient5Config.getPool().getHostConfig().isEmpty());
        
        // Test async pool host configs
        assertNotNull(httpClient5Config.getAsyncPool().getHostConfig());
        assertFalse(httpClient5Config.getAsyncPool().getHostConfig().isEmpty());
    }
    
    @Test
    void testRequestConfigResolution() {
        // Test default request config resolution
        RequestConfig defaultConfig = httpClient5Config.getDefaultRequestConfig().build();
        assertNotNull(defaultConfig);
        
        // Test specific request config resolution
        RequestConfig specificConfig = httpClient5Config.getRequestConfig("GET", "http://nas.capsi-informatique.fr:9999/test").build();
        assertNotNull(specificConfig);
        
        // Test that the configuration was applied correctly
        assertEquals(30000, specificConfig.getResponseTimeout().toMilliseconds());
    }
}