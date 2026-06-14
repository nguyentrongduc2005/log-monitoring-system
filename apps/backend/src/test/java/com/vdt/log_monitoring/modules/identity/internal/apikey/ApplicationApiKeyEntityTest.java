package com.vdt.log_monitoring.modules.identity.internal.apikey;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ApplicationApiKeyEntityTest {

	@Test
	void createStoresMetadataWithoutRawKey() {
		Instant expiresAt = Instant.parse("2026-06-11T06:00:00Z");
		UUID applicationId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID createdBy = UUID.fromString("00000000-0000-0000-0000-000000000002");

		ApplicationApiKeyEntity apiKey = ApplicationApiKeyEntity.create(
			applicationId,
			" production ",
			" lms_live_abc ",
			" hash ",
			expiresAt,
			createdBy
		);

		assertThat(apiKey.getId()).isNotNull();
		assertThat(apiKey.getApplicationId()).isEqualTo(applicationId);
		assertThat(apiKey.getName()).isEqualTo("production");
		assertThat(apiKey.getKeyPrefix()).isEqualTo("lms_live_abc");
		assertThat(apiKey.getKeyHash()).isEqualTo("hash");
		assertThat(apiKey.getStatus()).isEqualTo(ApiKeyStatus.ACTIVE);
		assertThat(apiKey.getExpiresAt()).isEqualTo(expiresAt);
		assertThat(apiKey.getCreatedBy()).isEqualTo(createdBy);
		assertThat(apiKey.getCreatedAt()).isNotNull();
	}

	@Test
	void revokeMarksKeyAsRevoked() {
		ApplicationApiKeyEntity apiKey = ApplicationApiKeyEntity.create(
			UUID.fromString("00000000-0000-0000-0000-000000000001"),
			"production",
			"lms_live_abc",
			"hash",
			null,
			UUID.fromString("00000000-0000-0000-0000-000000000002")
		);

		apiKey.revoke();

		assertThat(apiKey.getStatus()).isEqualTo(ApiKeyStatus.REVOKED);
		assertThat(apiKey.getRevokedAt()).isNotNull();
		assertThat(apiKey.isUsable(Instant.now())).isFalse();
	}

	@Test
	void usableOnlyWhenActiveAndNotExpired() {
		ApplicationApiKeyEntity apiKey = ApplicationApiKeyEntity.create(
			UUID.fromString("00000000-0000-0000-0000-000000000001"),
			"production",
			"lms_live_abc",
			"hash",
			Instant.parse("2026-06-10T07:00:00Z"),
			UUID.fromString("00000000-0000-0000-0000-000000000002")
		);

		assertThat(apiKey.isUsable(Instant.parse("2026-06-10T06:30:00Z"))).isTrue();
		assertThat(apiKey.isUsable(Instant.parse("2026-06-10T08:00:00Z"))).isFalse();
	}
}
