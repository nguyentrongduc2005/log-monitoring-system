package com.vdt.log_monitoring.modules.identity.model;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User {

	private final UUID id;
	private String email;
	private String passwordHash;
	private String displayName;
	private UserRole role;
	private UserStatus status;
	private Instant lastLoginAt;
	private final Instant createdAt;
	private Instant updatedAt;

	public static User create(
		String email,
		String passwordHash,
		String displayName,
		UserRole role,
		Instant now
	) {
		Objects.requireNonNull(now, "now must not be null");

		return new User(
			UUID.randomUUID(),
			normalizeEmail(email),
			requireText(passwordHash, "passwordHash"),
			requireText(displayName, "displayName"),
			Objects.requireNonNull(role, "role must not be null"),
			UserStatus.ACTIVE,
			null,
			now,
			now
		);
	}

	public static User restore(
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
		return new User(
			Objects.requireNonNull(id, "id must not be null"),
			normalizeEmail(email),
			requireText(passwordHash, "passwordHash"),
			requireText(displayName, "displayName"),
			Objects.requireNonNull(role, "role must not be null"),
			Objects.requireNonNull(status, "status must not be null"),
			lastLoginAt,
			Objects.requireNonNull(createdAt, "createdAt must not be null"),
			Objects.requireNonNull(updatedAt, "updatedAt must not be null")
		);
	}

	public void updateProfile(String email, String displayName, Instant now) {
		this.email = normalizeEmail(email);
		this.displayName = requireText(displayName, "displayName");
		touch(now);
	}

	public void changePasswordHash(String passwordHash, Instant now) {
		this.passwordHash = requireText(passwordHash, "passwordHash");
		touch(now);
	}

	public void changeRole(UserRole role, Instant now) {
		this.role = Objects.requireNonNull(role, "role must not be null");
		touch(now);
	}

	public void changeStatus(UserStatus status, Instant now) {
		this.status = Objects.requireNonNull(status, "status must not be null");
		touch(now);
	}

	public void recordSuccessfulLogin(Instant loginAt) {
		this.lastLoginAt = Objects.requireNonNull(loginAt, "loginAt must not be null");
		this.updatedAt = loginAt;
	}

	private void touch(Instant now) {
		this.updatedAt = Objects.requireNonNull(now, "now must not be null");
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
