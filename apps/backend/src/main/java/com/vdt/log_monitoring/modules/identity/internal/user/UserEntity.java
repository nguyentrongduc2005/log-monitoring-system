package com.vdt.log_monitoring.modules.identity.internal.user;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
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

	@Column(name = "deleted_at")
	private Instant deletedAt;

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

	public static UserEntity create(
		String email,
		String passwordHash,
		String displayName,
		UserRole role
	) {
		Instant now = Instant.now();
		return new UserEntity(
			UUID.randomUUID(),
			normalizeEmail(email),
			requireText(passwordHash, "passwordHash"),
			requireText(displayName, "displayName"),
			Objects.requireNonNull(role, "role must not be null"),
			UserStatus.ACTIVE,
			null,
			now,
			now,
			null
		);
	}

	public static UserEntity restore(
		UUID id,
		String email,
		String passwordHash,
		String displayName,
		UserRole role,
		UserStatus status,
		Instant lastLoginAt,
		Instant createdAt,
		Instant updatedAt,
		Instant deletedAt
	) {
		return new UserEntity(
			Objects.requireNonNull(id, "id must not be null"),
			normalizeEmail(email),
			requireText(passwordHash, "passwordHash"),
			requireText(displayName, "displayName"),
			Objects.requireNonNull(role, "role must not be null"),
			Objects.requireNonNull(status, "status must not be null"),
			lastLoginAt,
			Objects.requireNonNull(createdAt, "createdAt must not be null"),
			Objects.requireNonNull(updatedAt, "updatedAt must not be null"),
			deletedAt
		);
	}

	public static UserEntity restore(
		UUID id,
		String email,
		String passwordHash,
		String displayName,
		UserRole role,
		UserStatus status,
		Instant lastLoginAt,
		Instant createdAt,
		Instant updatedAt
	) {
		return restore(
			id,
			email,
			passwordHash,
			displayName,
			role,
			status,
			lastLoginAt,
			createdAt,
			updatedAt,
			null
		);
	}

	public void updateProfile(String email, String displayName) {
		this.email = normalizeEmail(email);
		this.displayName = requireText(displayName, "displayName");
	}

	public void changePasswordHash(String passwordHash) {
		this.passwordHash = requireText(passwordHash, "passwordHash");
	}

	public void changeRole(UserRole role) {
		this.role = Objects.requireNonNull(role, "role must not be null");
	}

	public void changeStatus(UserStatus status) {
		this.status = Objects.requireNonNull(status, "status must not be null");
		if (status != UserStatus.DELETED) {
			this.deletedAt = null;
		}
	}

	public void softDelete() {
		this.status = UserStatus.DELETED;
		this.deletedAt = Instant.now();
	}

	public void recordSuccessfulLogin() {
		this.lastLoginAt = Instant.now();
	}

	private static String normalizeEmail(String email) {
		return requireText(email, "email").toLowerCase(Locale.ROOT);
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value.trim();
	}
}
