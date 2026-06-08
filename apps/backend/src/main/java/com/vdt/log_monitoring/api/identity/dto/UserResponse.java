package com.vdt.log_monitoring.api.identity.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

import com.vdt.log_monitoring.modules.identity.api.IdentityFacade;

@Data
@Builder
public class UserResponse {
	private UUID id;
	private String email;
	private String displayName;
	private String role;
	private String status;
	private Instant lastLoginAt;
	private Instant createdAt;
	private Instant updatedAt;

	public static UserResponse from(IdentityFacade.UserDto user) {
		return UserResponse.builder()
			.id(user.id())
			.email(user.email())
			.displayName(user.displayName())
			.role(user.role())
			.status(user.status())
			.lastLoginAt(user.lastLoginAt())
			.createdAt(user.createdAt())
			.updatedAt(user.updatedAt())
			.build();
	}
}
