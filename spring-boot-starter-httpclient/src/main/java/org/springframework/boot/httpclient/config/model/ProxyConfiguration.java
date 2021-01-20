package org.springframework.boot.httpclient.config.model;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
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
public class ProxyConfiguration implements Serializable {
  private static final long serialVersionUID = -1213026849353104356L;
  private String host = StringUtils.EMPTY;
  private Integer port = 0;
  private Boolean useProxy = false;
  private Authentication authentification = new Authentication();
  
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE) ;
	}
}
