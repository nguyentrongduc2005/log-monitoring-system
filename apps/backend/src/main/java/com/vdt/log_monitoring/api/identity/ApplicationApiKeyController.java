package com.vdt.log_monitoring.api.identity;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vdt.log_monitoring.api.identity.dto.ApiKeyCreationResponse;
import com.vdt.log_monitoring.api.identity.dto.ApiKeyResponse;
import com.vdt.log_monitoring.api.identity.dto.CreateApiKeyRequest;
import com.vdt.log_monitoring.modules.identity.api.ApplicationAccessFacade;
import com.vdt.log_monitoring.modules.identity.api.IdentityFacade;
import com.vdt.log_monitoring.shared.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/applications/{applicationId}/api-keys")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ApplicationApiKeyController {

	private final ApplicationAccessFacade applicationAccessFacade;
	private final IdentityFacade identityFacade;

	@PostMapping
	public ResponseEntity<ApiResponse<ApiKeyCreationResponse>> createApiKey(
		@PathVariable UUID applicationId,
		Principal principal,
		@Valid @RequestBody CreateApiKeyRequest request
	) {
		ApplicationAccessFacade.ApiKeyCreationDto apiKey = applicationAccessFacade.createApiKey(
			applicationId,
			request.getName(),
			request.getExpiresAt(),
			currentUserId(principal)
		);
		return ResponseEntity.ok(ApiResponse.success(ApiKeyCreationResponse.from(apiKey)));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<ApiKeyResponse>>> listApiKeys(@PathVariable UUID applicationId) {
		List<ApiKeyResponse> apiKeys = applicationAccessFacade.findApiKeys(applicationId).stream()
			.map(ApiKeyResponse::from)
			.toList();
		return ResponseEntity.ok(ApiResponse.success(apiKeys));
	}

	@PostMapping("/{apiKeyId}/rotate")
	public ResponseEntity<ApiResponse<ApiKeyCreationResponse>> rotateApiKey(
		@PathVariable UUID applicationId,
		@PathVariable UUID apiKeyId,
		Principal principal
	) {
		ApplicationAccessFacade.ApiKeyCreationDto apiKey =
			applicationAccessFacade.rotateApiKey(applicationId, apiKeyId, currentUserId(principal));
		return ResponseEntity.ok(ApiResponse.success(ApiKeyCreationResponse.from(apiKey)));
	}

	@PostMapping("/{apiKeyId}/revoke")
	public ResponseEntity<ApiResponse<Void>> revokeApiKey(
		@PathVariable UUID applicationId,
		@PathVariable UUID apiKeyId,
		Principal principal
	) {
		applicationAccessFacade.revokeApiKey(applicationId, apiKeyId, currentUserId(principal));
		return ResponseEntity.ok(ApiResponse.success(null));
	}

	private UUID currentUserId(Principal principal) {
		return identityFacade.findUserByEmail(principal.getName()).id();
	}
}
