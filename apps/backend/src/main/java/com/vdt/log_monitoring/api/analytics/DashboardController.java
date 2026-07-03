package com.vdt.log_monitoring.api.analytics;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vdt.log_monitoring.modules.analytics.api.DashboardFacade;
import com.vdt.log_monitoring.modules.analytics.api.DashboardFacade.OverviewSnapshotDto;
import com.vdt.log_monitoring.modules.identity.api.ApplicationAccessFacade;
import com.vdt.log_monitoring.modules.identity.api.IdentityFacade;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Operations overview and analytics")
public class DashboardController {

	private final DashboardFacade dashboardFacade;
	private final IdentityFacade identityFacade;
	private final ApplicationAccessFacade applicationAccessFacade;

	@GetMapping("/overview")
	@Operation(summary = "Get overview snapshot", description = "Retrieves high-level metrics, log volume chart, and recent critical alerts")
	public ResponseEntity<OverviewSnapshotDto> getOverviewSnapshot(
		Principal principal,
		@RequestParam(defaultValue = "24h") String window
	) {
		IdentityFacade.UserDto user = identityFacade.findUserByEmail(principal.getName());
		List<UUID> visibleApplicationIds = applicationAccessFacade.findVisibleApplications(user.id(), user.role())
			.stream()
			.map(ApplicationAccessFacade.ApplicationDto::id)
			.toList();

		return ResponseEntity.ok(dashboardFacade.getOverviewSnapshot(window, visibleApplicationIds));
	}
}
