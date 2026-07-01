package com.vdt.log_monitoring.modules.analytics.internal;

import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.analytics.api.DashboardFacade;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DashboardFacadeImpl implements DashboardFacade {

	private final AnalyticsService analyticsService;

	@Override
	public OverviewSnapshotDto getOverviewSnapshot(String window, java.util.List<java.util.UUID> visibleApplicationIds) {
		return analyticsService.getDashboardOverview(window, visibleApplicationIds);
	}
}
