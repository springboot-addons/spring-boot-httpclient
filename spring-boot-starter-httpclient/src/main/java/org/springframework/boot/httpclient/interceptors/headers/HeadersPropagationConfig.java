package org.springframework.boot.httpclient.interceptors.headers;

import java.util.regex.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix= "app.httpclient.headers")
@Data
public class HeadersPropagationConfig {
    
    private boolean enabledPropagation = false;
	
	private Pattern downPattern = Pattern.compile("X[-_].*");
	private Pattern upPattern = Pattern.compile("X[-_].*");
	
	public void setDown(String down) {
		downPattern = Pattern.compile(down) ;
	}

	public void setUp(String up) {
		upPattern = Pattern.compile(up) ;
	}

}
