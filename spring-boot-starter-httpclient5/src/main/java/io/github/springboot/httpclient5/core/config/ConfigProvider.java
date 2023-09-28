package io.github.springboot.httpclient5.core.config;

public interface ConfigProvider<T> {
	public T get() ;
}
