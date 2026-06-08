package com.vdt.log_monitoring.api.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
	private String accessToken;
	private String refreshToken;
	private UserResponse user;
}
