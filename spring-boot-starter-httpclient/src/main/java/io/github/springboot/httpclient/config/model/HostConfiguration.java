package io.github.springboot.httpclient.config.model;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @inheritDoc
 */
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class HostConfiguration {
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private Pattern pattern;

	private String baseUrl;
	private Authentication authentication = new Authentication();
	private Map<String, MethodConfiguration> methods = new HashMap<String, MethodConfiguration>();
	private ConnectionConfiguration connection = new ConnectionConfiguration();
	private MonitoringConfiguration monitoring = new MonitoringConfiguration();
	private HeadersPropagation headersProparation = new HeadersPropagation();

	private ProxyConfiguration proxy = null;
	private String brokenCircuitAction;
	private Integer retryAttempts;
	private Integer retryWaitDuration;

	public boolean matches(String url) {
		if (pattern == null) {
			pattern = Pattern.compile(baseUrl);
		}
		return url.startsWith(baseUrl) || pattern.matcher(url).matches();
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
	}
}
