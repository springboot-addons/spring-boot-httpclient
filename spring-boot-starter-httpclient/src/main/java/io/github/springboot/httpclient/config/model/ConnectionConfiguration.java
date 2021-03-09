package io.github.springboot.httpclient.config.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@EqualsAndHashCode
public class ConnectionConfiguration {

	public final static Double DEFAULT_DELAY = 0.5;

	private Integer maxActive;
	private Integer socketTimeout;
	private Integer connectTimeout;
	private Integer bufferSize = 4096;
	private String userAgent = "httpclient";
	private String compression;
	private String requestHeader;
	private String cookiePolicy = "default";
	// private RateLimiter limiter;
	private Double delayBeforeRetrying = DEFAULT_DELAY;
	private List<String> removeHeaders = new ArrayList<String>();
	private Boolean trustSsl = false;
	private Boolean trustSslClient = false;
	private String trustSslClientAlias = null;
	private List<String> trustSslDomains = new ArrayList<String>();
	private String trustStoreFile = null; // = "/opt/java/openjdk/lib/security/cacerts";
	private String trustStoreType = "JKS";
	private String trustStorePassword = "changeit";

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
	}

}