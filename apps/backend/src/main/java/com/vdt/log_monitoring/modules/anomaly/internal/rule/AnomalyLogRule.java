package com.vdt.log_monitoring.modules.anomaly.internal.rule;

import java.time.Duration;
import java.util.List;

public enum AnomalyLogRule {
	RESOURCE_EXHAUSTED(
		Duration.ofMinutes(5),
		List.of("out of memory", "oom", "heap space", "disk full", "no space left"),
		List.of("host", "service")),
	SECURITY_AUTH_FAILURE(
		Duration.ofMinutes(5),
		List.of("failed login", "login failed", "authentication failed", "bad credentials",
			"invalid password", "invalid token", "ssh failed", "failed password"),
		List.of("user", "account", "ip", "service")),
	ACCESS_DENIED(
		Duration.ofMinutes(5),
		List.of("denied", "forbidden", "unauthorized", "permission denied"),
		List.of("user", "account", "ip", "service")),
	TIMEOUT(
		Duration.ofMinutes(2),
		List.of("timeout", "timed out", "deadline exceeded", "read timeout", "connection timeout"),
		List.of("service", "host")),
	EXCEPTION(
		Duration.ofMinutes(2),
		List.of("exception", "stacktrace", "nullpointer", "illegalstate", "runtimeexception"),
		List.of("service")),
	CRITICAL_LEVEL(
		Duration.ofMinutes(5),
		List.of(),
		List.of("service")),
	ERROR_LEVEL(
		Duration.ofMinutes(1),
		List.of(),
		List.of("service")),
	SUSPICIOUS_KEYWORD(
		Duration.ofMinutes(5),
		List.of("failed", "failure", "degraded", "unavailable", "retry exhausted", "circuit breaker open"),
		List.of("service"));

	private static final Duration GRACE = Duration.ofSeconds(60);

	private final Duration window;
	private final List<String> keywords;
	private final List<String> dimensionPriority;

	AnomalyLogRule(
		Duration window,
		List<String> keywords,
		List<String> dimensionPriority) {
		this.window = window;
		this.keywords = keywords;
		this.dimensionPriority = dimensionPriority;
	}

	public Duration window() {
		return window;
	}

	public Duration ttl() {
		return window.plus(GRACE);
	}

	public List<String> dimensionPriority() {
		return dimensionPriority;
	}

	public boolean matches(String level, String normalizedMessage) {
		return switch (this) {
			case CRITICAL_LEVEL -> "CRITICAL".equalsIgnoreCase(level);
			case ERROR_LEVEL -> "ERROR".equalsIgnoreCase(level);
			default -> keywords.stream().anyMatch(normalizedMessage::contains);
		};
	}

	public long thresholdCount() {
		return switch (this) {
			case RESOURCE_EXHAUSTED, CRITICAL_LEVEL -> 1;
			case SECURITY_AUTH_FAILURE, ACCESS_DENIED -> 5;
			case TIMEOUT, EXCEPTION -> 10;
			case ERROR_LEVEL -> 15;
			case SUSPICIOUS_KEYWORD -> 20;
		};
	}

	public boolean isBreached(long count) {
		return count >= thresholdCount();
	}

	public Duration cooldown() {
		return switch (this) {
			case SECURITY_AUTH_FAILURE, ACCESS_DENIED -> Duration.ofMinutes(10);
			case RESOURCE_EXHAUSTED, CRITICAL_LEVEL -> Duration.ofMinutes(5);
			default -> Duration.ofMinutes(3);
		};
	}

	public String severityFor(long count) {
		return switch (this) {
			case RESOURCE_EXHAUSTED, CRITICAL_LEVEL -> "CRITICAL";
			case SECURITY_AUTH_FAILURE, ACCESS_DENIED ->
				count >= thresholdCount() * 3 ? "CRITICAL" : "ERROR";
			case TIMEOUT, EXCEPTION, ERROR_LEVEL ->
				count >= thresholdCount() * 2 ? "CRITICAL" : "ERROR";
			case SUSPICIOUS_KEYWORD -> "WARN";
		};
	}

	public boolean shouldTriggerAi(long count, long repeatedDedupCount) {
		return "CRITICAL".equals(severityFor(count))
			|| count >= thresholdCount() * 3
			|| repeatedDedupCount >= 2
			|| this == SECURITY_AUTH_FAILURE
			|| this == RESOURCE_EXHAUSTED;
	}

	public double confidenceScore(long count) {
		double ratio = thresholdCount() <= 0 ? 1.0 : (double) count / thresholdCount();
		return Math.min(0.98, 0.45 + (ratio * 0.15));
	}

	public String likelihoodLabel(long count) {
		double confidence = confidenceScore(count);
		if (confidence >= 0.85) {
			return "VERY_HIGH";
		}
		if (confidence >= 0.70) {
			return "HIGH";
		}
		if (confidence >= 0.55) {
			return "MEDIUM";
		}
		return "LOW";
	}

	public String hypothesis(String dimensionType, String dimensionValue) {
		return switch (this) {
			case SECURITY_AUTH_FAILURE -> "Possible brute-force, credential stuffing, or authentication abuse for "
				+ dimensionType + " " + dimensionValue + ".";
			case ACCESS_DENIED -> "Possible permission or access-control issue affecting "
				+ dimensionType + " " + dimensionValue + ".";
			case RESOURCE_EXHAUSTED -> "Possible resource exhaustion affecting "
				+ dimensionType + " " + dimensionValue + ".";
			case TIMEOUT -> "Possible latency or downstream dependency timeout affecting "
				+ dimensionType + " " + dimensionValue + ".";
			case EXCEPTION -> "Repeated exception pattern detected for "
				+ dimensionType + " " + dimensionValue + ".";
			case CRITICAL_LEVEL -> "Critical logs indicate an active severe condition for "
				+ dimensionType + " " + dimensionValue + ".";
			case ERROR_LEVEL -> "Error volume exceeded normal threshold for "
				+ dimensionType + " " + dimensionValue + ".";
			case SUSPICIOUS_KEYWORD -> "Suspicious degradation keywords repeated for "
				+ dimensionType + " " + dimensionValue + ".";
		};
	}

	public List<String> recommendedActions() {
		return switch (this) {
			case SECURITY_AUTH_FAILURE -> List.of(
				"Check failed and successful login attempts for the same account or IP.",
				"Review source IP reputation and recent account activity.",
				"Apply MFA challenge, rate limiting, or temporary lockout if attempts continue.");
			case RESOURCE_EXHAUSTED -> List.of(
				"Check CPU, memory, disk, and container restart metrics.",
				"Inspect recent deployments and traffic spikes.",
				"Scale or restart the affected service if saturation continues.");
			case ACCESS_DENIED -> List.of(
				"Check recent permission, role, or token changes.",
				"Confirm whether denied access is expected for this user or service.",
				"Inspect related audit logs in the same time window.");
			case TIMEOUT -> List.of(
				"Inspect downstream dependency health and latency.",
				"Check request traces for the affected service.",
				"Review retry, circuit breaker, and timeout configuration.");
			default -> List.of(
				"Inspect related logs in the anomaly window.",
				"Compare with deployment, traffic, and metric changes.",
				"Escalate if the pattern continues or user impact is visible.");
		};
	}
}
