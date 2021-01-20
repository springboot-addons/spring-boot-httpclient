package io.github.springboot.httpclient.config;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Pour la propagation des headers via un bean en scope request sur des methodes déclarées @Async (annotation standard spring)
 * ref https://gitlab.com/snippets/175860
 * 
 * @author sru
 *
 */
@Configuration
@EnableAsync
@ConditionalOnMissingBean(name = "ContextAwareAsyncExecutorConfig")
public class AsyncExecutorConfig extends AsyncConfigurerSupport {
	
	@Override
	@Bean
	public Executor getAsyncExecutor() {
		return new ContextAwarePoolExecutor();
	}

	public static class ContextAwarePoolExecutor extends ThreadPoolTaskExecutor {
		private static final long serialVersionUID = -2774656603827672334L;

		@Override
		public <T> Future<T> submit(Callable<T> task) {
			return super.submit(new ContextAwareCallable<T>(task, RequestContextHolder.currentRequestAttributes()));
		}

		@Override
		public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
			return super.submitListenable(
					new ContextAwareCallable<T>(task, RequestContextHolder.currentRequestAttributes()));
		}
	}

	public static class ContextAwareCallable<T> implements Callable<T> {
		private Callable<T> task;
		private RequestAttributes context;

		public ContextAwareCallable(Callable<T> task, RequestAttributes context) {
			this.task = task;
			this.context = context;
		}

		@Override
		public T call() throws Exception {
			if (context != null) {
				RequestContextHolder.setRequestAttributes(context);
			}

			try {
				return task.call();
			} finally {
				RequestContextHolder.resetRequestAttributes();
			}
		}
	}

}
