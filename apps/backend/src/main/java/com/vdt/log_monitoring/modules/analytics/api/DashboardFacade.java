package com.vdt.log_monitoring.modules.analytics.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DashboardFacade {

	OverviewSnapshotDto getOverviewSnapshot(String window, List<UUID> visibleApplicationIds);

	LogSearchResponseDto searchLogs(LogSearchRequestDto request, List<UUID> visibleApplicationIds);

	record LogSearchRequestDto(
		String query,
		UUID applicationId,
		String level,
		String range,
		int page,
		int pageSize,
		String selectedLogId
	) {}

	record LogSearchResponseDto(
		List<ApplicationDto> applications,
		LogSearchSummaryDto summary,
		List<LogVolumePointDto> buckets,
		List<LogSearchEntryDto> results,
		List<LogSearchEntryDto> relatedTrace,
		int totalPages,
		int currentPage,
		int pageSize,
		long totalResults
	) {}

	record ApplicationDto(
		UUID id,
		String name
	) {}

	record LogSearchSummaryDto(
		long totalMatches,
		long errorMatches,
		long criticalMatches,
		long uniqueTraces,
		long slowestDurationMs
	) {}

	record LogSearchEntryDto(
		String id,
		String timestamp,
		UUID applicationId,
		String applicationName,
		String level,
		String message,
		String traceId,
		String spanId,
		String eventId,
		String source,
		String host,
		Long durationMs,
		Integer statusCode,
		java.util.Map<String, String> attributes,
		List<String> stack
	) {}

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
