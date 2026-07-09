package com.vdt.log_monitoring.modules.anomaly.internal.rule;

import java.util.List;

public enum AnomalyMetricRule {
	CPU_USAGE,
	MEMORY_USAGE,
	DISK_USAGE,
	DISK_WRITE_RATE,
	NETWORK_RX_RATE,
	NETWORK_TX_RATE;

	public String unit() {
		return switch (this) {
			case CPU_USAGE, MEMORY_USAGE, DISK_USAGE -> "%";
			case DISK_WRITE_RATE, NETWORK_RX_RATE, NETWORK_TX_RATE -> "bytes/sec";
		};
	}

	public double warningThreshold() {
		return switch (this) {
			case CPU_USAGE -> 85.0;
			case MEMORY_USAGE, DISK_USAGE -> 90.0;
			case DISK_WRITE_RATE -> 50_000_000.0;
			case NETWORK_RX_RATE, NETWORK_TX_RATE -> 100_000_000.0;
		};
	}

	public double criticalThreshold() {
		return switch (this) {
			case CPU_USAGE -> 90.0;
			case MEMORY_USAGE, DISK_USAGE -> 95.0;
			case DISK_WRITE_RATE -> 100_000_000.0;
			case NETWORK_RX_RATE, NETWORK_TX_RATE -> 250_000_000.0;
		};
	}

	public boolean isBreached(double current) {
		return current >= warningThreshold();
	}

	public boolean isBreached(List<Double> recentValues) {
		return isCriticalBreached(recentValues) || isWarningBreached(recentValues);
	}

	public boolean isWarningBreached(List<Double> recentValues) {
		return recentValues != null
			&& recentValues.size() >= 6
			&& recentValues.stream()
				.filter(value -> value >= warningThreshold())
				.count() >= 4;
	}

	public boolean isCriticalBreached(List<Double> recentValues) {
		if (recentValues == null || recentValues.size() < 3) {
			return false;
		}
		List<Double> lastThree = recentValues.subList(recentValues.size() - 3, recentValues.size());
		return lastThree.stream()
			.filter(value -> value >= criticalThreshold())
			.count() >= 2;
	}

	public double thresholdFor(double current) {
		return current >= criticalThreshold() ? criticalThreshold() : warningThreshold();
	}

	public double thresholdFor(List<Double> recentValues) {
		return isCriticalBreached(recentValues) ? criticalThreshold() : warningThreshold();
	}

	public String severityFor(double current) {
		if (current >= criticalThreshold()) {
			return "CRITICAL";
		}
		if (current >= warningThreshold()) {
			return switch (this) {
				case CPU_USAGE, MEMORY_USAGE, DISK_USAGE -> "ERROR";
				default -> "WARN";
			};
		}
		return "INFO";
	}

	public String severityFor(List<Double> recentValues) {
		if (isCriticalBreached(recentValues)) {
			return "CRITICAL";
		}
		if (isWarningBreached(recentValues)) {
			return switch (this) {
				case CPU_USAGE, MEMORY_USAGE, DISK_USAGE -> "ERROR";
				default -> "WARN";
			};
		}
		return "INFO";
	}

	public double confidenceScore(double current) {
		double ratio = warningThreshold() <= 0 ? 1.0 : current / warningThreshold();
		return Math.min(0.98, 0.40 + (ratio * 0.18));
	}
}
