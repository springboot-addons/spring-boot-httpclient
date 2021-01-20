package io.github.springboot.httpclient.config.model;

import java.io.Serializable;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@EqualsAndHashCode
public class MonitoringConfiguration implements Serializable {
  private static final long serialVersionUID = -3620972131902953639L;
  private Boolean disableJmxMonitoring = false;
  private Boolean disableRequestTracing = false;
  private Boolean logPostMethods = false;
  
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE) ;
	}
}
