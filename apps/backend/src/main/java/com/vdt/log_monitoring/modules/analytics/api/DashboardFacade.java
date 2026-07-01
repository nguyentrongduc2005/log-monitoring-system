package com.vdt.log_monitoring.modules.analytics.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DashboardFacade {

	OverviewSnapshotDto getOverviewSnapshot(String window, List<UUID> visibleApplicationIds);

	record OverviewSnapshotDto(
		String window,
		String generatedAt,
		List<OverviewMetricDto> metrics,
		List<LogVolumePointDto> volume,
		List<CriticalAlertSummaryDto> criticalAlerts
	) {}

	record OverviewMetricDto(
		String id,
		String label,
		String value,
		String trend,
		String helper,
		String tone
	) {}

	record LogVolumePointDto(
		String time,
		long INFO,
		long WARN,
		long ERROR,
		long CRITICAL
	) {}

	record CriticalAlertSummaryDto(
		UUID id,
		String severity,
		String application,
		List<LogSampleDto> logSamples,
		long occurrences,
		String lastSeen,
		String deliveryState
	) {}

	record LogSampleDto(
		String level,
		String message
	) {}
}
