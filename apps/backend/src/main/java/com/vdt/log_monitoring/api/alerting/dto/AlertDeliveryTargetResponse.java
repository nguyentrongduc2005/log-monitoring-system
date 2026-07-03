package com.vdt.log_monitoring.api.alerting.dto;

import java.util.UUID;

import com.vdt.log_monitoring.modules.alerting.api.AlertingFacade;

public record AlertDeliveryTargetResponse(
	String channel,
	UUID chatRoomId
) {

	public static AlertDeliveryTargetResponse from(AlertingFacade.AlertDeliveryTargetDto target) {
		return new AlertDeliveryTargetResponse(target.channel(), target.chatRoomId());
	}
}
