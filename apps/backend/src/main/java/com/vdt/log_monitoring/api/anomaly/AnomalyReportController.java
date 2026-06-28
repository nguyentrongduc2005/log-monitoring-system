package com.vdt.log_monitoring.api.anomaly;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vdt.log_monitoring.modules.anomaly.api.AnomalyFacade;
import com.vdt.log_monitoring.modules.identity.api.ApplicationAccessFacade;
import com.vdt.log_monitoring.modules.identity.api.IdentityFacade;
import com.vdt.log_monitoring.shared.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/anomaly/reports")
@RequiredArgsConstructor
public class AnomalyReportController {

	private final AnomalyFacade anomalyFacade;
	private final IdentityFacade identityFacade;
	private final ApplicationAccessFacade applicationAccessFacade;

	@GetMapping
	public ResponseEntity<ApiResponse<List<AnomalyFacade.AnomalyReportDto>>> listReports(
		Principal principal,
		@RequestParam(required = false) UUID applicationId
	) {
		IdentityFacade.UserDto user = identityFacade.findUserByEmail(principal.getName());
		List<UUID> visibleApplicationIds = visibleApplicationIds(user);
		if (applicationId != null) {
			ensureVisible(visibleApplicationIds, applicationId);
			visibleApplicationIds = List.of(applicationId);
		}
		return ResponseEntity.ok(ApiResponse.success(anomalyFacade.findReports(visibleApplicationIds)));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<AnomalyFacade.AnomalyReportDto>> getReport(
		Principal principal,
		@PathVariable UUID id
	) {
		IdentityFacade.UserDto user = identityFacade.findUserByEmail(principal.getName());
		List<UUID> visibleApplicationIds = visibleApplicationIds(user);
		AnomalyFacade.AnomalyReportDto report = anomalyFacade.findReportById(id);
		ensureVisible(visibleApplicationIds, report.applicationId());
		return ResponseEntity.ok(ApiResponse.success(report));
	}

	private List<UUID> visibleApplicationIds(IdentityFacade.UserDto user) {
		return applicationAccessFacade.findVisibleApplications(user.id(), user.role()).stream()
			.map(ApplicationAccessFacade.ApplicationDto::id)
			.toList();
	}

	private void ensureVisible(List<UUID> visibleApplicationIds, UUID applicationId) {
		if (!visibleApplicationIds.contains(applicationId)) {
			throw new AccessDeniedException("User is not allowed to access this anomaly report");
		}
	}
}
