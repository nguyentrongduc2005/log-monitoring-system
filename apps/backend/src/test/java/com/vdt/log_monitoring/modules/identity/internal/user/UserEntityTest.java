package com.vdt.log_monitoring.modules.identity.internal.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserEntityTest {

	@Test
	void createNormalizesEmailAndActivatesUser() {
		UserEntity user = UserEntity.create(
			"  Engineer@Example.com ",
			"password-hash",
			" Engineer ",
			UserRole.ENGINEER
		);

		assertThat(user.getId()).isNotNull();
		assertThat(user.getEmail()).isEqualTo("engineer@example.com");
		assertThat(user.getDisplayName()).isEqualTo("Engineer");
		assertThat(user.getRole()).isEqualTo(UserRole.ENGINEER);
		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(user.getCreatedAt()).isNotNull();
		assertThat(user.getUpdatedAt()).isNotNull();
	}

	@Test
	void changesMutableIdentityState() {
		UserEntity user = UserEntity.create(
			"engineer@example.com",
			"password-hash",
			"Engineer",
			UserRole.ENGINEER
		);

		user.updateProfile(" ADMIN@EXAMPLE.COM ", " Admin ");
		user.changePasswordHash("new-hash");
		user.changeRole(UserRole.ADMIN);
		user.changeStatus(UserStatus.DISABLED);
		user.recordSuccessfulLogin();

		assertThat(user.getEmail()).isEqualTo("admin@example.com");
		assertThat(user.getDisplayName()).isEqualTo("Admin");
		assertThat(user.getPasswordHash()).isEqualTo("new-hash");
		assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
		assertThat(user.getStatus()).isEqualTo(UserStatus.DISABLED);
		assertThat(user.getLastLoginAt()).isNotNull();
	}

	@Test
	void softDeleteMarksUserDeletedAndStoresTimestamp() {
		UserEntity user = UserEntity.create(
			"engineer@example.com",
			"password-hash",
			"Engineer",
			UserRole.ENGINEER
		);

		user.softDelete();

		assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
		assertThat(user.getDeletedAt()).isNotNull();

		user.changeStatus(UserStatus.ACTIVE);

		assertThat(user.getDeletedAt()).isNull();
	}
}
