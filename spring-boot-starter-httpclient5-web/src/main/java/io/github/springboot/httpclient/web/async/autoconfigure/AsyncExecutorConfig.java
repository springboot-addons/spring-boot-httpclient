package io.github.springboot.httpclient.web.async.autoconfigure;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.task.ThreadPoolTaskExecutorCustomizer;
import org.springframework.boot.tomcat.TomcatConnectorCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import io.github.springboot.httpclient5.core.utils.ThreadFactoryUtils;

@Configuration
@EnableAsync
@ConditionalOnProperty(name = "spring.httpclient5.web.async-decorator.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnWebApplication
public class AsyncExecutorConfig {

	@Autowired
	private ThreadFactoryUtils threadFactoryUtils;
	
	@Bean
	public ThreadPoolTaskExecutorCustomizer hc5ThreadPoolTaskExecutorCustomizer() {
		return tpte -> tpte.setThreadFactory(threadFactoryUtils.getThreadFactory()) ;
	}

	/**
	 * Propagates the request context to asynchronous tasks.
	 * <p>
	 * Registered as a {@link TaskDecorator} bean so that it also applies to the
	 * {@code SimpleAsyncTaskExecutor} used when virtual threads are enabled
	 * ({@code spring.threads.virtual.enabled=true}); the thread-pool customizer would be ignored
	 * in that case.
	 */
	@Bean
	@ConditionalOnMissingBean(TaskDecorator.class)
	public TaskDecorator hc5ContextCopyingTaskDecorator() {
		return new ContextCopyingTaskDecorator();
	}
	
	@Bean
	public TomcatConnectorCustomizer disableFacadeDiscard() {
	    return (connector) -> connector.setDiscardFacades(false);
	}
	
	// https://yashsrivastav.hashnode.dev/spring-boot-async-services
	public static class ContextCopyingTaskDecorator implements TaskDecorator {
	    @Override
	    public Runnable decorate(Runnable runnable) {
	        // Capture the current request attributes (may be null when no request is active)
	        RequestAttributes context = RequestContextHolder.getRequestAttributes();
	        if (context == null) {
	            return runnable;
	        }
	        return () -> {
	            RequestAttributes previous = RequestContextHolder.getRequestAttributes();
	            try {
	                // Set the request attributes for this thread
	                RequestContextHolder.setRequestAttributes(context, true);
	                runnable.run();
	            } finally {
	                // Restore the previous attributes (or clear them)
	                if (previous == null) {
	                    RequestContextHolder.resetRequestAttributes();
	                }
	                else {
	                    RequestContextHolder.setRequestAttributes(previous, true);
	                }
	            }
	        };
	    }
	}

}
