package io.github.springboot.httpclient5.core.config.model;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.commons.text.WordUtils;
import org.apache.hc.client5.http.config.RequestConfig;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import io.github.springboot.httpclient5.core.config.ConfigProvider;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor
public class RequestConfigProperties implements ConfigProvider<RequestConfig> {
	private static Map<String, Object> DEFAULT_PROPS ; 
	static {
		try {
			DEFAULT_PROPS = PropertyUtils.describe(new RequestConfigProperties()) ;
		} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ignore) {
		}
	}

	@Delegate
	private RequestConfig.Builder builder = RequestConfig.custom();
	
	@Getter @Setter
	@NestedConfigurationProperty
	private ErrorManagementProperties errorManagement = new ErrorManagementProperties() ;

	@Getter @Setter
	@NestedConfigurationProperty
	private HeadersPropagationProperties headersPropagation = new HeadersPropagationProperties();
	
	@Getter @Setter
	@NestedConfigurationProperty
	private SimplePredefinedCredentialsProvider credentials ;

	@Getter @Setter
	@NestedConfigurationProperty
	private RetryConfig retryConfig = new RetryConfig();

	
	@Getter @Setter
	@NestedConfigurationProperty
	private Map<String, String> customRequestContext = new HashMap<>() ;

	public RequestConfigProperties(RequestConfigProperties origin) {
		if (origin != null) {
			this.builder = RequestConfig.copy(origin.build());
			this.errorManagement = origin.getErrorManagement() ;
			this.headersPropagation = origin.getHeadersPropagation() ;
			this.credentials = origin.getCredentials();
			this.retryConfig = origin.getRetryConfig();
		}
		else {
			this.builder = RequestConfig.copy(RequestConfig.DEFAULT);
		}
	}
	
	@Delegate
	public RequestConfig get() {
		return builder.build();
	}
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this.get(), ToStringStyle.JSON_STYLE);
	}

	@SneakyThrows
	public RequestConfigProperties apply(RequestConfigProperties other) {
		// describe RequestConfig default and other as Map<String, Object)
		// Compare maps and set property from other if not identical to default
		PropertyUtils.describe(other)
				.entrySet().stream()
				.filter(e -> !Objects.equals(DEFAULT_PROPS.get(e.getKey()), e.getValue()))
				.filter(e -> e.getValue() != null)
				.forEach(e -> {
					log.debug("Applying request config : {} -> {}", e.getKey(), e.getValue());
					try {
						// TODO : opt String concat (pas possible de faire un PropertyUtils.setProperty car setter respecte pas la norme (return type != void)
						MethodUtils.invokeMethod(this, "set" + WordUtils.capitalize(e.getKey()), e.getValue()) ;
					} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
						log.error("Error applying {}", e.getKey(), ex);
					}
				});
		
		return this;
	}
}
