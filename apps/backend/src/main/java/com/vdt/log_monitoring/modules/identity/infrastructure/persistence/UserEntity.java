package com.vdt.log_monitoring.modules.identity.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.vdt.log_monitoring.modules.identity.model.User;
import com.vdt.log_monitoring.modules.identity.model.UserRole;
import com.vdt.log_monitoring.modules.identity.model.UserStatus;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
	name = "users",
	schema = "identity",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_users_email", columnNames = "email")
	},
	indexes = {
		@Index(name = "idx_users_status", columnList = "status")
	}
)
public class UserEntity {

	@Id
	private UUID id;

	@Column(nullable = false, length = 320)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Column(name = "display_name", nullable = false, length = 150)
	private String displayName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private UserRole role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private UserStatus status;

	@Column(name = "last_login_at")
	private Instant lastLoginAt;

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

	public static UserEntity fromModel(User user) {
		return new UserEntity(
			user.getId(),
			user.getEmail(),
			user.getPasswordHash(),
			user.getDisplayName(),
			user.getRole(),
			user.getStatus(),
			user.getLastLoginAt(),
			user.getCreatedAt(),
			user.getUpdatedAt()
		);
	}

	public User toModel() {
		return User.restore(
			id,
			email,
			passwordHash,
			displayName,
			role,
			status,
			lastLoginAt,
			createdAt,
			updatedAt
		);
	}
}
