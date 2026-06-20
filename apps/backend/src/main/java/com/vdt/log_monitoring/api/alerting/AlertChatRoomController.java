package com.vdt.log_monitoring.api.alerting;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vdt.log_monitoring.api.alerting.dto.ChatRoomResponse;
import com.vdt.log_monitoring.api.alerting.dto.ChatRoomStatusRequest;
import com.vdt.log_monitoring.api.alerting.dto.CreateChatRoomRequest;
import com.vdt.log_monitoring.api.alerting.dto.TelegramChatResponse;
import com.vdt.log_monitoring.modules.alerting.api.AlertingFacade;
import com.vdt.log_monitoring.modules.identity.api.IdentityFacade;
import com.vdt.log_monitoring.shared.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/alert-chat-rooms")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AlertChatRoomController {

	private final AlertingFacade alertingFacade;
	private final IdentityFacade identityFacade;

	@PostMapping
	public ResponseEntity<ApiResponse<ChatRoomResponse>> createChatRoom(
		Principal principal,
		@Valid @RequestBody CreateChatRoomRequest request
	) {
		IdentityFacade.UserDto user = identityFacade.findUserByEmail(principal.getName());
		AlertingFacade.ChatRoomDto chatRoom = alertingFacade.createChatRoom(
			new AlertingFacade.CreateChatRoomCommand(
				request.getChannel(),
				request.getName(),
				request.getChatId(),
				request.getDescription(),
				user.id()
			)
		);
		return ResponseEntity.ok(ApiResponse.success(ChatRoomResponse.from(chatRoom)));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<ChatRoomResponse>>> listChatRooms(
		@RequestParam(required = false) String channel,
		@RequestParam(required = false) String status
	) {
		List<ChatRoomResponse> chatRooms = alertingFacade.findChatRooms(channel, status).stream()
			.map(ChatRoomResponse::from)
			.toList();
		return ResponseEntity.ok(ApiResponse.success(chatRooms));
	}

	@GetMapping("/telegram/discover")
	public ResponseEntity<ApiResponse<List<TelegramChatResponse>>> discoverTelegramChats() {
		List<TelegramChatResponse> chats = alertingFacade.discoverTelegramChats().stream()
			.map(TelegramChatResponse::from)
			.toList();
		return ResponseEntity.ok(ApiResponse.success(chats));
	}

	@PutMapping("/{id}/status")
	public ResponseEntity<ApiResponse<ChatRoomResponse>> changeStatus(
		@PathVariable UUID id,
		@Valid @RequestBody ChatRoomStatusRequest request
	) {
		AlertingFacade.ChatRoomDto chatRoom = alertingFacade.changeChatRoomStatus(id, request.getStatus());
		return ResponseEntity.ok(ApiResponse.success(ChatRoomResponse.from(chatRoom)));
	}
}
