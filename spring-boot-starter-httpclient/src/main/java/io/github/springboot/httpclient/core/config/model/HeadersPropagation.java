package io.github.springboot.httpclient.core.config.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "httpclient.headers")
public class HeadersPropagation {
	private boolean enabledPropagation = false;
	private Pattern downPattern = Pattern.compile("X[-].*");
	private Pattern upPattern = Pattern.compile("X[-].*");
	private Map<String, String> add = new HashMap<>();
	private List<String> remove = new ArrayList<>();

	public void setDown(String down) {
		downPattern = Pattern.compile(down);
	}

	public void setUp(String up) {
		upPattern = Pattern.compile(up);
	}

}
