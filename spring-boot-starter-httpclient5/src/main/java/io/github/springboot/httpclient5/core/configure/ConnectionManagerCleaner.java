package io.github.springboot.httpclient5.core.configure;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.springframework.beans.factory.annotation.Value;
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
	private static final int DEFAULT_CLOSE_IDLE_CONNECTION_WAIT_TIME_SECS = 30;

	@Value("${spring.httpclient5.pool.connection-time-to-live:PT"+DEFAULT_CLOSE_IDLE_CONNECTION_WAIT_TIME_SECS+"S}")
	private TimeValue idleConnectionTimeout ;

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
		long delay = idleConnectionTimeout.getDuration() / 10 ;
		cleanerTask = executor.scheduleWithFixedDelay(this::clean, delay, delay, TimeUnit.MILLISECONDS) ;
	}

	protected void clean() {
        try {
            if (cm != null) {
                log.debug("run IdleConnectionMonitor - Closing expired and idle connections more than {}", idleConnectionTimeout);
                cm.closeExpired();
                cm.closeIdle(idleConnectionTimeout);
            } else {
                log.trace("run IdleConnectionMonitor - Http Client Connection manager is not initialised");
            }
        } catch (Exception e) {
            log.warn("run IdleConnectionMonitor - Exception occurred. msg={}, e={}", e.getMessage(), e);
        }
    }	
}

