package org.springframework.boot.httpclient.config.model;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@EqualsAndHashCode
@Configuration
@ConfigurationProperties("app.httpclient")
public class HttpClientConfiguration {

	private static final String DEFAULT_JMX_DOMAIN = "monitoring";
	private static final String DEFAULT_JMX_APPLICATION = "httpclient";
	private String jmxDomain = DEFAULT_JMX_DOMAIN;
	private String jmxApplication = DEFAULT_JMX_APPLICATION;
	private Integer poolTimeout = 30000;
	private Long poolIdleTimeout = 300000l;
	private Integer lingerTimeout = -1;
	private Boolean resolveDns = false;
	private String metricNameStrategy;
	private String brokenCircuitAction;
	private Integer retryAttempts;
	private Integer retryWaitDuration;

	private ConnectionConfiguration connection = new ConnectionConfiguration();
	private MonitoringConfiguration monitoring = new MonitoringConfiguration();
	private ProxyConfiguration proxy = new ProxyConfiguration();
	private HeadersPropagation headersProparation = new HeadersPropagation() ;

	private Map<String, HostConfiguration> hosts = new HashMap<String, HostConfiguration>();

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE) ;
	}

}
