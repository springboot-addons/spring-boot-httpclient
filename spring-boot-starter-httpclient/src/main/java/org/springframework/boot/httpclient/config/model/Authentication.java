/**
 *
 */
package org.springframework.boot.httpclient.config.model;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.boot.httpclient.constants.HttpClientConstants;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@EqualsAndHashCode
public class Authentication implements Serializable {

	private static final long serialVersionUID = -1391642922590563052L;

	public static final String AUTH_TYPE_BASIC = "Basic";
	public static final String AUTH_TYPE_NTLM = "NTLM";
	public static final String AUTH_TYPE_CAS = "CAS";
	public static final String AUTH_TYPE_CERT = "CLIENT_CERTIFICATE";
	public static final String SYSTEM_DEFAULT = "SYSTEM";

	private String user;
	private String password;
	private String domain = "";
	private String authType = "";
	private String authEndpoint;
	private String authKeyStore = "SYSTEM";
	private String authKeyStorePassword = "";
	private String authKeyStoreType = HttpClientConstants.KEYSTORE_DEFAULT_TYPE;
	private String authKeyAlias = "";
	private Boolean preemptive = false;
	private String credentialsCharset = "ASCII";

	public boolean isRequired() {
		return StringUtils.isNotBlank(user) && StringUtils.isNotBlank(password) || StringUtils.isNotBlank(domain);
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
	}
}