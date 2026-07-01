package com.vdt.log_monitoring.api.retention;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import com.vdt.log_monitoring.api.retention.dto.RetentionPolicyResponse;
import com.vdt.log_monitoring.api.retention.dto.RetentionPolicyUpdateRequest;
import com.vdt.log_monitoring.api.retention.dto.RetentionRunResponse;
import com.vdt.log_monitoring.modules.retention.api.RetentionFacade;
import com.vdt.log_monitoring.shared.dto.ApiResponse;

@Validated
@RestController
@RequestMapping("/api/v1/retention/policies")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RetentionController {

	private final RetentionFacade retentionFacade;

	@GetMapping
	public ResponseEntity<ApiResponse<List<RetentionPolicyResponse>>> listPolicies() {
		List<RetentionPolicyResponse> policies = retentionFacade.findPolicies().stream()
			.map(RetentionPolicyResponse::from)
			.toList();
		return ResponseEntity.ok(ApiResponse.success(policies));
	}

	@PutMapping
	public ResponseEntity<ApiResponse<List<RetentionPolicyResponse>>> updatePolicies(
		@Valid @RequestBody List<@Valid RetentionPolicyUpdateRequest> requests
	) {
		List<RetentionFacade.UpdateRetentionPolicyCommand> commands = requests.stream()
			.map(request -> new RetentionFacade.UpdateRetentionPolicyCommand(
				request.id(),
				request.retentionDays(),
				request.enabled()
			))
			.toList();
		List<RetentionPolicyResponse> policies = retentionFacade.updatePolicies(commands).stream()
			.map(RetentionPolicyResponse::from)
			.toList();
		return ResponseEntity.ok(ApiResponse.success(policies));
	}

	@PostMapping("/{id}/run")
	public ResponseEntity<ApiResponse<RetentionRunResponse>> runPolicy(@PathVariable UUID id) {
		return ResponseEntity.ok(ApiResponse.success(RetentionRunResponse.from(retentionFacade.runPolicy(id))));
	}
}
