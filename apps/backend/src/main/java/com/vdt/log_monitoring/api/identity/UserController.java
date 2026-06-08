package com.vdt.log_monitoring.api.identity;

import java.security.Principal;
import java.util.UUID;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vdt.log_monitoring.api.identity.dto.ChangePasswordRequest;
import com.vdt.log_monitoring.api.identity.dto.UserResponse;
import com.vdt.log_monitoring.api.identity.dto.UpdateUserRequest;
import com.vdt.log_monitoring.modules.identity.api.IdentityFacade;
import com.vdt.log_monitoring.shared.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

	private final IdentityFacade identityFacade;

	@GetMapping("/me")
	public ResponseEntity<ApiResponse<UserResponse>> getProfile(Principal principal) {
		IdentityFacade.UserDto user = identityFacade.findUserByEmail(principal.getName());
		return ResponseEntity.ok(ApiResponse.success(UserResponse.from(user)));
	}

	@PutMapping("/me")
	public ResponseEntity<ApiResponse<UserResponse>> updateProfile(Principal principal, @Valid @RequestBody UpdateUserRequest request) {
		IdentityFacade.UserDto user = identityFacade.findUserByEmail(principal.getName());
		IdentityFacade.UserDto updated = identityFacade.updateProfile(user.id(), request.getEmail(), request.getDisplayName());
		return ResponseEntity.ok(ApiResponse.success(UserResponse.from(updated)));
	}

	@PutMapping("/me/password")
	public ResponseEntity<ApiResponse<Void>> changePassword(Principal principal, @Valid @RequestBody ChangePasswordRequest request) {
		IdentityFacade.UserDto user = identityFacade.findUserByEmail(principal.getName());
		identityFacade.changePassword(user.id(), request.getOldPassword(), request.getNewPassword());
		return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully"));
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id) {
		IdentityFacade.UserDto user = identityFacade.findUserById(id);
		return ResponseEntity.ok(ApiResponse.success(UserResponse.from(user)));
	}

	@PutMapping("/{id}/role")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<UserResponse>> changeRole(@PathVariable UUID id, @RequestBody String role) {
		String cleanRole = role.replace("\"", "").trim();
		IdentityFacade.UserDto updated = identityFacade.changeRole(id, cleanRole);
		return ResponseEntity.ok(ApiResponse.success(UserResponse.from(updated)));
	}

	@PutMapping("/{id}/status")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<UserResponse>> changeStatus(@PathVariable UUID id, @RequestBody String status) {
		String cleanStatus = status.replace("\"", "").trim();
		IdentityFacade.UserDto updated = identityFacade.changeStatus(id, cleanStatus);
		return ResponseEntity.ok(ApiResponse.success(UserResponse.from(updated)));
	}
}
