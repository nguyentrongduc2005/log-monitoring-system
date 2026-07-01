package com.vdt.log_monitoring.modules.analytics.internal;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.vdt.log_monitoring.modules.analytics.api.DashboardFacade.CriticalAlertSummaryDto;
import com.vdt.log_monitoring.modules.analytics.api.DashboardFacade.LogSampleDto;
import com.vdt.log_monitoring.modules.analytics.api.DashboardFacade.LogVolumePointDto;
import com.vdt.log_monitoring.modules.analytics.api.DashboardFacade.OverviewMetricDto;
import com.vdt.log_monitoring.modules.analytics.api.DashboardFacade.OverviewSnapshotDto;
import com.vdt.log_monitoring.modules.anomaly.api.AnomalyFacade;
import com.vdt.log_monitoring.modules.anomaly.api.AnomalyFacade.AnomalyReportDto;

import com.vdt.log_monitoring.modules.ingestion.api.LogIngestionFacade;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

	private final ClickHouseAnalyticsRepository clickHouseRepository;
	private final AnomalyFacade anomalyFacade;
	private final LogIngestionFacade logIngestionFacade;

	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneOffset.UTC);

	public OverviewSnapshotDto getDashboardOverview(String window, java.util.List<java.util.UUID> visibleApplicationIds) {
		Instant end = Instant.now();
		// For now, hardcode to 24h as requested
		Instant start = end.minus(24, ChronoUnit.HOURS);

		if (visibleApplicationIds == null || visibleApplicationIds.isEmpty()) {
			return createEmptySnapshot();
		}
		
		long totalLogs = clickHouseRepository.getTotalLogsCount(start, end, visibleApplicationIds);
		long errorLogs = clickHouseRepository.getErrorLogsCount(start, end, visibleApplicationIds);
		
		double errorRate = totalLogs > 0 ? (double) errorLogs / totalLogs * 100 : 0;
		long logsPerMin = logIngestionFacade.getIngestedLogsPerMinute(visibleApplicationIds);
		long logsPerSec = logsPerMin / 60;

		List<AnomalyReportDto> recentCriticals = anomalyFacade.findReports(visibleApplicationIds).stream()
			.filter(r -> "CRITICAL".equals(r.severity()) && !"RESOLVED".equals(r.status()))
			.limit(5)
			.toList();

		List<OverviewMetricDto> metrics = new ArrayList<>();
		metrics.add(new OverviewMetricDto(
			"logs-per-second",
			"Log tiếp nhận/s",
			String.format("%,d", logsPerSec),
			null,
			"Thời gian thực (API)",
			"success"
		));
		
		metrics.add(new OverviewMetricDto(
			"error-rate",
			"Tỉ lệ lỗi",
			String.format("%.2f%%", errorRate),
			null,
			"Lỗi hệ thống",
			errorRate > 5 ? "error" : (errorRate > 1 ? "warning" : "success")
		));

		metrics.add(new OverviewMetricDto(
			"critical-alerts",
			"Cảnh báo hỏa tốc",
			String.valueOf(recentCriticals.size()),
			null,
			"Chưa xử lý",
			recentCriticals.size() > 0 ? "error" : "success"
		));

		metrics.add(new OverviewMetricDto(
			"active-apps",
			"Ứng dụng",
			String.valueOf(visibleApplicationIds.size()),
			null,
			"Đang hoạt động",
			"neutral"
		));

		// Volume points
		java.util.Map<Instant, ClickHouseAnalyticsRepository.LogVolumeRow> volumeMap = clickHouseRepository.getLogVolume(start, end, visibleApplicationIds).stream()
			.collect(Collectors.toMap(ClickHouseAnalyticsRepository.LogVolumeRow::bucket, row -> row));

		List<LogVolumePointDto> volume = new ArrayList<>();
		Instant currentBucket = start.truncatedTo(ChronoUnit.HOURS);
		Instant endBucket = end.truncatedTo(ChronoUnit.HOURS);
		while (!currentBucket.isAfter(endBucket)) {
			ClickHouseAnalyticsRepository.LogVolumeRow row = volumeMap.get(currentBucket);
			if (row != null) {
				volume.add(new LogVolumePointDto(
					row.bucket().toString(),
					row.infoCount(),
					row.warnCount(),
					row.errorCount(),
					row.criticalCount()
				));
			} else {
				volume.add(new LogVolumePointDto(
					currentBucket.toString(),
					0, 0, 0, 0
				));
			}
			currentBucket = currentBucket.plus(1, ChronoUnit.HOURS);
		}

		// Critical alerts
		List<CriticalAlertSummaryDto> alerts = recentCriticals.stream()
			.map(r -> new CriticalAlertSummaryDto(
				r.id(),
				r.severity(),
				"System", // Hardcoded application name for now since we don't fetch app details
				List.of(new LogSampleDto(r.severity(), r.title())),
				r.occurrenceCount(),
				TIME_FORMATTER.format(r.lastSeenAt()),
				r.status()
			))
			.toList();

		return new OverviewSnapshotDto(
			"Last 24 hours",
			Instant.now().toString(),
			metrics,
			volume,
			alerts
		);
	}

	private OverviewSnapshotDto createEmptySnapshot() {
		return new OverviewSnapshotDto(
			"Last 24 hours",
			Instant.now().toString(),
			List.of(
				new OverviewMetricDto("logs-per-second", "Log tiếp nhận/s", "0", null, "Thời gian thực (API)", "neutral"),
				new OverviewMetricDto("error-rate", "Tỉ lệ lỗi", "0%", null, "Lỗi hệ thống", "neutral"),
				new OverviewMetricDto("critical-alerts", "Cảnh báo hỏa tốc", "0", null, "Chưa xử lý", "success"),
				new OverviewMetricDto("active-apps", "Ứng dụng", "0", null, "Đang hoạt động", "neutral")
			),
			List.of(),
			List.of()
		);
	}
}
