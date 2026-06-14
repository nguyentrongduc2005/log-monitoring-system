package com.vdt.log_monitoring.api.identity.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

import com.vdt.log_monitoring.modules.identity.api.ApplicationAccessFacade;

@Data
@Builder
public class ApplicationAccessResponse {
	private UUID userId;
	private UUID applicationId;
	private String applicationName;
	private String applicationDisplayName;
	private String accessLevel;
	private UUID grantedBy;
	private Instant createdAt;
	private Instant updatedAt;

	public static ApplicationAccessResponse from(ApplicationAccessFacade.ApplicationAccessDto dto) {
		return ApplicationAccessResponse.builder()
			.userId(dto.userId())
			.applicationId(dto.applicationId())
			.applicationName(dto.applicationName())
			.applicationDisplayName(dto.applicationDisplayName())
			.accessLevel(dto.accessLevel())
			.grantedBy(dto.grantedBy())
			.createdAt(dto.createdAt())
			.updatedAt(dto.updatedAt())
			.build();
	}
}
