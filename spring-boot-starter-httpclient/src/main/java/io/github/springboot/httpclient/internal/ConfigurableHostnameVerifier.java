package io.github.springboot.httpclient.internal;

import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.apache.http.conn.ssl.DefaultHostnameVerifier;

import io.github.springboot.httpclient.config.HttpClientConfigurationHelper;
import io.github.springboot.httpclient.config.model.HostConfiguration;
import io.github.springboot.httpclient.constants.ConfigurationConstants;
import io.github.springboot.httpclient.utils.HostUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConfigurableHostnameVerifier implements HostnameVerifier {
  private final HttpClientConfigurationHelper config;
  private static HostnameVerifier DEFAULT = new DefaultHostnameVerifier();

  public ConfigurableHostnameVerifier(HttpClientConfigurationHelper config) {
    this.config = config;
  }

  @Override
  public boolean verify(String hostname, SSLSession session) {
    String uri = HostUtils.HTTPS + "://" + hostname;
    if (HostUtils.HTTPS_PORT != session.getPeerPort()) {
      uri += ":" + session.getPeerPort();
    }
    Boolean trust = config.getConfiguration(uri, ConfigurationConstants.TRUST_SSL);
    log.debug("HostNameVerifier for {} gives {}", uri, trust);

    if (!trust) {
      Map<String, HostConfiguration> hosts = config.getAllConfigurations().getHosts();
      HostConfiguration hostConfiguration = hosts.entrySet().stream()
          .filter(e -> e.getValue().getConnection().getTrustSslDomains().contains(hostname)).map(Map.Entry::getValue)
          .findFirst().orElse(null);

      if (hostConfiguration != null && hostConfiguration.getConnection() != null) {
        trust = hostConfiguration.getConnection().getTrustSsl();
      }

      log.info("Search into SSL domains for {} gives {}", uri, trust);
    }

    if (trust != null) {
      return trust;
    } else {
      log.warn("HostNameVerifier for {} gives {} ; using {} HostNameVerifier", hostname, trust, DEFAULT);
      return DEFAULT.verify(hostname, session);
    }
  }
}