package com.vdt.log_monitoring.modules.alerting.internal.notification;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertChannel;

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
	name = "chat_rooms",
	schema = "alerting",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_chat_rooms_channel_chat_id",
			columnNames = { "channel", "chat_id" }
		)
	},
	indexes = {
		@Index(name = "idx_chat_rooms_channel_status", columnList = "channel,status")
	}
)
public class ChatRoomEntity {

	@Id
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private AlertChannel channel;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(name = "chat_id", nullable = false, length = 128)
	private String chatId;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private ChatRoomStatus status;

	@Column(name = "created_by")
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

	public static ChatRoomEntity create(
		AlertChannel channel,
		String name,
		String chatId,
		String description,
		UUID createdBy
	) {
		Instant now = Instant.now();
		return new ChatRoomEntity(
			UUID.randomUUID(),
			Objects.requireNonNull(channel, "channel must not be null"),
			requireText(name, "name"),
			requireText(chatId, "chatId"),
			trimOptional(description),
			ChatRoomStatus.ACTIVE,
			createdBy,
			now,
			now
		);
	}

	public void changeStatus(ChatRoomStatus status) {
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
