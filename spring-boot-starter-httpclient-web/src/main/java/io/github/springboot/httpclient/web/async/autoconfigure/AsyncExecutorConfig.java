package io.github.springboot.httpclient.web.async.autoconfigure;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.task.TaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Pour la propagation des headers via un bean en scope request sur des methodes
 * déclarées @Async (annotation standard spring) ref
 * https://gitlab.com/snippets/175860
 * 
 * @author sru
 *
 */
@Configuration
@EnableAsync
@ConditionalOnWebApplication
public class AsyncExecutorConfig extends AsyncConfigurerSupport {

	@Autowired
	private TaskExecutorBuilder builder;

	@Override
	@Bean
	@Primary
	public Executor getAsyncExecutor() {
		return builder.build(ContextAwarePoolExecutor.class);
	}

	public static class ContextAwarePoolExecutor extends ThreadPoolTaskExecutor {
		private static final long serialVersionUID = -2774656603827672334L;

		@Override
		public void execute( Runnable task) {
			super.execute(new ContextAwareRunnable(task, RequestContextHolder.currentRequestAttributes()));
		}

		@Override
		public Future<?> submit(Runnable task) {
			return super.submit(new ContextAwareRunnable(task, RequestContextHolder.currentRequestAttributes()));
		}

		@Override
		public <T> Future<T> submit(Callable<T> task) {
			return super.submit(new ContextAwareCallable<>(task, RequestContextHolder.currentRequestAttributes()));
		}

		@Override
		public ListenableFuture<?> submitListenable(Runnable task) {
			return super.submitListenable(new ContextAwareRunnable(task, RequestContextHolder.currentRequestAttributes()));
		}


		@Override
		public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
			return super.submitListenable(
					new ContextAwareCallable<>(task, RequestContextHolder.currentRequestAttributes()));
		}
	}

	public static class ContextAwareCallable<T> implements Callable<T> {
		private final Callable<T> task;
		private final RequestAttributes context;

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

	public static class ContextAwareRunnable implements Runnable {

		private final Runnable task;
		private final RequestAttributes context;

		public ContextAwareRunnable(Runnable task, RequestAttributes context) {
			this.task = task;
			this.context = context;
		}

		@Override
		public void run() {
			if (context != null) {
				RequestContextHolder.setRequestAttributes(context);
			}
			try {
				task.run();
			} finally {
				RequestContextHolder.resetRequestAttributes();
			}
		}
	}
}
