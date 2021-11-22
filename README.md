# spring-boot-httpclient
Spring Boot AutoConfiguration starters for Apache HttpClient

*SpringBoot 2.4.x +
Java 1.8 +*

***Documentation In progress***

Configuration support for HttpClient through SpringBoot yaml / properties
- Support configuration at url / method level for timeouts
- Http connnection pool size configurable at host level
- Support for actuator / dropmetrics
- Support for resilience4j circuit breaker / ratelimiter
- Support for cas and ntlm auth
- Support for headers propagation (bidirectional)
- many more

Dependencies : https://mvnrepository.com/artifact/io.github.springboot-addons

Httpclient configuration support 

	<dependency>
		<groupId>io.github.springboot-addons</groupId>
		<artifactId>spring-boot-starter-httpclient</artifactId>
		<version>1.0.3</version>
	</dependency>

Httpclient actuator support 

	<dependency>
		<groupId>io.github.springboot-addons</groupId>
		<artifactId>spring-boot-starter-actuator</artifactId>
		<version>1.0.3</version>
	</dependency>


Httpclient resilience4j support 

	<dependency>
		<groupId>io.github.springboot-addons</groupId>
		<artifactId>spring-boot-starter-resilience4j</artifactId>
		<version>1.0.3</version>
	</dependency>

Httpclient cas support 

	<dependency>
		<groupId>io.github.springboot-addons</groupId>
		<artifactId>spring-boot-starter-security-cas</artifactId>
		<version>1.0.3</version>
	</dependency>

Httpclient all in one support 

	<dependency>
		<groupId>io.github.springboot-addons</groupId>
		<artifactId>spring-boot-starter-all</artifactId>
		<version>1.0.3</version>
	</dependency>

Sample configuration : 

    httpclient:
        broken-circuit-action: 503     # defaut resilience4j circuit breaker action (in case of resilience4j use)
        core:
	      cookie-store.type: thread-local  # cookie store : thread-local, request or shared
        connection:
          buffer-size: 4096
          compression: gzip,deflate
          connect-timeout: 2000
          cookie-policy: standard     	# global httpclient cookie policy
          max-active: 20
          socket-timeout: 10000
          trust-ssl: false    			# trust all certs by default
          user-agent: httpclient
          delay-before-retrying: 0.5   	# retry delay (seconds) in case of http 429 reponse status (TOO_MANY_REQUEST)
        headers:
	  enable-propagation: true
          down: X-TEST-.*
          up: X-TEST-.*
	  remove: TEST_H1, TEST-H2
	  add:
	    TEST_ADD1: Value 1
	    TEST_ADD2: Value 2
        hosts:
          google:
            base-url: https://www.google.fr
            broken-circuit-action: exception
            connection:
              compression: gzip,deflate
              max-active: 30
              socket-timeout: 5000
              trust-ssl: true
              trust-ssl-domains: www.google.fr,www.google.com
          httpbin-org:
            base-url: https://httpbin.org
            broken-circuit-action: 503
            connection:
              compression: gzip,deflate
              max-active: 10
              socket-timeout: 3000
              trust-ssl: true
            methods:
              POST:
                connection:
                  socket-timeout: 5000
            proxy:
              use-proxy: false
        jmx-domain: test-app         # domain for dropmetrics jmxexporter
        linger-timeout: -1
        monitoring:
          log-post-methods: false    # logs post method sent content 
        pool-idle-timeout: 300000
        pool-timeout: 30000
        proxy:
          authentification:
            auth-type: basic
            domain: ''
            password: xxx
            user: proxyuser
          host: ''
          port: ''
          use-proxy: false
        retry-attempts: 2         # resilience4j retry config 
        retry-wait-duration: 100  # resilience4j retry config
        web:
	      headers-propagation:
		    enabled: true
	      headers-filter:
		    enabled: true
	
	resilience4j:
	  circuitbreaker:
		instances:
		  default:
			eventConsumerBufferSize: 10
			failureRateThreshold: 100
			registerHealthIndicator: true
			ringBufferSizeInClosedState: 2
			ringBufferSizeInHalfOpenState: 3
			wait-duration-in-open-state: 3000
		  httpbin-org:
			eventConsumerBufferSize: 10
			failureRateThreshold: 100
			registerHealthIndicator: true
			ringBufferSizeInClosedState: 5
			ringBufferSizeInHalfOpenState: 3
			wait-duration-in-open-state: 5000
	  ratelimiter:
		instances:
		  httpbin-org:
			limitForPeriod: 10
			timeoutDuration: 5s
			limitRefreshPeriod: 10s
			registerHealthIndicator: true
