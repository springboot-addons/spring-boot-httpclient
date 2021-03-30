package io.github.springboot.httpclient.core.config;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.github.springboot.httpclient.core.config.model.HostConfiguration;
import io.github.springboot.httpclient.core.config.model.HttpClientConfiguration;
import io.github.springboot.httpclient.core.config.model.MethodConfiguration;
import io.github.springboot.httpclient.core.config.model.ProxyConfiguration;
import io.github.springboot.httpclient.core.constants.ConfigurationConstants;
import io.github.springboot.httpclient.core.utils.HttpClientUtils;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class HttpClientConfigurationHelper {

    @Autowired
    private HttpClientConfiguration config;

    @PostConstruct
    public void check() {
        log.debug("Config : " + ToStringBuilder.reflectionToString(config, ToStringStyle.JSON_STYLE));
    }

    private final Map<ConfigKey, Object> configurationCache = new ConcurrentHashMap<>();

    public boolean isCookieManagementDisabled() {
        final String cookiePolicy = getGlobalConfiguration(ConfigurationConstants.COOKIE_POLICY);
        return HttpClientUtils.isDisabledCookiePolicy(cookiePolicy);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.pagesjaunes.commun.core.httpclient.config.hierarchical.
     * HttpClientConfigurationHelper#getConfiguration(java.lang.String)
     */

    public HostConfiguration getUniqueConfigurationForHostname(String hostname) {
        Optional<Entry<String, HostConfiguration>> entry = config.getHosts().entrySet().stream()
                .filter(e -> e.getValue().getBaseUrl().contains(hostname)).findFirst();
        return entry.isPresent() ? entry.get().getValue() : null;
    }

    @SuppressWarnings("unchecked")

    public <E> E getGlobalConfiguration(String key) {
        final ConfigKey configKey = new ConfigKey(key);
        E value = fromCache(configKey);
        if (value == null) {
            try {
                value = (E) PropertyUtils.getProperty(config, key);
                log.debug("Found property {} = {} at top level", key, value);
            } catch (final Exception e) {
                log.debug("No property {} found at top level", key);
            }
            toCache(configKey, value);
        }
        return value;
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.pagesjaunes.commun.core.httpclient.config.hierarchical.
     * HttpClientConfigurationHelper#getConfiguration(java.lang.String,
     * java.lang.String, java.lang.String, java.lang.String)
     */
    @SuppressWarnings("unchecked")

    public <E> E getConfiguration(String uri, String method, String key) {
        log.debug("Looking for property {} for {} {}", key, method, uri);

        final ConfigKey configKey = new ConfigKey(uri, method, key);
        E value = fromCache(configKey);
        if (value == null) {
            final HostConfiguration hostConfiguration = config.getHosts().get(getConfigurationKeyForRequestUri(uri));
            if (hostConfiguration != null) {
                final MethodConfiguration methodConfiguration = hostConfiguration.getMethods().get(method);
                try {
                    value = (E) PropertyUtils.getProperty(methodConfiguration, key);
                    if (value != null) {
                        log.debug("Found property {} = {} at method level for {} {}", key, value, method, uri);
                    }
                } catch (final Exception e) {
                    log.debug("No property {} found at method level, configuration uri {}", key, uri);
                }
                if (value == null) {
                    try {
                        value = (E) PropertyUtils.getProperty(hostConfiguration, key);
                        log.debug("Found property {} = {} at host level for {} {}", key, value, hostConfiguration, uri);
                    } catch (final Exception e) {
                        log.debug("No property {} found at host level", key);
                    }
                }
            }
            if (value == null) {
                try {
                    value = (E) PropertyUtils.getProperty(config, key);
                    log.debug("Found property {} = {} at top level for {} {} {}", key, value, method, uri);
                } catch (final Exception e) {
                    log.debug("No property {} found at top level", key);
                }
            }
            toCache(configKey, value);
        }

        return value;
    }

    private static final Object NULL_VALUE = new Object();

    @SuppressWarnings("unchecked")
    private <E> E fromCache(ConfigKey configKey) {
        final E value = (E) configurationCache.get(configKey);
        return value == NULL_VALUE ? null : value;
    }

    private <E> void toCache(ConfigKey configKey, E value) {
        configurationCache.put(configKey, value == null ? NULL_VALUE : value);
    }

    public boolean isTrue(String uri, String method, String key) {
        final Boolean configuration = getConfiguration(uri, method, key);
        return configuration != null && configuration;
    }

    public boolean isTrue(String uri, String key) {
        final Boolean configuration = getConfiguration(uri, StringUtils.EMPTY, key);
        return configuration != null && configuration;
    }

    public <E> E getConfiguration(String uri, String key) {
        return getConfiguration(uri, StringUtils.EMPTY, key);
    }

    public HttpClientConfiguration getAllConfigurations() {
        return config;
    }

    public boolean useProxyForHost(final String uri) {
        return isTrue(uri, ConfigurationConstants.PROXY_USE);
    }

    public boolean useAuthentication(final String uri) {
        return isTrue(uri, ConfigurationConstants.AUTHENTICATION_USER);
    }

    public boolean isTrue(String key) {
        final Boolean configuration = getGlobalConfiguration(key);
        return configuration != null && configuration;
    }

    public String getConfigurationKeyForRequestUri(String requestUri) {
        return getConfigurationKeyForRequestUri(requestUri, null);
    }

    public String getConfigurationKeyForRequestUri(String requestUri, String defaultKeyname) {
        final List<String> matchingKeys = config.getHosts().entrySet().stream()
                .filter(h -> h.getValue().matches(requestUri)).map(e -> e.getKey()).collect(Collectors.toList());

        if (matchingKeys.isEmpty()) {
            return defaultKeyname;
        } else {
            final String key = matchingKeys.get(0);
            if (matchingKeys.size() > 1) {
                log.warn("More than one config key matching for '{}', using '{}'", requestUri, key);
            }
            return key;
        }
    }

    public ProxyConfiguration getProxyConfiguration(String uri) {
        ProxyConfiguration pc = null;

        if (useProxyForHost(uri)) {
            pc = getConfiguration(uri, ConfigurationConstants.PROXY_CONFIGURATION);
            if (StringUtils.isBlank(pc.getHost())) {
                pc = getGlobalConfiguration(ConfigurationConstants.PROXY_CONFIGURATION);
            }
        }

        return pc;
    }

}
