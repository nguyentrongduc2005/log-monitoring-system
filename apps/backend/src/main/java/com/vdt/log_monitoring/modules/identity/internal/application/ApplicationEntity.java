package com.vdt.log_monitoring.modules.identity.internal.application;

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
	name = "applications",
	schema = "identity",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_applications_name", columnNames = "name")
	},
	indexes = {
		@Index(name = "idx_applications_status", columnList = "status")
	}
)
public class ApplicationEntity {

	@Id
	private UUID id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(name = "display_name", nullable = false, length = 150)
	private String displayName;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private ApplicationStatus status;

	@Column(name = "created_by", nullable = false)
	private UUID createdBy;

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

	public static ApplicationEntity create(
		String name,
		String displayName,
		String description,
		UUID createdBy
	) {
		Instant now = Instant.now();
		return new ApplicationEntity(
			UUID.randomUUID(),
			requireText(name, "name"),
			requireText(displayName, "displayName"),
			trimOptional(description),
			ApplicationStatus.ACTIVE,
			Objects.requireNonNull(createdBy, "createdBy must not be null"),
			now,
			now
		);
	}

	public static ApplicationEntity restore(
		UUID id,
		String name,
		String displayName,
		String description,
		ApplicationStatus status,
		UUID createdBy,
		Instant createdAt,
		Instant updatedAt
	) {
		return new ApplicationEntity(
			Objects.requireNonNull(id, "id must not be null"),
			requireText(name, "name"),
			requireText(displayName, "displayName"),
			trimOptional(description),
			Objects.requireNonNull(status, "status must not be null"),
			Objects.requireNonNull(createdBy, "createdBy must not be null"),
			Objects.requireNonNull(createdAt, "createdAt must not be null"),
			Objects.requireNonNull(updatedAt, "updatedAt must not be null")
		);
	}

	public void update(String name, String displayName, String description) {
		this.name = requireText(name, "name");
		this.displayName = requireText(displayName, "displayName");
		this.description = trimOptional(description);
	}

	public void changeStatus(ApplicationStatus status) {
		this.status = Objects.requireNonNull(status, "status must not be null");
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value.trim();
	}

	private static String trimOptional(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
