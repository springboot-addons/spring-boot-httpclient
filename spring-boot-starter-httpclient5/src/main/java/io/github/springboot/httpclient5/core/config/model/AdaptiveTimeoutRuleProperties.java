package io.github.springboot.httpclient5.core.config.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-rule override of the adaptive response timeout policy.
 * <p>
 * Only the activation flag is configurable per rule; the policy parameters are global. A
 * {@code null} value inherits the global {@code enabled} flag.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class AdaptiveTimeoutRuleProperties {

	private Boolean enabled;
}
