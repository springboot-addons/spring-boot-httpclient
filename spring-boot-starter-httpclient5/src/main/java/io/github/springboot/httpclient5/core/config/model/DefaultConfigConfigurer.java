package io.github.springboot.httpclient5.core.config.model;

import org.apache.hc.core5.pool.PoolConcurrencyPolicy;

public interface DefaultConfigConfigurer {
    public void setDefaultConnectionConfig(final ConnectionConfigProperties config) ;
    public ConnectionConfigProperties getDefaultConnectionConfig() ;
    
    public <T> T setPoolConcurrencyPolicy(final PoolConcurrencyPolicy poolConcurrencyPolicy) ;
    public <T> T setMaxConnPerRoute(final int maxConnPerRoute) ;
    
    default public SocketConfigProperties getDefaultSocketConfig() {return null ;}
    default public void setDefaultSocketConfig(final SocketConfigProperties config) {}
}
