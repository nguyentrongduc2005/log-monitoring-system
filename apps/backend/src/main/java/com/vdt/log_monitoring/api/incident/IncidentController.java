package com.vdt.log_monitoring.api.incident;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vdt.log_monitoring.modules.alerting.api.AlertingFacade;
import com.vdt.log_monitoring.modules.identity.api.ApplicationAccessFacade;
import com.vdt.log_monitoring.modules.identity.api.IdentityFacade;
import com.vdt.log_monitoring.modules.incident.api.IncidentFacade;
import com.vdt.log_monitoring.shared.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
public class IncidentController {

	private final IncidentFacade incidentFacade;
	private final AlertingFacade alertingFacade;
	private final IdentityFacade identityFacade;
	private final ApplicationAccessFacade applicationAccessFacade;

	@GetMapping
	public ResponseEntity<ApiResponse<List<IncidentFacade.IncidentSummaryDto>>> listIncidents(
			Principal principal,
			@RequestParam(required = false) UUID applicationId,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String severity) {
		IdentityFacade.UserDto user = identityFacade.findUserByEmail(principal.getName());
		List<UUID> visibleApplicationIds = visibleApplicationIds(user);
		if (applicationId != null) {
			ensureVisible(visibleApplicationIds, applicationId, "User is not allowed to access this application");
			visibleApplicationIds = List.of(applicationId);
		}
		return ResponseEntity.ok(ApiResponse.success(
				incidentFacade.findIncidents(visibleApplicationIds, status, severity)));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<IncidentFacade.IncidentDto>> getIncident(
			Principal principal,
			@PathVariable UUID id) {
		IdentityFacade.UserDto user = identityFacade.findUserByEmail(principal.getName());
		IncidentFacade.IncidentDto incident = incidentFacade.findIncidentById(id);
		ensureAnyVisible(visibleApplicationIds(user), incident.applications().stream()
				.map(IncidentFacade.ApplicationImpactDto::applicationId)
				.toList());
		return ResponseEntity.ok(ApiResponse.success(incident));
	}

	@PostMapping("/from-alert/{alertId}")
	public ResponseEntity<ApiResponse<IncidentFacade.IncidentDto>> startFromAlert(
			Principal principal,
			@PathVariable UUID alertId) {
		IdentityFacade.UserDto user = identityFacade.findUserByEmail(principal.getName());
		List<UUID> visibleApplicationIds = visibleApplicationIds(user);
		AlertingFacade.AlertDto alert = alertingFacade.findAlertById(alertId);
		ensureVisible(visibleApplicationIds, alert.applicationId(),
				"User is not allowed to investigate this alert");
		IncidentFacade.IncidentDto incident = incidentFacade.startFromAlert(
				new IncidentFacade.StartFromAlertCommand(
						alert.id(),
						alert.applicationId(),
						alert.applicationName(),
						alert.applicationDisplayName(),
						alert.severity(),
						alert.logSamples(),
						alert.triggeredAt(),
						user.id()));
		return ResponseEntity.ok(ApiResponse.success(incident));
	}

	@PutMapping("/{id}/resolve")
	public ResponseEntity<ApiResponse<IncidentFacade.IncidentDto>> resolveIncident(
			Principal principal,
			@PathVariable UUID id) {
		IdentityFacade.UserDto user = identityFacade.findUserByEmail(principal.getName());
		IncidentFacade.IncidentDto incident = incidentFacade.findIncidentById(id);
		ensureAnyVisible(visibleApplicationIds(user), incident.applications().stream()
				.map(IncidentFacade.ApplicationImpactDto::applicationId)
				.toList());
		return ResponseEntity.ok(ApiResponse.success(incidentFacade.resolveIncident(id, user.id())));
	}

	@GetMapping("/anomaly-reports")
	public ResponseEntity<ApiResponse<List<IncidentFacade.IncidentAnomalyReportDto>>> listAnomalyReports(
			Principal principal) {
		IdentityFacade.UserDto user = identityFacade.findUserByEmail(principal.getName());
		List<UUID> visibleApplicationIds = visibleApplicationIds(user);
		return ResponseEntity.ok(ApiResponse.success(incidentFacade.findAnomalyReports(visibleApplicationIds)));
	}

	@GetMapping("/anomaly-reports/{id}")
	public ResponseEntity<ApiResponse<IncidentFacade.IncidentAnomalyReportDto>> getAnomalyReport(
			Principal principal,
			@PathVariable UUID id) {
		// Just a basic check for auth
		IdentityFacade.UserDto user = identityFacade.findUserByEmail(principal.getName());
		visibleApplicationIds(user); 
		return ResponseEntity.ok(ApiResponse.success(incidentFacade.findAnomalyReportById(id)));
	}

	private List<UUID> visibleApplicationIds(IdentityFacade.UserDto user) {
		return applicationAccessFacade.findVisibleApplications(user.id(), user.role()).stream()
				.map(ApplicationAccessFacade.ApplicationDto::id)
				.toList();
	}

	private void ensureAnyVisible(List<UUID> visibleApplicationIds, List<UUID> incidentApplicationIds) {
		boolean allowed = incidentApplicationIds.stream().anyMatch(visibleApplicationIds::contains);
		if (!allowed) {
			throw new AccessDeniedException("User is not allowed to access this incident");
		}
	}

	private void ensureVisible(List<UUID> visibleApplicationIds, UUID applicationId, String message) {
		if (!visibleApplicationIds.contains(applicationId)) {
			throw new AccessDeniedException(message);
		}
	}
}
