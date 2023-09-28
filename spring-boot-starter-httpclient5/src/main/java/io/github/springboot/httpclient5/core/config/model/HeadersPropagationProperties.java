package io.github.springboot.httpclient5.core.config.model;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.NestedConfigurationProperty;

import lombok.Data;

@Data
public class HeadersPropagationProperties {
	private Boolean enabled;
	private String down;
	private String up;
	@NestedConfigurationProperty
	private Map<String, String> add = new HashMap<>();
	@NestedConfigurationProperty
	private List<String> remove = new ArrayList<>();
}
