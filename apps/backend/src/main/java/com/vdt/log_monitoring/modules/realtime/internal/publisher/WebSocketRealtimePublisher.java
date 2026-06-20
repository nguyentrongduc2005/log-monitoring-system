package com.vdt.log_monitoring.modules.realtime.internal.publisher;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.realtime.api.RealtimeFacade;
import com.vdt.log_monitoring.modules.realtime.api.events.AlertNotificationMessage;
import com.vdt.log_monitoring.modules.realtime.api.events.IncidentNotificationMessage;
import com.vdt.log_monitoring.modules.realtime.api.events.LiveLogMessage;
import com.vdt.log_monitoring.modules.realtime.internal.routing.RealtimeDestinationResolver;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WebSocketRealtimePublisher implements RealtimeFacade {

    private final SimpMessagingTemplate messagingTemplate;
    private final RealtimeDestinationResolver destinationResolver;

    @Override
    public void publishLiveLog(LiveLogMessage message) {
        messagingTemplate.convertAndSend(destinationResolver.liveLogsDestination(message), message);
    }

    @Override
    public void publishIncidentNotification(IncidentNotificationMessage message) {
        messagingTemplate.convertAndSend(destinationResolver.incidentNotificationsDestination(message), message);
    }

    @Override
    public void publishAlertNotification(AlertNotificationMessage message) {
        messagingTemplate.convertAndSend(destinationResolver.alertNotificationsDestination(message), message);
    }

}
