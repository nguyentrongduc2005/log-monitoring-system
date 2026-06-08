package com.vdt.log_monitoring.api.identity;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vdt.log_monitoring.api.identity.dto.CreateUserRequest;
import com.vdt.log_monitoring.api.identity.dto.LoginRequest;
import com.vdt.log_monitoring.api.identity.dto.LoginResponse;
import com.vdt.log_monitoring.api.identity.dto.RefreshTokenRequest;
import com.vdt.log_monitoring.api.identity.dto.UserResponse;
import com.vdt.log_monitoring.modules.identity.api.IdentityFacade;
import com.vdt.log_monitoring.shared.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

	private final IdentityFacade identityFacade;

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
		IdentityFacade.TokenPairDto tokenPair = identityFacade.authenticate(request.getEmail(), request.getPassword());
		LoginResponse loginResponse = new LoginResponse(
			tokenPair.accessToken(),
			tokenPair.refreshToken(),
			UserResponse.from(tokenPair.user())
		);
		return ResponseEntity.ok(ApiResponse.success(loginResponse));
	}

	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<LoginResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		IdentityFacade.TokenPairDto tokenPair = identityFacade.refresh(request.getRefreshToken());
		LoginResponse loginResponse = new LoginResponse(
			tokenPair.accessToken(),
			tokenPair.refreshToken(),
			UserResponse.from(tokenPair.user())
		);
		return ResponseEntity.ok(ApiResponse.success(loginResponse));
	}

	@PostMapping("/register")
	public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody CreateUserRequest request) {
		IdentityFacade.UserDto user = identityFacade.createUser(
			request.getEmail(),
			request.getPassword(),
			request.getDisplayName(),
			request.getRole()
		);
		return ResponseEntity.ok(ApiResponse.success(UserResponse.from(user)));
	}
}
