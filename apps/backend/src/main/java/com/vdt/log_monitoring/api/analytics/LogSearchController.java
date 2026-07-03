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
import com.vdt.log_monitoring.modules.analytics.api.DashboardFacade.LogSearchRequestDto;
import com.vdt.log_monitoring.modules.analytics.api.DashboardFacade.LogSearchResponseDto;
import com.vdt.log_monitoring.modules.identity.api.ApplicationAccessFacade;
import com.vdt.log_monitoring.modules.identity.api.IdentityFacade;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
@Tag(name = "Logs Search", description = "Log querying and trace inspection")
public class LogSearchController {

	private final DashboardFacade dashboardFacade;
	private final IdentityFacade identityFacade;
	private final ApplicationAccessFacade applicationAccessFacade;

	@GetMapping("/search")
	@Operation(summary = "Search logs", description = "Query, paginate, and filter ClickHouse processed logs with application visibility controls")
	public ResponseEntity<LogSearchResponseDto> searchLogs(
		Principal principal,
		@RequestParam(required = false) String query,
		@RequestParam(required = false) UUID applicationId,
		@RequestParam(defaultValue = "ALL") String level,
		@RequestParam(defaultValue = "24h") String range,
		@RequestParam(defaultValue = "1") int page,
		@RequestParam(defaultValue = "20") int pageSize,
		@RequestParam(required = false) String selectedLogId
	) {
		IdentityFacade.UserDto user = identityFacade.findUserByEmail(principal.getName());
		List<UUID> visibleApplicationIds = applicationAccessFacade.findVisibleApplications(user.id(), user.role())
			.stream()
			.map(ApplicationAccessFacade.ApplicationDto::id)
			.toList();

		LogSearchRequestDto request = new LogSearchRequestDto(
			query,
			applicationId,
			level,
			range,
			page,
			pageSize,
			selectedLogId
		);

		return ResponseEntity.ok(dashboardFacade.searchLogs(request, visibleApplicationIds));
	}
}
