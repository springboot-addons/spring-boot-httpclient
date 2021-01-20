package org.springframework.boot.httpclient.config.model;

import java.util.regex.Pattern;

import lombok.Data;

@Data
public class HeadersPropagation {
	
	private Pattern downPattern = Pattern.compile("X[-_].*");
	private Pattern upPattern = Pattern.compile("X[-_].*");
	
	public void setDown(String down) {
		downPattern = Pattern.compile(down) ;
	}

	public void setUp(String up) {
		upPattern = Pattern.compile(up) ;
	}

}
