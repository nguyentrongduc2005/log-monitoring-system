package com.vdt.log_monitoring.modules.incident.internal.incident;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "incident_alerts", schema = "incident")
public class IncidentAlertEntity {

	@EmbeddedId
	private IncidentAlertId id;

	@ManyToOne(optional = false)
	@MapsId("incidentId")
	@JoinColumn(name = "incident_id", nullable = false)
	private IncidentEntity incident;

	@Enumerated(EnumType.STRING)
	@Column(name = "relation_type", nullable = false, length = 32)
	private AlertRelationType relationType;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	void onCreate() {
		createdAt = createdAt == null ? Instant.now() : createdAt;
	}

	static IncidentAlertEntity create(IncidentEntity incident, UUID alertId, AlertRelationType relationType) {
		Objects.requireNonNull(incident, "incident must not be null");
		Objects.requireNonNull(alertId, "alertId must not be null");
		return new IncidentAlertEntity(
			new IncidentAlertId(incident.getId(), alertId),
			incident,
			Objects.requireNonNull(relationType, "relationType must not be null"),
			Instant.now());
	}

	public UUID getAlertId() {
		return id.getAlertId();
	}
}
