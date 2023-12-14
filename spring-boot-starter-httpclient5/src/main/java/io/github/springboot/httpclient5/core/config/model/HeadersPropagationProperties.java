package io.github.springboot.httpclient5.core.config.model;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.NestedConfigurationProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class HeadersPropagationProperties {
	private Boolean enabled;
	private List<String> down;
	private List<String> up;

	@NestedConfigurationProperty
	@Builder.Default
	private Map<String, String> add = new HashMap<>();
	
	@NestedConfigurationProperty
	@Builder.Default
	private List<String> remove = new ArrayList<>();
}
