# spring-boot-httpclient
Spring Boot AutoConfiguration starters for Apache HttpClient 5.x

* Requires SpringBoot 3.0.x or higher and Java 17 +
* Tested with SpringBoot 3.1.x and SpringBoot 3.2.x
* Spring Boot 3.3.x / HttpClient5 5.3.x + need spring-boot-httpclient5 1.0.6+ 
* Spring Boot 3.4.x / HttpClient5 5.4.x + need spring-boot-httpclient5 1.2.0+ 
* Spring Boot 4.0.x / HttpClient5 5.5.x + need spring-boot-httpclient5 2.0.0+ 

***Documentation In progress***

Configuration support for HttpClient through SpringBoot yaml / properties
- Support configuration at url / method level for timeouts
- Http connnection pool size configurable at host level
- Support for actuator / dropmetrics
- Support for resilience4j circuit breaker / ratelimiter
- Support for headers propagation (bidirectional)
- Support for Async client : CloseableHttpAsyncClient
- many more

Dependencies : https://mvnrepository.com/artifact/io.github.springboot-addons

Httpclient configuration support 

	<dependency>
		<groupId>io.github.springboot-addons</groupId>
		<artifactId>spring-boot-starter-httpclient5</artifactId>
		<version>2.0.0</version>
	</dependency>

Httpclient actuator support 

	<dependency>
		<groupId>io.github.springboot-addons</groupId>
		<artifactId>spring-boot-starter-httpclient5-actuator</artifactId>
		<version>2.0.0</version>
	</dependency>


Httpclient resilience4j support 

	<dependency>
		<groupId>io.github.springboot-addons</groupId>
		<artifactId>spring-boot-starter-httpclient5-resilience4j</artifactId>
		<version>2.0.0</version>
	</dependency>



Httpclient all in one support 

	<dependency>
		<groupId>io.github.springboot-addons</groupId>
		<artifactId>spring-boot-starter-httpclient5-all</artifactId>
		<version>2.0.0</version>
		<type>pom</type>
	</dependency>

Minimum recommended configuration : (or no configuration => it will use this defaults)

	spring:
	  httpclient5:
	    pool:
	      max-connper-route: 50
	      pool-concurrency-policy: LAX
	      default-connection-config:
	        connect-timeout: PT1S
	    async-pool:
	      max-connper-route: 50
	      pool-concurrency-policy: LAX
	      default-connection-config:
	        connect-timeout: PT1S
	    request-config:
	      '[default]':
	        connection-request-timeout: PT3S
	        response-timeout: PT30S

Full sample configuration : 

	spring:
	  httpclient5:
	    user-agent: HttpClient5
	    jmx.domain: ${spring.application.name}
	    # see properties from org.apache.hc.core5.http.config.Http1Config
	    http1:
	      buffer-size: 4096
	      waitForContinueTimeout: PT32S
	    # see properties org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
	    async-pool:
	      max-connper-route: 20
	      dns-resolver: system
	      conn-pool-policy: LIFO
	      connection-time-to-live: PT120S
	      pool-concurrency-policy: LAX
	      # see properties from org.apache.hc.core5.http.io.SocketConfig
	      default-connection-config:
	        connect-timeout: PT0.1S
	        socket-timeout: PT15S
	      default-socket-config:
	        so-timeout: PT250S
	      host-config:
	        '[${app.httpbin-host}]':
	          connect-timeout: PT0.5S
	          max-connections: 15 
	        '[https://testhost:4443]':
	          connect-timeout: PT0.3S
	          max-connections: 40
	    pool:
	      max-conn-total: 128
	      max-connper-route: 5
	      dns-resolver: system
	      conn-pool-policy: LIFO
	      connection-time-to-live: PT120S
	      pool-concurrency-policy: STRICT
	      # see properties from org.apache.hc.core5.http.io.SocketConfig
	      default-socket-config:
	        so-timeout: PT60S
	      # see properties from org.apache.hc.client5.http.config.ConnectionConfig
	      default-connection-config:
	        connect-timeout: PT1S
	        socket-timeout: PT30S
	      host-config:
	        '[${app.httpbin-host}]':
		       # see properties from org.apache.hc.client5.http.config.ConnectionConfig + max-connections
	          connect-timeout: PT1S
	          max-connections: 10 
	        '[https://testhost:4443]':
	          connect-timeout: PT2S
	          max-connections: 20 
	    # See properties from org.apache.hc.client5.http.config.RequestConfig + addons headers-propagation, error-management
	    # WARNING connection-timeout deprecated and no more working on 5.2.x+, use host-config or default-connection-config
	    request-config:
	      '[default]':
	        connection-keep-alive: PT30S
	        connection-request-timeout: 1000
	        response-timeout: 2000
	        headers-propagation:
	         enabled: true
	         up: 
	           - X-TEST-.*
	         down: 
	           - X-TEST-.*
	         add:
	           '[X-TU]': "SRU ADDED HEADER"
	        error-management:
	          circuit-name: default
	          broken-circuit-action: 503
	        retry-config: 
	          max-retries: 2
	          retry-interval: PT3S
	        interceptors:
	          '[myinterceptor]': false
	        custom-request-context:           
	          '[propA]': "abc"
	      '[GET https://httpbin.org/.*]':
	        response-timeout: 3000
	        error-management.circuit-name: httpbin-org
	      '[GET https://httpbin.org/basic-auth/.*]':
	        credentials: admin:pwd
	        #credentials: BASIC(admin:pwd)
	      '[GET https://httpbin.org/hidden-basic-auth/.*]':
	        credentials: PREEMPTIVE(admin:pwd)	        
	      '[POST https://httpbin.org/.*]':
	        response-timeout: 5000
	        error-management.circuit-name: httpbin-org
	      '[.* https://testhost:4443/.*]':
	        proxy: https://localhost:3128
	      '[.* https://testhost/.*]':
	        response-timeout: 1000
	      '[POST https://testhost/subpath/.*]':
	        response-timeout: 3000

	resilience4j:
	  circuitbreaker:
	    instances:
	      default:
	        minimumNumberOfCalls: 2
	        eventConsumerBufferSize: 10
	        failureRateThreshold: 100
	        registerHealthIndicator: true
	        wait-duration-in-open-state: 1000ms
	      httpbin-org:
	        minimumNumberOfCalls: 3
	        slidingWindowSize: 5
	        eventConsumerBufferSize: 10
	        failureRateThreshold: 100
	        registerHealthIndicator: true
	        wait-duration-in-open-state: 1000ms
	  ratelimiter:
	    instances:
	      httpbin-org:
	        limitForPeriod: 1
	        timeoutDuration: 1s
	        limitRefreshPeriod: 1000ms
	        registerHealthIndicator: true