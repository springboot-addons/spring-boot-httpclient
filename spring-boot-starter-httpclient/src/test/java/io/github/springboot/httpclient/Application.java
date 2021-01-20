package io.github.springboot.httpclient;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

import com.codahale.metrics.MetricRegistry;

@SpringBootApplication
public class Application {

	@Bean
	public MetricRegistry registry() {
		return new MetricRegistry();
	}

	public static void main(String[] args) throws Exception {
		new SpringApplicationBuilder(Application.class).properties("spring.config.name:application").build().run(args);
	}
}
