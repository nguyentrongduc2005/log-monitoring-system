package com.vdt.log_monitoring.api.alerting;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vdt.log_monitoring.api.alerting.dto.AlertDeliveryTargetRequest;
import com.vdt.log_monitoring.api.alerting.dto.AlertRuleRequest;
import com.vdt.log_monitoring.api.alerting.dto.AlertRuleResponse;
import com.vdt.log_monitoring.api.alerting.dto.AlertRuleStatusRequest;
import com.vdt.log_monitoring.api.alerting.dto.UpdateAlertRuleRequest;
import com.vdt.log_monitoring.modules.alerting.api.AlertingFacade;
import com.vdt.log_monitoring.modules.identity.api.IdentityFacade;
import com.vdt.log_monitoring.shared.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/alert-rules")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AlertRuleController {

	private final AlertingFacade alertingFacade;
	private final IdentityFacade identityFacade;

	@PostMapping
	public ResponseEntity<ApiResponse<AlertRuleResponse>> createRule(
		Principal principal,
		@Valid @RequestBody AlertRuleRequest request
	) {
		IdentityFacade.UserDto user = identityFacade.findUserByEmail(principal.getName());
		AlertingFacade.AlertRuleDto rule = alertingFacade.createRule(
			new AlertingFacade.CreateAlertRuleCommand(
				request.getApplicationId(),
				request.getName(),
				request.getDescription(),
				request.getMinSeverity(),
				request.getKeywordPattern(),
				request.getThresholdCount(),
				request.getThresholdWindowSeconds(),
				request.getCooldownSeconds(),
				request.getChannels(),
				mapDeliveryTargets(request.getDeliveryTargets()),
				user.id()
			)
		);
		return ResponseEntity.ok(ApiResponse.success(AlertRuleResponse.from(rule)));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<AlertRuleResponse>>> listRules(
		@RequestParam(required = false) UUID applicationId
	) {
		List<AlertRuleResponse> rules = alertingFacade.findRules(applicationId).stream()
			.map(AlertRuleResponse::from)
			.toList();
		return ResponseEntity.ok(ApiResponse.success(rules));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<AlertRuleResponse>> getRule(@PathVariable UUID id) {
		return ResponseEntity.ok(ApiResponse.success(AlertRuleResponse.from(alertingFacade.findRuleById(id))));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<AlertRuleResponse>> updateRule(
		@PathVariable UUID id,
		@Valid @RequestBody UpdateAlertRuleRequest request
	) {
		AlertingFacade.AlertRuleDto rule = alertingFacade.updateRule(
			id,
			new AlertingFacade.UpdateAlertRuleCommand(
				request.getName(),
				request.getDescription(),
				request.getMinSeverity(),
				request.getKeywordPattern(),
				request.getThresholdCount(),
				request.getThresholdWindowSeconds(),
				request.getCooldownSeconds(),
				request.getChannels(),
				mapDeliveryTargets(request.getDeliveryTargets())
			)
		);
		return ResponseEntity.ok(ApiResponse.success(AlertRuleResponse.from(rule)));
	}

	@PutMapping("/{id}/status")
	public ResponseEntity<ApiResponse<AlertRuleResponse>> changeStatus(
		@PathVariable UUID id,
		@Valid @RequestBody AlertRuleStatusRequest request
	) {
		AlertingFacade.AlertRuleDto rule = alertingFacade.changeRuleStatus(id, request.getStatus());
		return ResponseEntity.ok(ApiResponse.success(AlertRuleResponse.from(rule)));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable UUID id) {
		alertingFacade.deleteRule(id);
		return ResponseEntity.ok(ApiResponse.success(null, "Alert rule deleted"));
	}

	private List<AlertingFacade.AlertDeliveryTargetCommand> mapDeliveryTargets(
		List<AlertDeliveryTargetRequest> targets
	) {
		if (targets == null) {
			return null;
		}
		return targets.stream()
			.map(target -> new AlertingFacade.AlertDeliveryTargetCommand(
				target.getChannel(),
				target.getChatRoomId()
			))
			.toList();
	}
}
