package io.github.springboot.httpclient5.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import io.github.springboot.httpclient5.AbstractHttpBinTest;
import io.github.springboot.httpclient5.core.config.model.RequestConfigProperties;

@SpringBootTest
@Disabled("Disabled until we can find a way to test this without relying on external services")
@TestPropertySource(locations = "classpath:application-example.yml")
class HttpClient5ConfigIntegrationTest extends AbstractHttpBinTest {

    @Autowired
    private HttpClient5Config httpClient5Config;

    @Test
    void testConfigurationLoaded() {
        assertNotNull(httpClient5Config);
        
        // Test basic properties
        assertEquals("HttpClient5/ExampleApp", httpClient5Config.getUserAgent());
        
        // Test pool configuration
        assertNotNull(httpClient5Config.getPool());
        PoolingHttpClientConnectionManager connectionManager = httpClient5Config.getPool().get().build();
        
		assertEquals(256, connectionManager.getMaxTotal());
        assertEquals(40, connectionManager.getDefaultMaxPerRoute());
        
        // Test async pool configuration

        PoolingAsyncClientConnectionManager asynConnectionManager = httpClient5Config.getAsyncPool().get().build();

        assertNotNull(httpClient5Config.getAsyncPool());
        assertEquals(25, asynConnectionManager.getMaxTotal());
        
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
        RequestConfig specificConfig = httpClient5Config.getRequestConfig("GET", "https://example.com/test") ;
        assertNotNull(specificConfig);
        
        // Test that the configuration was applied correctly
        assertEquals(30000, specificConfig.getResponseTimeout().toMilliseconds());
    }
}