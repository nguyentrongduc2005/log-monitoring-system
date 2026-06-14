package com.vdt.log_monitoring.modules.identity.internal.access;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
	name = "user_application_access",
	schema = "identity",
	indexes = {
		@Index(
			name = "idx_user_application_access_application_user",
			columnList = "application_id,user_id"
		)
	}
)
public class UserApplicationAccessEntity {

	@EmbeddedId
	private UserApplicationAccessId id;

	@Enumerated(EnumType.STRING)
	@Column(name = "access_level", nullable = false, length = 32)
	private ApplicationAccessLevel accessLevel;

	@Column(name = "granted_by", nullable = false)
	private UUID grantedBy;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		createdAt = createdAt == null ? now : createdAt;
		updatedAt = updatedAt == null ? now : updatedAt;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	public static UserApplicationAccessEntity create(
		UUID userId,
		UUID applicationId,
		ApplicationAccessLevel accessLevel,
		UUID grantedBy
	) {
		Instant now = Instant.now();
		return new UserApplicationAccessEntity(
			new UserApplicationAccessId(
				Objects.requireNonNull(userId, "userId must not be null"),
				Objects.requireNonNull(applicationId, "applicationId must not be null")
			),
			Objects.requireNonNull(accessLevel, "accessLevel must not be null"),
			Objects.requireNonNull(grantedBy, "grantedBy must not be null"),
			now,
			now
		);
	}

	public static UserApplicationAccessEntity restore(
		UUID userId,
		UUID applicationId,
		ApplicationAccessLevel accessLevel,
		UUID grantedBy,
		Instant createdAt,
		Instant updatedAt
	) {
		return new UserApplicationAccessEntity(
			new UserApplicationAccessId(
				Objects.requireNonNull(userId, "userId must not be null"),
				Objects.requireNonNull(applicationId, "applicationId must not be null")
			),
			Objects.requireNonNull(accessLevel, "accessLevel must not be null"),
			Objects.requireNonNull(grantedBy, "grantedBy must not be null"),
			Objects.requireNonNull(createdAt, "createdAt must not be null"),
			Objects.requireNonNull(updatedAt, "updatedAt must not be null")
		);
	}

	public void changeAccessLevel(ApplicationAccessLevel accessLevel, UUID grantedBy) {
		this.accessLevel = Objects.requireNonNull(accessLevel, "accessLevel must not be null");
		this.grantedBy = Objects.requireNonNull(grantedBy, "grantedBy must not be null");
	}
}
