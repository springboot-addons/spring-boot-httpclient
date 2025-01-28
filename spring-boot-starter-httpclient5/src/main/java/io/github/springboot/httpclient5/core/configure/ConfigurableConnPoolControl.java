package io.github.springboot.httpclient5.core.configure;

import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.core5.function.Resolver;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.pool.ConnPoolControl;

public interface ConfigurableConnPoolControl extends ConnPoolControl<HttpRoute> {
    public void setDefaultConnectionConfig(final ConnectionConfig config) ;
    default public void setDefaultSocketConfig(final SocketConfig config) {}
    public void setConnectionConfigResolver(final Resolver<HttpRoute, ConnectionConfig> connectionConfigResolver) ;
//    public void setSocketConfigResolver(final Resolver<HttpRoute, SocketConfig> socketConfigResolver) ;
}
