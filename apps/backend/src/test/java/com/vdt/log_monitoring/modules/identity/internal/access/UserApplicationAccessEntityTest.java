package com.vdt.log_monitoring.modules.identity.internal.access;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class UserApplicationAccessEntityTest {

	@Test
	void createStoresGrantState() {
		UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID applicationId = UUID.fromString("00000000-0000-0000-0000-000000000002");
		UUID grantedBy = UUID.fromString("00000000-0000-0000-0000-000000000003");

		UserApplicationAccessEntity access = UserApplicationAccessEntity.create(
			userId,
			applicationId,
			ApplicationAccessLevel.VIEW,
			grantedBy
		);

		assertThat(access.getId().getUserId()).isEqualTo(userId);
		assertThat(access.getId().getApplicationId()).isEqualTo(applicationId);
		assertThat(access.getAccessLevel()).isEqualTo(ApplicationAccessLevel.VIEW);
		assertThat(access.getGrantedBy()).isEqualTo(grantedBy);
		assertThat(access.getCreatedAt()).isNotNull();
		assertThat(access.getUpdatedAt()).isNotNull();
	}

	@Test
	void changeAccessLevelUpdatesGrantMetadata() {
		UUID nextGrantedBy = UUID.fromString("00000000-0000-0000-0000-000000000004");
		UserApplicationAccessEntity access = UserApplicationAccessEntity.create(
			UUID.fromString("00000000-0000-0000-0000-000000000001"),
			UUID.fromString("00000000-0000-0000-0000-000000000002"),
			ApplicationAccessLevel.VIEW,
			UUID.fromString("00000000-0000-0000-0000-000000000003")
		);

		access.changeAccessLevel(ApplicationAccessLevel.MANAGE, nextGrantedBy);

		assertThat(access.getAccessLevel()).isEqualTo(ApplicationAccessLevel.MANAGE);
		assertThat(access.getGrantedBy()).isEqualTo(nextGrantedBy);
	}
}
