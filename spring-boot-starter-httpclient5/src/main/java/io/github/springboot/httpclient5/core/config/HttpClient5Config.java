package io.github.springboot.httpclient5.core.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.hc.client5.http.config.RequestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ConcurrentLruCache;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.springboot.httpclient5.core.config.model.CharCodingConfigProperties;
import io.github.springboot.httpclient5.core.config.model.ConnectionManagerConfigProperties;
import io.github.springboot.httpclient5.core.config.model.Http1ConfigProperties;
import io.github.springboot.httpclient5.core.config.model.RequestConfigProperties;
import io.github.springboot.httpclient5.core.utils.PatternUtils;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@Configuration
@ConfigurationProperties(prefix = "spring.httpclient5")
@Slf4j
public class HttpClient5Config {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	public static final String DEFAULT_HOST_KEY = "default";
	
	@NestedConfigurationProperty
	private ConnectionManagerConfigProperties pool = new ConnectionManagerConfigProperties() ;
	@NestedConfigurationProperty
	private Http1ConfigProperties http1 = new Http1ConfigProperties();
	@NestedConfigurationProperty
	private CharCodingConfigProperties charCoding = new CharCodingConfigProperties();
	
	@NestedConfigurationProperty
	private Map<String, RequestConfigProperties> requestConfig = new HashMap<String, RequestConfigProperties>();

	@NestedConfigurationProperty
	private JmxConfig jmx = new JmxConfig() ;
	
	@Getter(value = AccessLevel.NONE)
	@Setter(value = AccessLevel.NONE)
	private ConcurrentLruCache<Pair<String, String>, RequestConfigProperties> requestConfigPropertiesCache = new ConcurrentLruCache<>(10240, this::getRequestConfigProperties) ; 
	
	private String userAgent ;
	
	@Autowired
	ConfigurableEnvironment env;
	
	@Override
	@SneakyThrows
	public String toString() {
		return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this.requestConfig);
	}
	
	@PostConstruct
	public void init() {
		// Expansion des clés dans les map requestConfig et hostConfig du pool
		Map<String, RequestConfigProperties> erc = new HashMap<String, RequestConfigProperties>();
		requestConfig.forEach((k, v) -> erc.put(env.resolvePlaceholders(k), v));
		requestConfig = erc ;
		
		Map<String, Integer> ehc = new HashMap<String, Integer>();
		pool.getHostConfig().forEach((k, v) -> ehc.put(env.resolvePlaceholders(k), v));
		pool.setHostConfig(ehc) ;
	}
	
	@SneakyThrows
	public RequestConfig getRequestConfig(String method, String uri) {
		return getRequestConfigProperties(method, uri).build() ;
	}
	
	@SneakyThrows
	public RequestConfigProperties getRequestConfigProperties(String method, String uri) {
		return requestConfigPropertiesCache.get(Pair.of(method, uri)) ;
	}
	
	private RequestConfigProperties getRequestConfigProperties(Pair<String, String> key) {
		String methodAndUrl = key.getLeft() + " " + key.getRight();
		RequestConfigProperties initial = getDefaultRequestConfig() ;
		RequestConfigProperties res = getRequestConfig().entrySet().stream()
			.filter(e -> {
				return PatternUtils.matches(methodAndUrl, e.getKey()) ;
			})
			.sorted(this::byMatcherPrecision)
			.map(hc -> hc.getValue())
			.reduce(initial, (current, toApply) -> { 
				log.trace("Applying request config for {}", key);
				return current.apply(toApply);	
			}) ;
			 
		return res ;
	}

	public RequestConfigProperties getDefaultRequestConfig() {
		return new RequestConfigProperties(requestConfig.get(HttpClient5Config.DEFAULT_HOST_KEY));
	}
	
	private int byMatcherPrecision(Map.Entry<String, RequestConfigProperties> a, Map.Entry<String, RequestConfigProperties> b) {
		return a.getKey().compareTo(b.getKey());
	}
	
	@Data
	static public class JmxConfig {
		private String domain = "default";
		private String metricNameStrategy = "HOST_AND_METHOD";
	}
}
