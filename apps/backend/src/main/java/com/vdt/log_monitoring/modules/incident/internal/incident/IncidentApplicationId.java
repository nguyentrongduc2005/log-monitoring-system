package com.vdt.log_monitoring.modules.incident.internal.incident;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class IncidentApplicationId implements Serializable {

	@Column(name = "incident_id", nullable = false)
	private UUID incidentId;

	@Column(name = "application_id", nullable = false)
	private UUID applicationId;
}
