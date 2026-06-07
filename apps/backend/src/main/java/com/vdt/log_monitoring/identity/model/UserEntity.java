package com.vdt.log_monitoring.identity.model;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
	name = "users",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_users_email", columnNames = "email")
	},
	indexes = {
		@Index(name = "idx_users_status", columnList = "status")
	}
)
public class UserEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
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

	public UserEntity(
		String email,
		String passwordHash,
		String displayName,
		UserRole role
	) {
		this.email = normalizeEmail(email);
		this.passwordHash = requireText(passwordHash, "passwordHash");
		this.displayName = requireText(displayName, "displayName");
		this.role = Objects.requireNonNull(role, "role must not be null");
		this.status = UserStatus.ACTIVE;
	}

	@PrePersist
	void onCreate() {
		email = normalizeEmail(email);
		role = Objects.requireNonNull(role, "role must not be null");
		status = status == null ? UserStatus.ACTIVE : status;

		Instant now = Instant.now();
		createdAt = createdAt == null ? now : createdAt;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		email = normalizeEmail(email);
		updatedAt = Instant.now();
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
	}

	public void recordSuccessfulLogin(Instant loginAt) {
		this.lastLoginAt = Objects.requireNonNull(loginAt, "loginAt must not be null");
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
