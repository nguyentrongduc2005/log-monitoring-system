package com.vdt.log_monitoring.api.identity;

import java.util.UUID;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vdt.log_monitoring.api.identity.dto.CreateUserRequest;
import com.vdt.log_monitoring.api.identity.dto.UpdateUserRequest;
import com.vdt.log_monitoring.api.identity.dto.UserPageResponse;
import com.vdt.log_monitoring.api.identity.dto.UserResponse;
import com.vdt.log_monitoring.modules.identity.api.IdentityFacade;
import com.vdt.log_monitoring.shared.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {

	private final IdentityFacade identityFacade;

	@PostMapping
	public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
		IdentityFacade.UserDto user = identityFacade.createUser(
			request.getEmail(),
			request.getPassword(),
			request.getDisplayName(),
			request.getRole()
		);
		return ResponseEntity.ok(ApiResponse.success(UserResponse.from(user)));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<UserPageResponse>> listUsers(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(required = false) String search,
		@RequestParam(required = false) String role,
		@RequestParam(required = false) String status,
		@RequestParam(defaultValue = "false") boolean includeDeleted
	) {
		IdentityFacade.UserPageDto users = identityFacade.findUsers(
			new IdentityFacade.UserSearchQuery(page, size, search, role, status, includeDeleted)
		);
		return ResponseEntity.ok(ApiResponse.success(UserPageResponse.from(users)));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id) {
		IdentityFacade.UserDto user = identityFacade.findUserById(id);
		return ResponseEntity.ok(ApiResponse.success(UserResponse.from(user)));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<UserResponse>> updateUser(
		@PathVariable UUID id,
		@Valid @RequestBody UpdateUserRequest request
	) {
		IdentityFacade.UserDto updated = identityFacade.updateUser(
			id,
			request.getEmail(),
			request.getDisplayName()
		);
		return ResponseEntity.ok(ApiResponse.success(UserResponse.from(updated)));
	}

	@PutMapping("/{id}/role")
	public ResponseEntity<ApiResponse<UserResponse>> changeRole(@PathVariable UUID id, @RequestBody String role) {
		String cleanRole = role.replace("\"", "").trim();
		IdentityFacade.UserDto updated = identityFacade.changeRole(id, cleanRole);
		return ResponseEntity.ok(ApiResponse.success(UserResponse.from(updated)));
	}

	@PutMapping("/{id}/status")
	public ResponseEntity<ApiResponse<UserResponse>> changeStatus(@PathVariable UUID id, @RequestBody String status) {
		String cleanStatus = status.replace("\"", "").trim();
		IdentityFacade.UserDto updated = identityFacade.changeStatus(id, cleanStatus);
		return ResponseEntity.ok(ApiResponse.success(UserResponse.from(updated)));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<UserResponse>> softDeleteUser(@PathVariable UUID id) {
		IdentityFacade.UserDto deleted = identityFacade.softDeleteUser(id);
		return ResponseEntity.ok(ApiResponse.success(UserResponse.from(deleted)));
	}
}
