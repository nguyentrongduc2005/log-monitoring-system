package com.vdt.log_monitoring.modules.incident.internal;

import com.vdt.log_monitoring.modules.alerting.api.AnomalyReportTrigger;
import com.vdt.log_monitoring.modules.incident.api.IncidentFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class IncidentAnomalyReportTriggerImpl implements AnomalyReportTrigger {

    private final IncidentFacade incidentFacade;

    @Override
    public void triggerReport(UUID alertId, String evidencePayload) {
        incidentFacade.generateAnomalyReport(alertId, evidencePayload);
    }
}
