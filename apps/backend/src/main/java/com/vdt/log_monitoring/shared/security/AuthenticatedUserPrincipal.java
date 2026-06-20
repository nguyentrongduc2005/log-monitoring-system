package com.vdt.log_monitoring.shared.security;

import java.security.Principal;
import java.util.UUID;

public record AuthenticatedUserPrincipal(
	UUID userId,
	String email,
	String role,
	String displayName
) implements Principal {

	@Override
	public String getName() {
		return email;
	}
}
