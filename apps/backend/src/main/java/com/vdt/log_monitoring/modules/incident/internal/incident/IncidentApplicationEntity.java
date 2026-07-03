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
@Table(name = "incident_applications", schema = "incident")
public class IncidentApplicationEntity {

	@EmbeddedId
	private IncidentApplicationId id;

	@ManyToOne(optional = false)
	@MapsId("incidentId")
	@JoinColumn(name = "incident_id", nullable = false)
	private IncidentEntity incident;

	@Enumerated(EnumType.STRING)
	@Column(name = "impact_role", nullable = false, length = 32)
	private ImpactRole impactRole;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	void onCreate() {
		createdAt = createdAt == null ? Instant.now() : createdAt;
	}

	static IncidentApplicationEntity create(IncidentEntity incident, UUID applicationId, ImpactRole impactRole) {
		Objects.requireNonNull(incident, "incident must not be null");
		Objects.requireNonNull(applicationId, "applicationId must not be null");
		return new IncidentApplicationEntity(
			new IncidentApplicationId(incident.getId(), applicationId),
			incident,
			Objects.requireNonNull(impactRole, "impactRole must not be null"),
			Instant.now());
	}

	public UUID getApplicationId() {
		return id.getApplicationId();
	}
}
