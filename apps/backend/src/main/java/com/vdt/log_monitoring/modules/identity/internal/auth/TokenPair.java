package com.vdt.log_monitoring.modules.identity.internal.auth;

import com.vdt.log_monitoring.modules.identity.internal.user.UserEntity;

public record TokenPair(
	String accessToken,
	String refreshToken,
	UserEntity user
) {}
