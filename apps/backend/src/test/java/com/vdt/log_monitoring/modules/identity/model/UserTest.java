package com.vdt.log_monitoring.modules.identity.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class UserTest {

	@Test
	void createNormalizesEmailAndActivatesUser() {
		Instant now = Instant.parse("2026-06-08T10:00:00Z");

		User user = User.create(
			"  Engineer@Example.com ",
			"password-hash",
			" Engineer ",
			UserRole.ENGINEER,
			now
		);

		assertThat(user.getId()).isNotNull();
		assertThat(user.getEmail()).isEqualTo("engineer@example.com");
		assertThat(user.getDisplayName()).isEqualTo("Engineer");
		assertThat(user.getRole()).isEqualTo(UserRole.ENGINEER);
		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(user.getCreatedAt()).isEqualTo(now);
		assertThat(user.getUpdatedAt()).isEqualTo(now);
	}

	@Test
	void changingStatusUpdatesModificationTime() {
		Instant createdAt = Instant.parse("2026-06-08T10:00:00Z");
		Instant changedAt = Instant.parse("2026-06-08T11:00:00Z");
		User user = User.create(
			"engineer@example.com",
			"password-hash",
			"Engineer",
			UserRole.ENGINEER,
			createdAt
		);

		user.changeStatus(UserStatus.DISABLED, changedAt);

		assertThat(user.getStatus()).isEqualTo(UserStatus.DISABLED);
		assertThat(user.getUpdatedAt()).isEqualTo(changedAt);
	}
}
