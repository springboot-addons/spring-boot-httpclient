package io.github.springboot.httpclient;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class Application {

	public static void main(String[] args) throws Exception {
		new SpringApplicationBuilder(Application.class).properties("spring.config.name:application").build().run(args);
	}
}
