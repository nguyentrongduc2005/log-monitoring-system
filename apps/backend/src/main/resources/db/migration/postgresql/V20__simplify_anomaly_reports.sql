DROP INDEX IF EXISTS anomaly.idx_anomaly_reports_dimension;

ALTER TABLE anomaly.anomaly_reports
    DROP COLUMN IF EXISTS likelihood_label,
    DROP COLUMN IF EXISTS impact_summary,
    DROP COLUMN IF EXISTS investigation_steps,
    DROP COLUMN IF EXISTS recommended_actions,
    DROP COLUMN IF EXISTS dimension_type,
    DROP COLUMN IF EXISTS dimension_value,
    DROP COLUMN IF EXISTS metric_group,
    DROP COLUMN IF EXISTS observed_value,
    DROP COLUMN IF EXISTS threshold_value,
    DROP COLUMN IF EXISTS observed_count,
    DROP COLUMN IF EXISTS threshold_count,
    DROP COLUMN IF EXISTS ai_model,
    DROP COLUMN IF EXISTS ai_prompt_version,
    DROP COLUMN IF EXISTS ai_summary,
    DROP COLUMN IF EXISTS ai_confidence_score,
    DROP COLUMN IF EXISTS ai_likelihood_label,
    DROP COLUMN IF EXISTS ai_root_cause_candidates,
    DROP COLUMN IF EXISTS ai_recommended_actions,
    DROP COLUMN IF EXISTS ai_investigation_steps,
    DROP COLUMN IF EXISTS ai_raw_response;
