package com.vdt.log_monitoring.api.realtime;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.vdt.log_monitoring.modules.identity.api.ApplicationAccessFacade;
import com.vdt.log_monitoring.shared.security.AuthenticatedUserPrincipal;
import com.vdt.log_monitoring.shared.security.JwtTokenProvider;

@Component
public class RealtimeWebSocketSecurityInterceptor implements ChannelInterceptor {

	private static final Pattern LIVE_LOG_DESTINATION = Pattern.compile(
		"^/topic/applications/([0-9a-fA-F-]{36})/logs$"
	);

	private final JwtTokenProvider jwtTokenProvider;
	private final ApplicationAccessFacade applicationAccessFacade;
	private final StringRedisTemplate redisTemplate;
	private final String accessTokenBlacklistPrefix;

	public RealtimeWebSocketSecurityInterceptor(
		JwtTokenProvider jwtTokenProvider,
		ApplicationAccessFacade applicationAccessFacade,
		ObjectProvider<StringRedisTemplate> redisTemplateProvider,
		@Value("${app.security.jwt.access-token.blacklist-prefix}") String accessTokenBlacklistPrefix
	) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.applicationAccessFacade = applicationAccessFacade;
		this.redisTemplate = redisTemplateProvider.getIfAvailable();
		this.accessTokenBlacklistPrefix = accessTokenBlacklistPrefix;
	}

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		if (accessor == null) {
			accessor = StompHeaderAccessor.wrap(message);
		}
		StompCommand command = accessor.getCommand();

		if (StompCommand.CONNECT.equals(command)) {
			authenticate(accessor);
		}

		if (StompCommand.SUBSCRIBE.equals(command)) {
			authorizeLiveLogSubscription(accessor);
		}

		return message;
	}

	private void authenticate(StompHeaderAccessor accessor) {
		String token = resolveBearerToken(accessor.getNativeHeader("Authorization"));
		if (!StringUtils.hasText(token)) {
			throw new BadCredentialsException("Missing WebSocket access token");
		}

		try {
			jwtTokenProvider.validateTokenOrThrow(token);
			if (isAccessTokenBlacklisted(token)) {
				throw new BadCredentialsException("WebSocket access token has been revoked");
			}

			AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
				UUID.fromString(jwtTokenProvider.getUserIdFromToken(token)),
				jwtTokenProvider.getEmailFromToken(token),
				jwtTokenProvider.getRoleFromToken(token),
				jwtTokenProvider.getDisplayNameFromToken(token)
			);

			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				principal,
				null,
				List.of(new SimpleGrantedAuthority("ROLE_" + principal.role()))
			);
			accessor.setUser(authentication);
		} catch (IllegalArgumentException | JwtException ex) {
			throw new BadCredentialsException("Invalid WebSocket access token", ex);
		}
	}

	private void authorizeLiveLogSubscription(StompHeaderAccessor accessor) {
		String destination = accessor.getDestination();
		if (!StringUtils.hasText(destination)) {
			throw new AccessDeniedException("Missing WebSocket subscription destination");
		}

		Matcher matcher = LIVE_LOG_DESTINATION.matcher(destination);
		if (!matcher.matches()) {
			return;
		}

		AuthenticatedUserPrincipal principal = currentPrincipal(accessor);
		UUID applicationId = UUID.fromString(matcher.group(1));
		if (!applicationAccessFacade.canViewApplication(principal.userId(), applicationId)) {
			throw new AccessDeniedException("User is not allowed to subscribe to this live log stream");
		}
	}

	private AuthenticatedUserPrincipal currentPrincipal(StompHeaderAccessor accessor) {
		if (accessor.getUser() instanceof UsernamePasswordAuthenticationToken authentication
			&& authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal) {
			return principal;
		}
		throw new AccessDeniedException("WebSocket subscription requires authentication");
	}

	private String resolveBearerToken(List<String> values) {
		if (values == null || values.isEmpty()) {
			return null;
		}

		String authorization = values.get(0);
		if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
			return authorization.substring(7);
		}
		return null;
	}

	private boolean isAccessTokenBlacklisted(String token) {
		if (redisTemplate == null) {
			return false;
		}
		return Boolean.TRUE.equals(redisTemplate.hasKey(accessTokenBlacklistPrefix + hashToken(token)));
	}

	private String hashToken(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 is not available", ex);
		}
	}
}
