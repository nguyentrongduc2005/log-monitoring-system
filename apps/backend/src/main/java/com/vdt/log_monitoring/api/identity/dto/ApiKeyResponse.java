package com.vdt.log_monitoring.api.identity.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

import com.vdt.log_monitoring.modules.identity.api.ApplicationAccessFacade;

@Data
@Builder
public class ApiKeyResponse {
	private UUID id;
	private UUID applicationId;
	private String name;
	private String keyPrefix;
	private String status;
	private Instant expiresAt;
	private Instant lastUsedAt;
	private Instant createdAt;
	private Instant revokedAt;

	public static ApiKeyResponse from(ApplicationAccessFacade.ApiKeyDto dto) {
		return ApiKeyResponse.builder()
			.id(dto.id())
			.applicationId(dto.applicationId())
			.name(dto.name())
			.keyPrefix(dto.keyPrefix())
			.status(dto.status())
			.expiresAt(dto.expiresAt())
			.lastUsedAt(dto.lastUsedAt())
			.createdAt(dto.createdAt())
			.revokedAt(dto.revokedAt())
			.build();
	}
}
