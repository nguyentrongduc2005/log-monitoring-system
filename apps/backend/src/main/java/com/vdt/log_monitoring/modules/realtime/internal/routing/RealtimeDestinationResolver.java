package com.vdt.log_monitoring.modules.realtime.internal.routing;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.realtime.api.events.AlertNotificationMessage;
import com.vdt.log_monitoring.modules.realtime.api.events.AnomalyReportNotificationMessage;
import com.vdt.log_monitoring.modules.realtime.api.events.IncidentNotificationMessage;
import com.vdt.log_monitoring.modules.realtime.api.events.LiveLogMessage;

@Component
public class RealtimeDestinationResolver {

    public String liveLogsDestination(LiveLogMessage message) {
        return applicationDestination(message.applicationId(), "logs");
    }

    public String incidentNotificationsDestination(IncidentNotificationMessage message) {
        return "/topic/incidents";
    }

    public String alertNotificationsDestination(AlertNotificationMessage message) {
        return applicationDestination(message.applicationId(), "alerts");
    }

    public String anomalyReportNotificationsDestination(AnomalyReportNotificationMessage message) {
        return applicationDestination(message.applicationId(), "anomaly-reports");
    }

    private String applicationDestination(UUID applicationId, String stream) {
        return "/topic/applications/" + applicationId + "/" + stream;
    }
}
