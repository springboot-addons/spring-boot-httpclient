package io.github.springboot.httpclient.core.constants;

public interface ConfigurationConstants {
	String JMX_DOMAIN = "jmxDomain";
	String POOL_TIMEOUT = "poolTimeout";
	String POOL_IDLE_TIMEOUT = "poolIdleTimeout";
	String LINGER_TIMEOUT = "lingerTimeout";
	String HTTP_PREFIX = "http";
	String TRUST_ALL_HOSTS = "trustSsl";
	String METRIC_NANE_STRATEGY = "metricNameStrategy";
	String HEADERS_MANAGEMENT = "headers";

	String MAX_ACTIVE_CONNECTIONS = "connection.maxActive";
	String SOCKET_TIMEOUT = "connection.socketTimeout";
	String CONNECTION_TIMEOUT = "connection.connectTimeout";
	String BUFFER_SIZE = "connection.bufferSize";
	String USER_AGENT = "connection.userAgent";
	String COMPRESSION = "connection.compression";
	String COOKIE_POLICY = "connection.cookiePolicy";
	String TRUST_SSL = "connection.trustSsl";
	String TRUST_SSL_CLIENT = "connection.trustSslClient";
	String TRUST_SSL_CLIENT_ALIAS = "connection.trustSslClientAlias";
	String TRUST_SSL_DOMAIN = "connection.trustSslDomains";

	String TRUST_STORE_FILE = "connection.trustStoreFile";
	String TRUST_STORE_TYPE = "connection.trustStoreType";
	String TRUST_STORE_PASSWORD = "connection.trustStorePassword";

	String DELAY_BEFORE_RETRY = "connection.delayBeforeRetrying";

	String LOG_POST_METHODS = "monitoring.logPostMethods";

	String PROXY_HOST = "proxy.host";
	String PROXY_PORT = "proxy.port";
	String PROXY_USE = "proxy.useProxy";
	String PROXY_AUTHENTICATION_USER = "proxy.authentication.user";
	String PROXY_AUTHENTICATION_PASSWORD = "proxy.authentication.password";
	String PROXY_AUTHENTICATION_DOMAIN = "proxy.authentication.domain";
	String PROXY_AUTHENTICATION_AUTH_TYPE = "proxy.authentication.authType";
	String PROXY_AUTHENTICATION_PREEMPTIVE = "proxy.authentication.preemptive";
	String PROXY_AUTHENTICATION_CREDENTIALS_CHARSET = "proxy.authentication.credentialsCharset";
	String PROXY_AUTHENTICATION_REQUIRED = "proxy.authentication.required";
	String PROXY_CONFIGURATION = "proxy";

	String AUTHENTICATION_USER = "authentication.user";
	String AUTHENTICATION_PASSWORD = "authentication.password";
	String AUTHENTICATION_DOMAIN = "authentication.domain";
	String AUTHENTICATION_AUTH_TYPE = "authentication.authType";
	String AUTHENTICATION_AUTH_ENDPOINT = "authentication.authEndpoint";
	String AUTHENTICATION_PREEMPTIVE = "authentication.preemptive";
	String AUTHENTICATION_CREDENTIALS_CHARSET = "authentication.credentialsCharset";
	String AUTHENTICATION_REQUIRED = "authentication.required";

	String DISABLE_COOKIES_MANAGEMENT = "disable-all";
	String ON_BROKEN_CIRCUIT = "brokenCircuitAction";

	String RETRY_ATTEMPTS = "retryAttempts";
	String RETRY_WAIT_DURATION = "retryWaitDuration";
}