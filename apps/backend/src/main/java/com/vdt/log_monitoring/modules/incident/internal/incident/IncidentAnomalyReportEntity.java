package com.vdt.log_monitoring.modules.incident.internal.incident;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "incident_anomaly_reports")
public class IncidentAnomalyReportEntity {

    @Id
    private UUID id;

    @Column(name = "alert_id", nullable = false)
    private UUID alertId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_payload", nullable = false)
    private String evidencePayload;

    @Column(name = "ai_analysis_result")
    private String aiAnalysisResult;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public IncidentAnomalyReportEntity() {
    }

    public IncidentAnomalyReportEntity(UUID id, UUID alertId, String evidencePayload, String status, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.alertId = alertId;
        this.evidencePayload = evidencePayload;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAlertId() {
        return alertId;
    }

    public String getEvidencePayload() {
        return evidencePayload;
    }

    public void setEvidencePayload(String evidencePayload) {
        this.evidencePayload = evidencePayload;
    }

    public String getAiAnalysisResult() {
        return aiAnalysisResult;
    }

    public void setAiAnalysisResult(String aiAnalysisResult) {
        this.aiAnalysisResult = aiAnalysisResult;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
