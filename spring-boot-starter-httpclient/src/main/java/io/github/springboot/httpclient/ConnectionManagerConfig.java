package io.github.springboot.httpclient;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.http.HttpHost;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.HttpClientConnectionOperator;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.conn.DefaultHttpClientConnectionOperator;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

import io.github.springboot.httpclient.config.HttpClientConfigurationHelper;
import io.github.springboot.httpclient.config.model.Authentication;
import io.github.springboot.httpclient.config.model.HostConfiguration;
import io.github.springboot.httpclient.config.model.HttpClientConfiguration;
import io.github.springboot.httpclient.config.model.ProxyConfiguration;
import io.github.springboot.httpclient.constants.ConfigurationConstants;
import io.github.springboot.httpclient.constants.HttpClientConstants;
import io.github.springboot.httpclient.internal.ConfigurableHostnameVerifier;
import io.github.springboot.httpclient.ssl.ConfigurablePrivateKeyStrategy;
import io.github.springboot.httpclient.ssl.ConfigurableTrustSslStrategy;
import io.github.springboot.httpclient.utils.HostUtils;
import io.github.springboot.httpclient.utils.HttpClientUtils;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
@EnableScheduling
public class ConnectionManagerConfig {

	private static final String DEFAULT_JRE_TRUSTSTORE_FILE_JDK8 = System.getProperty("java.home")
			+ "/jre/lib/security/cacerts";
	private static final String DEFAULT_JRE_TRUSTSTORE_FILE_JDK11 = System.getProperty("java.home")
			+ "/lib/security/cacerts";

	@Autowired
	protected HttpClientConfigurationHelper configHelper;

	@Bean
	public HostnameVerifier getHostnameVerifier() {
		return new ConfigurableHostnameVerifier(configHelper);
	}

	@Bean
	public Registry<ConnectionSocketFactory> connectionSocketFactoryRegistry() {

		RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder.<ConnectionSocketFactory>create()
				.register(HostUtils.HTTP, PlainConnectionSocketFactory.getSocketFactory())
				.register(HostUtils.HTTPS, new SSLConnectionSocketFactory(getSslContext(), getHostnameVerifier()));
		return registryBuilder.build();
	}

	@Bean
	@ConditionalOnMissingBean(HttpClientConnectionOperator.class)
	public HttpClientConnectionOperator defaultHttpClientConnectionOperator(
			Registry<ConnectionSocketFactory> registry) {
		return new DefaultHttpClientConnectionOperator(registry, null, null);
	}

	@Bean
	@ConditionalOnMissingBean(PoolingHttpClientConnectionManager.class)
	public PoolingHttpClientConnectionManager connectionManager(HttpClientConnectionOperator operator) {
		return new PoolingHttpClientConnectionManager(operator, null, -1, TimeUnit.MILLISECONDS);
	}

	@Bean
	public HttpClientConnectionManagerCustomizerSupport httpClientConnectionManagerCustomizerSupport(
			ObjectProvider<HttpClientConnectionManagerCustomizer> httpClientConnectionManagerCustomizers,
			PoolingHttpClientConnectionManager cm) {
		return new HttpClientConnectionManagerCustomizerSupport(httpClientConnectionManagerCustomizers, cm);
	}

	public static class HttpClientConnectionManagerCustomizerSupport {
		public HttpClientConnectionManagerCustomizerSupport(
				ObjectProvider<HttpClientConnectionManagerCustomizer> customizers,
				PoolingHttpClientConnectionManager cm) {
			customizers.orderedStream().forEach(c -> c.customize(cm));
		}
	}

