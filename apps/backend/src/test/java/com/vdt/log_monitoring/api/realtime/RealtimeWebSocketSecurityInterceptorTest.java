package com.vdt.log_monitoring.api.realtime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.vdt.log_monitoring.modules.identity.api.ApplicationAccessFacade;
import com.vdt.log_monitoring.shared.security.AuthenticatedUserPrincipal;
import com.vdt.log_monitoring.shared.security.JwtTokenProvider;

class RealtimeWebSocketSecurityInterceptorTest {

	private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID APPLICATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
	private static final UUID OTHER_APPLICATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000202");

	private JwtTokenProvider jwtTokenProvider;
	private ApplicationAccessFacade applicationAccessFacade;
	private RealtimeWebSocketSecurityInterceptor interceptor;

	@BeforeEach
	void setUp() {
		jwtTokenProvider = new JwtTokenProvider(
			"default-secret-key-that-must-be-very-long-and-secure-for-hmac-sha-256",
			86_400_000L
		);
		applicationAccessFacade = mock(ApplicationAccessFacade.class);
		@SuppressWarnings("unchecked")
		ObjectProvider<org.springframework.data.redis.core.StringRedisTemplate> redisProvider = mock(ObjectProvider.class);
		when(redisProvider.getIfAvailable()).thenReturn(null);
		interceptor = new RealtimeWebSocketSecurityInterceptor(
			jwtTokenProvider,
			applicationAccessFacade,
			redisProvider,
			"identity:access_token:blacklist:"
		);
	}

	@Test
	void connectRequiresBearerToken() {
		Message<byte[]> message = message(StompCommand.CONNECT, null, null, null);

		assertThatThrownBy(() -> interceptor.preSend(message, null))
			.isInstanceOf(BadCredentialsException.class)
			.hasMessage("Missing WebSocket access token");
	}

	@Test
	void connectRejectsInvalidBearerToken() {
		Message<byte[]> message = message(StompCommand.CONNECT, null, "Bearer not-a-jwt", null);

		assertThatThrownBy(() -> interceptor.preSend(message, null))
			.isInstanceOf(BadCredentialsException.class)
			.hasMessage("Invalid WebSocket access token");
	}

	@Test
	void subscribeRequiresAuthenticatedUserForLiveLogTopic() {
		Message<byte[]> message = message(
			StompCommand.SUBSCRIBE,
			"/topic/applications/" + APPLICATION_ID + "/logs",
			null,
			null
		);

		assertThatThrownBy(() -> interceptor.preSend(message, null))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessage("WebSocket subscription requires authentication");
	}

	@Test
	void subscribeAllowsAuthorizedLiveLogApplication() {
		when(applicationAccessFacade.canViewApplication(USER_ID, APPLICATION_ID)).thenReturn(true);
		Message<byte[]> message = message(
			StompCommand.SUBSCRIBE,
			"/topic/applications/" + APPLICATION_ID + "/logs",
			null,
			authenticatedUser()
		);

		assertThatCode(() -> interceptor.preSend(message, null)).doesNotThrowAnyException();
		verify(applicationAccessFacade).canViewApplication(USER_ID, APPLICATION_ID);
	}

	@Test
	void subscribeRejectsUnauthorizedLiveLogApplication() {
		when(applicationAccessFacade.canViewApplication(USER_ID, OTHER_APPLICATION_ID)).thenReturn(false);
		Message<byte[]> message = message(
			StompCommand.SUBSCRIBE,
			"/topic/applications/" + OTHER_APPLICATION_ID + "/logs",
			null,
			authenticatedUser()
		);

		assertThatThrownBy(() -> interceptor.preSend(message, null))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessage("User is not allowed to subscribe to this live log stream");
		verify(applicationAccessFacade).canViewApplication(USER_ID, OTHER_APPLICATION_ID);
	}

	@Test
	void subscribeIgnoresNonLiveLogTopics() {
		Message<byte[]> message = message(
			StompCommand.SUBSCRIBE,
			"/topic/alerts",
			null,
			null
		);

		assertThatCode(() -> interceptor.preSend(message, null)).doesNotThrowAnyException();
	}

	private Message<byte[]> message(
		StompCommand command,
		String destination,
		String authorization,
		UsernamePasswordAuthenticationToken user
	) {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
		if (destination != null) {
			accessor.setDestination(destination);
		}
		if (authorization != null) {
			accessor.setNativeHeader("Authorization", authorization);
		}
		if (user != null) {
			accessor.setUser(user);
		}
		return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
	}

	private UsernamePasswordAuthenticationToken authenticatedUser() {
		AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
			USER_ID,
			"engineer@example.com",
			"ENGINEER",
			"Engineer"
		);
		return new UsernamePasswordAuthenticationToken(principal, null, List.of());
	}
}
