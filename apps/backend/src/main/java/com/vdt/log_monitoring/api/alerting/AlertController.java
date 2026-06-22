package com.vdt.log_monitoring.api.alerting;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.AccessDeniedException;

import com.vdt.log_monitoring.api.alerting.dto.AlertResponse;
import com.vdt.log_monitoring.modules.alerting.api.AlertingFacade;
import com.vdt.log_monitoring.modules.identity.api.IdentityFacade;
import com.vdt.log_monitoring.modules.identity.api.ApplicationAccessFacade;
import com.vdt.log_monitoring.shared.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

	private final AlertingFacade alertingFacade;
	private final IdentityFacade identityFacade;
	private final ApplicationAccessFacade applicationAccessFacade;

	@GetMapping
	public ResponseEntity<ApiResponse<List<AlertResponse>>> listAlerts(
		Principal principal,
		@RequestParam(required = false) UUID applicationId,
		@RequestParam(required = false) String status,
		@RequestParam(required = false) String severity
	) {
		IdentityFacade.UserDto user = identityFacade.findUserByEmail(principal.getName());
		List<UUID> visibleApplicationIds = visibleApplicationIds(user);
		if (applicationId != null) {
			ensureVisible(visibleApplicationIds, applicationId);
			visibleApplicationIds = List.of(applicationId);
		}
		List<AlertResponse> alerts = alertingFacade.findAlerts(visibleApplicationIds, status, severity).stream()
			.map(AlertResponse::from)
			.toList();
		return ResponseEntity.ok(ApiResponse.success(alerts));
	}

	@PutMapping("/{id}/acknowledge")
	public ResponseEntity<ApiResponse<AlertResponse>> acknowledgeAlert(
		Principal principal,
		@PathVariable UUID id
	) {
		IdentityFacade.UserDto user = identityFacade.findUserByEmail(principal.getName());
		ensureVisible(visibleApplicationIds(user), alertingFacade.findAlertById(id).applicationId());
		return ResponseEntity.ok(ApiResponse.success(
			AlertResponse.from(alertingFacade.acknowledgeAlert(id, user.id()))
		));
	}

	@PutMapping("/{id}/resolve")
	public ResponseEntity<ApiResponse<AlertResponse>> resolveAlert(
		Principal principal,
		@PathVariable UUID id
	) {
		IdentityFacade.UserDto user = identityFacade.findUserByEmail(principal.getName());
		ensureVisible(visibleApplicationIds(user), alertingFacade.findAlertById(id).applicationId());
		return ResponseEntity.ok(ApiResponse.success(
			AlertResponse.from(alertingFacade.resolveAlert(id, user.id()))
		));
	}

	private List<UUID> visibleApplicationIds(IdentityFacade.UserDto user) {
		return applicationAccessFacade.findVisibleApplications(user.id(), user.role()).stream()
			.map(ApplicationAccessFacade.ApplicationDto::id)
			.toList();
	}

	private void ensureVisible(List<UUID> visibleApplicationIds, UUID applicationId) {
		if (!visibleApplicationIds.contains(applicationId)) {
			throw new AccessDeniedException("User is not allowed to access this alert");
		}
	}
}
