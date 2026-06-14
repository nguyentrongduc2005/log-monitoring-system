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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vdt.log_monitoring.api.identity.dto.ApplicationRequest;
import com.vdt.log_monitoring.api.identity.dto.ApplicationResponse;
import com.vdt.log_monitoring.api.identity.dto.ApplicationStatusRequest;
import com.vdt.log_monitoring.modules.identity.api.ApplicationAccessFacade;
import com.vdt.log_monitoring.modules.identity.api.IdentityFacade;
import com.vdt.log_monitoring.shared.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

	private final ApplicationAccessFacade applicationAccessFacade;
	private final IdentityFacade identityFacade;

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<ApplicationResponse>> createApplication(
		Principal principal,
		@Valid @RequestBody ApplicationRequest request
	) {
		IdentityFacade.UserDto admin = identityFacade.findUserByEmail(principal.getName());
		ApplicationAccessFacade.ApplicationDto application = applicationAccessFacade.createApplication(
			new ApplicationAccessFacade.CreateApplicationCommand(
				request.getName(),
				request.getDisplayName(),
				request.getDescription(),
				admin.id()
			)
		);
		return ResponseEntity.ok(ApiResponse.success(ApplicationResponse.from(application)));
	}

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<List<ApplicationResponse>>> listApplications() {
		List<ApplicationResponse> applications = applicationAccessFacade.findAllApplications().stream()
			.map(ApplicationResponse::from)
			.toList();
		return ResponseEntity.ok(ApiResponse.success(applications));
	}

	@GetMapping("/me")
	public ResponseEntity<ApiResponse<List<ApplicationResponse>>> listVisibleApplications(Principal principal) {
		IdentityFacade.UserDto user = identityFacade.findUserByEmail(principal.getName());
		List<ApplicationResponse> applications = applicationAccessFacade.findVisibleApplications(user.id(), user.role()).stream()
			.map(ApplicationResponse::from)
			.toList();
		return ResponseEntity.ok(ApiResponse.success(applications));
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<ApplicationResponse>> getApplicationById(@PathVariable UUID id) {
		ApplicationAccessFacade.ApplicationDto application = applicationAccessFacade.findApplicationById(id);
		return ResponseEntity.ok(ApiResponse.success(ApplicationResponse.from(application)));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<ApplicationResponse>> updateApplication(
		@PathVariable UUID id,
		@Valid @RequestBody ApplicationRequest request
	) {
		ApplicationAccessFacade.ApplicationDto application = applicationAccessFacade.updateApplication(
			id,
			new ApplicationAccessFacade.UpdateApplicationCommand(
				request.getName(),
				request.getDisplayName(),
				request.getDescription()
			)
		);
		return ResponseEntity.ok(ApiResponse.success(ApplicationResponse.from(application)));
	}

	@PutMapping("/{id}/status")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<ApplicationResponse>> changeStatus(
		@PathVariable UUID id,
		@Valid @RequestBody ApplicationStatusRequest request
	) {
		ApplicationAccessFacade.ApplicationDto application =
			applicationAccessFacade.changeApplicationStatus(id, request.getStatus());
		return ResponseEntity.ok(ApiResponse.success(ApplicationResponse.from(application)));
	}
}