	@Bean
	public HttpClientConnectionManagerCustomizer defaultHttpClientConnectionManagerCustomizerSupport() {
		return cm -> {
			final int maxConnection = configHelper
					.getGlobalConfiguration(ConfigurationConstants.MAX_ACTIVE_CONNECTIONS);

			cm.setMaxTotal(maxConnection);
			cm.setDefaultMaxPerRoute(maxConnection);

			for (final HostConfiguration h : configHelper.getAllConfigurations().getHosts().values()) {
				final HttpRoute httpRoute = getHttpRoute(h.getBaseUrl());
				if (httpRoute == null) {
					continue;
				}

				final Integer maxRoute = configHelper.getConfiguration(h.getBaseUrl(),
						ConfigurationConstants.MAX_ACTIVE_CONNECTIONS);
				cm.setMaxPerRoute(httpRoute, maxRoute);

				final Integer bufferSize = configHelper.getConfiguration(h.getBaseUrl(),
						ConfigurationConstants.BUFFER_SIZE);
				ConnectionConfig connectionConfig = ConnectionConfig.custom().setBufferSize(bufferSize).build();
				cm.setConnectionConfig(httpRoute.getTargetHost(), connectionConfig);
			}

			final int lingerTimeout = configHelper.getGlobalConfiguration(ConfigurationConstants.LINGER_TIMEOUT);
			final int socketTimeout = configHelper.getGlobalConfiguration(ConfigurationConstants.SOCKET_TIMEOUT);

			final SocketConfig defaultSocketConfig = SocketConfig.custom().setSoTimeout(socketTimeout)
					.setSoLinger(lingerTimeout).build();
			cm.setDefaultSocketConfig(defaultSocketConfig);
		};
	}

	protected SSLContext getSslContext() {
		SSLContext systemDefault = SSLContexts.createSystemDefault();
		try {
			final Object contextSpi = FieldUtils.readField(systemDefault, "contextSpi", true);
			final X509TrustManager systemTrustManager = (X509TrustManager) FieldUtils.readField(contextSpi,
					"trustManager", true);
			SSLContextBuilder builder = SSLContexts.custom();
			// System.getProperty(HttpClientConstants.TRUSTSTORE);
			String TRUSTSTORE = System.getProperty(HttpClientConstants.TRUSTSTORE, getDefaultTrustStorePath());
			String TRUSTSTORE_TYPE = System.getProperty(HttpClientConstants.TRUSTSTORE_TYPE,
					configHelper.getGlobalConfiguration(ConfigurationConstants.TRUST_STORE_TYPE));
			String TRUSTSTORE_PASSWORD = System.getProperty(HttpClientConstants.TRUSTSTORE_PASSWORD,
					configHelper.getGlobalConfiguration(ConfigurationConstants.TRUST_STORE_PASSWORD));

			KeyStore trustStoreKeystore = getStore(TRUSTSTORE, TRUSTSTORE_PASSWORD, TRUSTSTORE_TYPE);

			if (trustStoreKeystore != null) {
				builder.loadTrustMaterial(trustStoreKeystore,
						new ConfigurableTrustSslStrategy(systemTrustManager, configHelper));
			}

			HttpClientConfiguration configs = configHelper.getAllConfigurations();
			for (Entry<String, HostConfiguration> hostconfiguration : configs.getHosts().entrySet()) {
				Authentication authentication = hostconfiguration.getValue().getAuthentication();
				if (authentication != null) {
					boolean authTypeCert = authentication.getAuthType().equals(Authentication.AUTH_TYPE_CERT);

					if (authTypeCert) {
						String keystore = StringUtils.isBlank(authentication.getAuthKeyStore())
								|| authentication.getAuthKeyStore().equals(Authentication.SYSTEM_DEFAULT)
										? System.getProperty(HttpClientConstants.KEYSTORE)
										: authentication.getAuthKeyStore();
						String keystorePassword = StringUtils.isNotBlank(authentication.getAuthKeyStorePassword())
								? authentication.getAuthKeyStorePassword()
								: System.getProperty(HttpClientConstants.KEYSTORE_PASSWORD);
						String keystoreType = StringUtils.isNotBlank(authentication.getAuthKeyStoreType())
								? authentication.getAuthKeyStoreType()
								: System.getProperty(HttpClientConstants.KEYSTORE_TYPE);
						KeyStore clientKeyStore = getStore(keystore, keystorePassword, keystoreType);

						builder.loadKeyMaterial(clientKeyStore, keystorePassword.toCharArray(),
								new ConfigurablePrivateKeyStrategy(configHelper));

					}
				}
			}

			return builder.build();
		} catch (KeyManagementException | IllegalAccessException | NoSuchAlgorithmException | KeyStoreException e) {
			log.warn("Erreur to load trustStore or keyStore", e);
			return systemDefault;
		} catch (CertificateException e2) {
			log.warn("Erreur to load Certificate", e2);
			return systemDefault;
		} catch (UnrecoverableKeyException e) {
			log.warn("Erreur to load keyStore file, UnrecoverableKeyException", e);
			return systemDefault;
		}
	}
//    }

