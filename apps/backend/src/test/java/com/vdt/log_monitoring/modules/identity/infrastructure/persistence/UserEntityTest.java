package com.vdt.log_monitoring.modules.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.vdt.log_monitoring.modules.identity.model.User;
import com.vdt.log_monitoring.modules.identity.model.UserRole;

class UserEntityTest {

	@Test
	void mapsModelWithoutLosingIdentityState() {
		Instant now = Instant.parse("2026-06-08T10:00:00Z");
		User original = User.create(
			"admin@example.com",
			"password-hash",
			"Admin",
			UserRole.ADMIN,
			now
		);

		User restored = UserEntity.fromModel(original).toModel();

		assertThat(restored.getId()).isEqualTo(original.getId());
		assertThat(restored.getEmail()).isEqualTo(original.getEmail());
		assertThat(restored.getPasswordHash()).isEqualTo(original.getPasswordHash());
		assertThat(restored.getDisplayName()).isEqualTo(original.getDisplayName());
		assertThat(restored.getRole()).isEqualTo(original.getRole());
		assertThat(restored.getStatus()).isEqualTo(original.getStatus());
		assertThat(restored.getCreatedAt()).isEqualTo(original.getCreatedAt());
		assertThat(restored.getUpdatedAt()).isEqualTo(original.getUpdatedAt());
	}
}
