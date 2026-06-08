package com.vdt.log_monitoring.modules.identity.application;

import com.vdt.log_monitoring.modules.identity.model.User;

public record TokenPair(
	String accessToken,
	String refreshToken,
	User user
) {}
