package io.github.springboot.httpclient.ssl;

import java.net.Socket;
import java.util.Map;

import org.apache.http.ssl.PrivateKeyDetails;
import org.apache.http.ssl.PrivateKeyStrategy;

import io.github.springboot.httpclient.config.HttpClientConfigurationHelper;
import io.github.springboot.httpclient.config.model.Authentication;
import io.github.springboot.httpclient.config.model.HostConfiguration;

public class ConfigurablePrivateKeyStrategy implements PrivateKeyStrategy {

	private HttpClientConfigurationHelper config;

	public ConfigurablePrivateKeyStrategy(HttpClientConfigurationHelper config) {
		this.config = config;
	}

	@Override
	public String chooseAlias(Map<String, PrivateKeyDetails> aliases, Socket socket) {

		Map<String, HostConfiguration> hosts = config.getAllConfigurations().getHosts();
		HostConfiguration hostConfiguration = hosts.entrySet().stream()
				.filter(e -> e.getValue().getConnection().getTrustSslDomains()
						.contains(socket.getInetAddress().getHostName() + ":" + socket.getPort()))
				.map(Map.Entry::getValue).findFirst().orElse(null);

		Authentication authentication = hostConfiguration.getAuthentication();

		if (hostConfiguration != null && authentication.getAuthType().equals(Authentication.AUTH_TYPE_CERT)) {
			String clientAlias = authentication.getAuthKeyAlias();
			if (aliases.keySet().contains(clientAlias)) {
				return clientAlias;
			}
		}

		return null;
	}

}
