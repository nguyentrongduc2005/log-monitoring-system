package com.vdt.log_monitoring.modules.alerting.internal.notification;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertChannel;

public interface ChatRoomRepository extends JpaRepository<ChatRoomEntity, UUID> {

	List<ChatRoomEntity> findByChannelAndStatus(AlertChannel channel, ChatRoomStatus status);

	List<ChatRoomEntity> findByChannel(AlertChannel channel);

	List<ChatRoomEntity> findByStatus(ChatRoomStatus status);

	boolean existsByChannelAndChatId(AlertChannel channel, String chatId);
}
