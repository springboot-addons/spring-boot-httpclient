package io.github.springboot.httpclient.ssl;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.ssl.TrustStrategy;

import io.github.springboot.httpclient.config.HttpClientConfigurationHelper;
import io.github.springboot.httpclient.config.model.HostConfiguration;
import io.github.springboot.httpclient.constants.ConfigurationConstants;
import io.github.springboot.httpclient.constants.HttpClientConstants;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConfigurableTrustSslStrategy implements TrustStrategy {

	private X509TrustManager systemTrustManager;
	private HttpClientConfigurationHelper configHelper;

	public ConfigurableTrustSslStrategy(X509TrustManager systemTrustManager,
			HttpClientConfigurationHelper configHelper) {
		this.systemTrustManager = systemTrustManager;
		this.configHelper = configHelper;
	}

	@Override
	public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		LdapName ldapName = null;
		try {
			ldapName = new LdapName(chain[0].getSubjectX500Principal().getName());
		} catch (InvalidNameException e1) {
			return false;
		}
		String hostname = null;
		try {
			// Collections.reverse(ldapName.getRdns());
			// usually CN comes at the end
			Object cn = ldapName.getRdn(ldapName.size() - 1).getValue();

			if (cn instanceof String) {
				hostname = (String) cn;
			} else {
				List<Rdn> rdns = new ArrayList<>(ldapName.getRdns());
				Collections.reverse(rdns);
				for (Rdn rdn : rdns) {
					Object rdnValue = rdn.getValue();
					if (rdnValue instanceof String) {
						hostname = (String) rdnValue;
						break;
					}
				}
			}

			final String sslHostName = (StringUtils.startsWith(hostname,
					HttpClientConstants.WILDCARD_PREFIX_CERTIFICATE))
							? StringUtils.substringAfter(hostname, HttpClientConstants.WILDCARD_PREFIX_CERTIFICATE)
							: hostname;

			Boolean trustAll = configHelper.getGlobalConfiguration(ConfigurationConstants.TRUST_SSL);
			Boolean trustSsl = Boolean.FALSE;

			HostConfiguration hostConfiguration = configHelper.getUniqueConfigurationForHostname(sslHostName);

			if (hostConfiguration == null) {

				log.debug("No config key : '{}' found...Checking other configurations", sslHostName);

				Map<String, HostConfiguration> hosts = configHelper.getAllConfigurations().getHosts();
				hostConfiguration = hosts.entrySet().stream()
						.filter(e -> e.getValue().getConnection().getTrustSslDomains().contains(sslHostName))
						.map(Map.Entry::getValue).findFirst().orElse(null);
			}

			if (hostConfiguration != null) {
				trustSsl = hostConfiguration.getConnection().getTrustSsl();
			}

			if (trustAll || trustSsl) {
				log.debug("Config key : {}/{} for host {} is Trust={} on {}", ConfigurationConstants.TRUST_SSL,
						ConfigurationConstants.TRUST_SSL_DOMAIN, hostname, trustSsl, sslHostName);
				return trustSsl;
			} else {
				try {
					systemTrustManager.checkClientTrusted(chain, authType);
					return true;
				} catch (Exception e) {
					log.warn(
							"Unable to find config key : {}/{} for host {} ; check your HTTP configuration; using system trustStore witch failed",
							ConfigurationConstants.TRUST_SSL, ConfigurationConstants.TRUST_SSL_DOMAIN, hostname, e);
					return false;
				}
			}

		} catch (Exception e) {
			log.warn("Unable to auto trust SSL hostname : {}; check your configuration", hostname, e);
			return false;
		}

	}

}