	private String getDefaultTrustStorePath() {
		String TRUSTSTORE = System.getProperty(HttpClientConstants.TRUSTSTORE);
		if (TRUSTSTORE == null) {
			TRUSTSTORE = DEFAULT_JRE_TRUSTSTORE_FILE_JDK8;

			if (!Files.exists(Paths.get(TRUSTSTORE))) {
				TRUSTSTORE = DEFAULT_JRE_TRUSTSTORE_FILE_JDK11;
			}
		}
		return TRUSTSTORE;
	}

	protected HttpRoute getHttpRoute(final String uri) {

		HttpHost httpHost = null;
		try {
			httpHost = HttpClientUtils.getHttpHost(uri);
		} catch (URISyntaxException e) {
			log.warn("Invalide hostname in base url, note that regexp are supported only in path", e);
			return null;
		}

		HttpRoute httpRoute = null;
		ProxyConfiguration proxyConfig = configHelper.getProxyConfiguration(uri);

		if (proxyConfig != null) {
			final String proxyHostName = proxyConfig.getHost();
			final int proxyPort = proxyConfig.getPort();
			httpRoute = new HttpRoute(httpHost, null, new HttpHost(proxyHostName, proxyPort),
					httpHost.getPort() == HostUtils.HTTPS_PORT);
		} else {
			httpRoute = new HttpRoute(httpHost);
		}
		return httpRoute;
	}

	@Bean("legacyTaskScheduler")
	public TaskScheduler taskScheduler() {
		return new ConcurrentTaskScheduler();
	}

	@Bean("legacyIdleConnectionMonitor")
	public Runnable idleConnectionMonitor(final PoolingHttpClientConnectionManager connectionManager) {
		return new Runnable() {
			@Override
			@Scheduled(fixedDelay = 10000)
			public void run() {
				try {
					if (connectionManager != null) {
						log.trace("run IdleConnectionMonitor - Closing expired and idle connections...");
						connectionManager.closeExpiredConnections();
						connectionManager.closeIdleConnections(
								configHelper.getGlobalConfiguration(ConfigurationConstants.POOL_IDLE_TIMEOUT),
								TimeUnit.SECONDS);
					} else {
						log.trace("run IdleConnectionMonitor - Http Client Connection manager is not initialised");
					}
				} catch (final Exception e) {
					log.error("run IdleConnectionMonitor - Exception occurred. msg={}, e={}", e.getMessage(), e);
				}
			}

		};
	}

	public static KeyStore getStore(final String storeFileName, final String password, String storeType)
			throws KeyStoreException, CertificateException, NoSuchAlgorithmException,
			java.security.cert.CertificateException {
		KeyStore store = null;
		if (StringUtils.isNotBlank(storeFileName) && password != null) {
			if (StringUtils.isBlank(storeType)) {
				storeType = HttpClientConstants.KEYSTORE_DEFAULT_TYPE;
			}
			store = KeyStore.getInstance(storeType);
			try (InputStream inputStream = new FileInputStream(storeFileName)) {
				store.load(inputStream, password.toCharArray());
			} catch (IOException e) {
				log.info("Unable to load store={} of type={} with pass {}", storeFileName, storeType, password, e);
			}
		}
		return store;
	}

}
