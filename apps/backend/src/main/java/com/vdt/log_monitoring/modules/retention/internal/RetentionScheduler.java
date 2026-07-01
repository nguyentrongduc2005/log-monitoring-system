package com.vdt.log_monitoring.modules.retention.internal;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RetentionScheduler {

	private final RetentionService retentionService;

	@Scheduled(fixedDelayString = "${app.retention.interval-ms:3600000}")
	public void runRetention() {
		retentionService.runDuePolicies();
	}
}
