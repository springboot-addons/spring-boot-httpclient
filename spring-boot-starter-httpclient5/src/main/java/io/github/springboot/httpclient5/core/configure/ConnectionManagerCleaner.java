package io.github.springboot.httpclient5.core.configure;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.springframework.stereotype.Component;

import io.github.springboot.httpclient5.core.config.HttpClient5Config;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConnectionManagerCleaner implements PoolingHttpClientConnectionManagerPostConfigurer {

	private final HttpClient5Config config ;
	
	private PoolingHttpClientConnectionManager cm; 
	private ScheduledFuture<?> cleanerTask;
	
	private ScheduledExecutorService executor ;

	@PostConstruct
	public void init() {
		executor = new ScheduledThreadPoolExecutor(1); 
	}

	@PreDestroy
	public void dispose() {
		cleanerTask.cancel(false) ;
		executor.shutdown();
	}
	
	@Override
	public void configure(PoolingHttpClientConnectionManager cm) {
		this.cm = cm;
		long delay = config.getPool().getConnectionIdleTimeout().convert(TimeUnit.MILLISECONDS) / 10 ;
		cleanerTask = executor.scheduleWithFixedDelay(this::clean, delay, delay, TimeUnit.MILLISECONDS) ;
	}

	protected void clean() {
        try {
            if (cm != null) {
                log.trace("run IdleConnectionMonitor - Closing expired and idle connections more than {}", config.getPool().getConnectionIdleTimeout());
                cm.closeExpired();
                cm.closeIdle(config.getPool().getConnectionIdleTimeout());
            } else {
                log.trace("run IdleConnectionMonitor - Http Client Connection manager is not initialised");
            }
        } catch (Exception e) {
            log.warn("run IdleConnectionMonitor - Exception occurred. msg={}, e={}", e.getMessage(), e);
        }
    }	
}

