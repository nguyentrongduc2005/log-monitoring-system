package com.vdt.log_monitoring.modules.realtime.api;

import com.vdt.log_monitoring.modules.realtime.api.events.AlertNotificationMessage;
import com.vdt.log_monitoring.modules.realtime.api.events.AnomalyReportNotificationMessage;
import com.vdt.log_monitoring.modules.realtime.api.events.IncidentNotificationMessage;
import com.vdt.log_monitoring.modules.realtime.api.events.LiveLogMessage;

public interface RealtimeFacade {

    void publishLiveLog(LiveLogMessage message);

    void publishIncidentNotification(IncidentNotificationMessage message);

    void publishAlertNotification(AlertNotificationMessage message);

    void publishAnomalyReportNotification(AnomalyReportNotificationMessage message);
}
