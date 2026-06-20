package com.vdt.log_monitoring.api.alerting;

import java.security.Principal;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vdt.log_monitoring.api.alerting.dto.AlertResponse;
import com.vdt.log_monitoring.modules.alerting.api.AlertingFacade;
import com.vdt.log_monitoring.modules.identity.api.IdentityFacade;
import com.vdt.log_monitoring.shared.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

	private final AlertingFacade alertingFacade;
	private final IdentityFacade identityFacade;

	@PutMapping("/{id}/acknowledge")
	public ResponseEntity<ApiResponse<AlertResponse>> acknowledgeAlert(
		Principal principal,
		@PathVariable UUID id
	) {
		IdentityFacade.UserDto user = identityFacade.findUserByEmail(principal.getName());
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
		return ResponseEntity.ok(ApiResponse.success(
			AlertResponse.from(alertingFacade.resolveAlert(id, user.id()))
		));
	}
}
