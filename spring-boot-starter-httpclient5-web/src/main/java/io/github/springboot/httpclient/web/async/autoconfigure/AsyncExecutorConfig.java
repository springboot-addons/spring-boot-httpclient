package io.github.springboot.httpclient.web.async.autoconfigure;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.task.ThreadPoolTaskExecutorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import io.github.springboot.httpclient5.core.utils.ThreadFactoryUtils;

@Configuration
@EnableAsync
@ConditionalOnWebApplication
public class AsyncExecutorConfig {

	@Autowired
	private ThreadFactoryUtils threadFactoryUtils;
	
	@Bean
	public ThreadPoolTaskExecutorCustomizer hc5ThreadPoolTaskExecutorCustomizer() {
		return tpte -> { 
			tpte.setTaskDecorator(new ContextCopyingTaskDecorator());
			tpte.setThreadFactory(threadFactoryUtils.getThreadFactory());
		} ;
	}
	
	@Bean
	public TomcatConnectorCustomizer disableFacadeDiscard() {
	    return (connector) -> connector.setDiscardFacades(false);
	}
	
	// https://yashsrivastav.hashnode.dev/spring-boot-async-services
	public static class ContextCopyingTaskDecorator implements TaskDecorator {
	    @Override
	    public Runnable decorate(Runnable runnable) {
	        // Capture the current request attributes
	        RequestAttributes context = RequestContextHolder.currentRequestAttributes();
	        return () -> {
	            try {
	                // Set the request attributes for this thread
	                RequestContextHolder.setRequestAttributes(context);
	                runnable.run();
	            } finally {
	                // Reset the request attributes after execution
	                RequestContextHolder.resetRequestAttributes();
	            }
	        };
	    }
	}

}
