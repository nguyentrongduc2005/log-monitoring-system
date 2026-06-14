package com.vdt.log_monitoring.api.identity;

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
import org.springframework.web.bind.annotation.RestController;

import com.vdt.log_monitoring.api.identity.dto.ApplicationAccessGrantRequest;
import com.vdt.log_monitoring.api.identity.dto.ApplicationAccessResponse;
import com.vdt.log_monitoring.api.identity.dto.ReplaceApplicationAccessRequest;
import com.vdt.log_monitoring.modules.identity.api.ApplicationAccessFacade;
import com.vdt.log_monitoring.modules.identity.api.IdentityFacade;
import com.vdt.log_monitoring.shared.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/users/{userId}/applications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ApplicationAccessController {

	private final ApplicationAccessFacade applicationAccessFacade;
	private final IdentityFacade identityFacade;

	@GetMapping
	public ResponseEntity<ApiResponse<List<ApplicationAccessResponse>>> listUserApplicationAccess(
		@PathVariable UUID userId
	) {
		List<ApplicationAccessResponse> grants = applicationAccessFacade.findUserApplicationAccess(userId).stream()
			.map(ApplicationAccessResponse::from)
			.toList();
		return ResponseEntity.ok(ApiResponse.success(grants));
	}

	@PutMapping
	public ResponseEntity<ApiResponse<List<ApplicationAccessResponse>>> replaceUserApplicationAccess(
		@PathVariable UUID userId,
		Principal principal,
		@Valid @RequestBody ReplaceApplicationAccessRequest request
	) {
		UUID adminId = currentUserId(principal);
		List<ApplicationAccessFacade.ApplicationAccessGrantCommand> grants = request.getGrants().stream()
			.map(grant -> new ApplicationAccessFacade.ApplicationAccessGrantCommand(
				grant.getApplicationId(),
				grant.getAccessLevel()
			))
			.toList();
		List<ApplicationAccessResponse> updated =
			applicationAccessFacade.replaceUserApplicationAccess(userId, grants, adminId).stream()
				.map(ApplicationAccessResponse::from)
				.toList();
		return ResponseEntity.ok(ApiResponse.success(updated));
	}

	@PostMapping("/{applicationId}")
	public ResponseEntity<ApiResponse<ApplicationAccessResponse>> grantUserApplicationAccess(
		@PathVariable UUID userId,
		@PathVariable UUID applicationId,
		Principal principal,
		@Valid @RequestBody ApplicationAccessGrantRequest request
	) {
		ApplicationAccessFacade.ApplicationAccessDto grant =
			applicationAccessFacade.grantUserApplicationAccess(
				userId,
				applicationId,
				request.getAccessLevel(),
				currentUserId(principal)
			);
		return ResponseEntity.ok(ApiResponse.success(ApplicationAccessResponse.from(grant)));
	}

	@DeleteMapping("/{applicationId}")
	public ResponseEntity<ApiResponse<Void>> removeUserApplicationAccess(
		@PathVariable UUID userId,
		@PathVariable UUID applicationId
	) {
		applicationAccessFacade.removeUserApplicationAccess(userId, applicationId);
		return ResponseEntity.ok(ApiResponse.success(null));
	}

	private UUID currentUserId(Principal principal) {
		return identityFacade.findUserByEmail(principal.getName()).id();
	}
}
