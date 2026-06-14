package com.vdt.log_monitoring.modules.identity.internal.apikey;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.security.api-key")
public class ApiKeyProperties {

	@NotBlank
	private String keyPrefix;

	@NotBlank
	private String prefixCharacters;

	@Positive
	private int randomPrefixLength;

	@Positive
	private int secretLengthBytes;

	@Valid
	@NotNull
	private VerificationCache verificationCache = new VerificationCache();

	@Getter
	@Setter
	public static class VerificationCache {

		@NotBlank
		private String prefix;

		@NotBlank
		private String indexPrefix;

		@Positive
		private long ttlSeconds;
	}
}
