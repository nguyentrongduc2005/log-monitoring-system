package com.vdt.log_monitoring.modules.anomaly.internal.log;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.anomaly.internal.rule.AnomalyLogRule;
import com.vdt.log_monitoring.modules.processing.api.events.AnomalySignalEvent;

@Component
public class AnomalyLogClassifier {

	private static final Pattern USER_PATTERN = Pattern.compile(
		"(?:user|username|account|accountId)=([A-Za-z0-9@._-]+)",
		Pattern.CASE_INSENSITIVE);
	private static final Pattern IP_PATTERN = Pattern.compile(
		"(?:ip|sourceIp|clientIp|from)=([0-9a-fA-F:.]+)",
		Pattern.CASE_INSENSITIVE);
	private static final Pattern HOST_PATTERN = Pattern.compile(
		"host=([A-Za-z0-9._-]+)",
		Pattern.CASE_INSENSITIVE);

	public Optional<AnomalyLogMatch> classify(AnomalySignalEvent event) {
		String normalizedMessage = event.message() == null
			? ""
			: event.message().toLowerCase(Locale.ROOT);

		for (AnomalyLogRule rule : AnomalyLogRule.values()) {
			if (rule.matches(event.level(), normalizedMessage)) {
				return Optional.of(new AnomalyLogMatch(
					rule,
					dimensionType(rule, event),
					dimensionValue(rule, event)));
			}
		}
		return Optional.empty();
	}

	private String dimensionType(AnomalyLogRule rule, AnomalySignalEvent event) {
		if (rule.dimensionPriority().contains("fingerprint")) {
			return "fingerprint";
		}
		for (String candidate : rule.dimensionPriority()) {
			if (extractAttribute(candidate, event.attributes()).isPresent() || hasDimension(candidate, event.message())) {
				return normalizeDimensionType(candidate);
			}
		}
		return "service";
	}

	private String dimensionValue(AnomalyLogRule rule, AnomalySignalEvent event) {
		String message = event.message();
		for (String candidate : rule.dimensionPriority()) {
			Optional<String> attribute = extractAttribute(candidate, event.attributes());
			if (attribute.isPresent()) {
				return attribute.get();
			}
			if ("fingerprint".equals(normalizeDimensionType(candidate)) && hasText(event.fingerprint())) {
				return event.fingerprint().trim();
			}
			Optional<String> extracted = extract(candidate, message);
			if (extracted.isPresent()) {
				return extracted.get();
			}
		}
		return event.serviceName();
	}

	private Optional<String> extractAttribute(String dimensionType, Map<String, String> attributes) {
		if (attributes == null || attributes.isEmpty()) {
			return Optional.empty();
		}
		for (String key : attributeKeys(dimensionType)) {
			String value = attributes.get(key);
			if (hasText(value)) {
				return Optional.of(value.trim());
			}
		}
		return Optional.empty();
	}

	private java.util.List<String> attributeKeys(String dimensionType) {
		return switch (normalizeDimensionType(dimensionType)) {
			case "user" -> java.util.List.of("user", "username", "account", "accountId", "userId");
			case "ip" -> java.util.List.of("ip", "sourceIp", "clientIp", "remoteIp");
			case "host" -> java.util.List.of("host", "hostname");
			case "endpoint" -> java.util.List.of("endpoint", "path", "uri", "route");
			case "fingerprint" -> java.util.List.of("fingerprint");
			default -> java.util.List.of(dimensionType);
		};
	}

	private boolean hasDimension(String dimensionType, String message) {
		return extract(dimensionType, message).isPresent();
	}

	private Optional<String> extract(String dimensionType, String message) {
		if (message == null || message.isBlank()) {
			return Optional.empty();
		}
		Pattern pattern = switch (normalizeDimensionType(dimensionType)) {
			case "user", "account" -> USER_PATTERN;
			case "ip" -> IP_PATTERN;
			case "host" -> HOST_PATTERN;
			default -> null;
		};
		if (pattern == null) {
			return Optional.empty();
		}
		var matcher = pattern.matcher(message);
		return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
	}

	private String normalizeDimensionType(String dimensionType) {
		if ("account".equalsIgnoreCase(dimensionType)) {
			return "user";
		}
		return dimensionType.toLowerCase(Locale.ROOT);
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
