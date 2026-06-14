package com.vdt.log_monitoring.modules.identity.internal.apikey;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
	name = "application_api_keys",
	schema = "identity",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_application_api_keys_prefix", columnNames = "key_prefix")
	},
	indexes = {
		@Index(name = "idx_application_api_keys_application_status", columnList = "application_id,status")
	}
)
public class ApplicationApiKeyEntity {

	@Id
	private UUID id;

	@Column(name = "application_id", nullable = false)
	private UUID applicationId;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(name = "key_prefix", nullable = false, length = 32)
	private String keyPrefix;

	@Column(name = "key_hash", nullable = false, length = 255)
	private String keyHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private ApiKeyStatus status;

	@Column(name = "expires_at")
	private Instant expiresAt;

	@Column(name = "last_used_at")
	private Instant lastUsedAt;

	@Column(name = "created_by", nullable = false)
	private UUID createdBy;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@PrePersist
	void onCreate() {
		createdAt = createdAt == null ? Instant.now() : createdAt;
	}

	public static ApplicationApiKeyEntity create(
		UUID applicationId,
		String name,
		String keyPrefix,
		String keyHash,
		Instant expiresAt,
		UUID createdBy
	) {
		Instant now = Instant.now();
		return new ApplicationApiKeyEntity(
			UUID.randomUUID(),
			Objects.requireNonNull(applicationId, "applicationId must not be null"),
			requireText(name, "name"),
			requireText(keyPrefix, "keyPrefix"),
			requireText(keyHash, "keyHash"),
			ApiKeyStatus.ACTIVE,
			expiresAt,
			null,
			Objects.requireNonNull(createdBy, "createdBy must not be null"),
			now,
			null
		);
	}

	public static ApplicationApiKeyEntity restore(
		UUID id,
		UUID applicationId,
		String name,
		String keyPrefix,
		String keyHash,
		ApiKeyStatus status,
		Instant expiresAt,
		Instant lastUsedAt,
		UUID createdBy,
		Instant createdAt,
		Instant revokedAt
	) {
		return new ApplicationApiKeyEntity(
			Objects.requireNonNull(id, "id must not be null"),
			Objects.requireNonNull(applicationId, "applicationId must not be null"),
			requireText(name, "name"),
			requireText(keyPrefix, "keyPrefix"),
			requireText(keyHash, "keyHash"),
			Objects.requireNonNull(status, "status must not be null"),
			expiresAt,
			lastUsedAt,
			Objects.requireNonNull(createdBy, "createdBy must not be null"),
			Objects.requireNonNull(createdAt, "createdAt must not be null"),
			revokedAt
		);
	}

	public void revoke() {
		this.status = ApiKeyStatus.REVOKED;
		this.revokedAt = Instant.now();
	}

	public boolean isUsable(Instant now) {
		Objects.requireNonNull(now, "now must not be null");
		return status == ApiKeyStatus.ACTIVE && (expiresAt == null || expiresAt.isAfter(now));
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value.trim();
	}
}
