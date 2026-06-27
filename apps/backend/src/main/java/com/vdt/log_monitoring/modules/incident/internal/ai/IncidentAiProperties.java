package com.vdt.log_monitoring.modules.incident.internal.ai;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.incident.ai")
public record IncidentAiProperties(
	String provider,
	String apiBaseUrl,
	String apiKey,
	String model,
	Duration timeout
) {

	public IncidentAiProperties {
		provider = blankToDefault(provider, "gemini");
		apiBaseUrl = blankToDefault(apiBaseUrl, "https://generativelanguage.googleapis.com");
		model = blankToDefault(model, "gemini-3.1-flash-lite");
		timeout = timeout == null ? Duration.ofSeconds(30) : timeout;
	}

	private static String blankToDefault(String value, String defaultValue) {
		return value == null || value.isBlank() ? defaultValue : value.trim();
	}
}
