package io.github.springboot.httpclient5.core.config;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ConcurrentLruCache;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.springboot.httpclient5.core.config.model.CharCodingConfigProperties;
import io.github.springboot.httpclient5.core.config.model.ConnectionConfigProperties;
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
	private static final int DEFAULT_MAX_CONNEXION_PER_HOST = 50;
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	public static final String DEFAULT_HOST_KEY = "default";
	
	public static Timeout DEFAULT_CONNECT_TIMEOUT = Timeout.ofSeconds(3) ;
	public static Timeout DEFAULT_SOCKET_TIMEOUT = Timeout.ofSeconds(30) ;
	public static Timeout DEFAULT_RESPONSE_TIMEOUT = DEFAULT_SOCKET_TIMEOUT;
	public static Timeout DEFAULT_CONNECTION_REQUEST_TIMEOUT = DEFAULT_CONNECT_TIMEOUT ;
	
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
	
	private boolean autoconfig = true; 

	@Autowired(required = false)
	private RequestConfigCustomizer requestConfigCustomizer = RequestConfigCustomizer.NOOP; 
	
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
		
		Map<String, ConnectionConfigProperties> ehc = new HashMap<String, ConnectionConfigProperties>();
		pool.getHostConfig().forEach((k, v) -> ehc.put(normalizeAndExpandHost(k), v));
		pool.setHostConfig(ehc) ;
		
		if (autoconfig) {
			managedDefaultsOnZeroConf(pool);
		}
	}

	private String normalizeAndExpandHost(String uri) {
		try {
			HttpHost httpHost = HttpHost.create(env.resolvePlaceholders(uri)) ;
			if (httpHost.getPort() == -1) {
				httpHost = "https".equals(httpHost.getSchemeName()) ? 
						HttpHost.create(env.resolvePlaceholders(uri+":443"))
						:
						HttpHost.create(env.resolvePlaceholders(uri+":80")) ;

			}
			
			return httpHost.toURI();
		} catch (URISyntaxException e) {
			return uri ;
		}
	}

	private void managedDefaultsOnZeroConf(ConnectionManagerConfigProperties cpool) {
		if (requestConfig.isEmpty()) {
			RequestConfigProperties defaultRequestConfig = new RequestConfigProperties();
			defaultRequestConfig.setResponseTimeout(DEFAULT_RESPONSE_TIMEOUT) ;
			defaultRequestConfig.setConnectionRequestTimeout(DEFAULT_CONNECTION_REQUEST_TIMEOUT) ;
			
			requestConfig.put(HttpClient5Config.DEFAULT_HOST_KEY, defaultRequestConfig) ;
		}
		
		if (pool.getHostConfig().isEmpty()) {
			cpool.setMaxConnPerRoute(DEFAULT_MAX_CONNEXION_PER_HOST) ;
			cpool.setPoolConcurrencyPolicy(PoolConcurrencyPolicy.LAX) ;
	
			ConnectionConfig connectionConfig = ConnectionConfig.custom()
					.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT)
					.setSocketTimeout(DEFAULT_SOCKET_TIMEOUT)
					.setTimeToLive(TimeValue.ofSeconds(120))  		   // Usual 
					.setValidateAfterInactivity(Timeout.ofSeconds(20)) // Usual tomcat connection timeout
					.build();
			cpool.setDefaultConnectionConfig(connectionConfig) ;

			SocketConfig socketConfig = SocketConfig.custom()
					.setSoTimeout(DEFAULT_SOCKET_TIMEOUT)
					.build();
			cpool.setDefaultSocketConfig(socketConfig) ;
		}
		
	}
	
	@SneakyThrows
	public RequestConfig getRequestConfig(String method, String uri) {
		return getRequestConfigProperties(method, uri).build() ;
	}
	
	@SneakyThrows
	public RequestConfigProperties getRequestConfigProperties(String method, String uri) {
		RequestConfigProperties properties = new RequestConfigProperties(requestConfigPropertiesCache.get(Pair.of(method, uri))) ;
		return requestConfigCustomizer.customizeRequestConfigProperties(method, uri, properties) ;
//		return requestConfigPropertiesCache.get(Pair.of(method, uri)) ;
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
				log.debug("Applying request config for {}", key);
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
