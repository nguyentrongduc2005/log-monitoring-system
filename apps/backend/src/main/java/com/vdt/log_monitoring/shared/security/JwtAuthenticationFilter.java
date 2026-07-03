package com.vdt.log_monitoring.shared.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HexFormat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.vdt.log_monitoring.shared.dto.ApiResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenProvider jwtTokenProvider;
	private final ObjectMapper objectMapper;
	private final StringRedisTemplate redisTemplate;
	private final String accessTokenBlacklistPrefix;

	public JwtAuthenticationFilter(
			JwtTokenProvider jwtTokenProvider,
			ObjectMapper objectMapper,
			ObjectProvider<StringRedisTemplate> redisTemplateProvider,
			@Value("${app.security.jwt.access-token.blacklist-prefix}") String accessTokenBlacklistPrefix
	) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.objectMapper = objectMapper;
		this.redisTemplate = redisTemplateProvider.getIfAvailable();
		this.accessTokenBlacklistPrefix = accessTokenBlacklistPrefix;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		try {
			String jwt = getJwtFromRequest(request);

			if (StringUtils.hasText(jwt)) {
				jwtTokenProvider.validateTokenOrThrow(jwt);
				if (isAccessTokenBlacklisted(jwt)) {
					writeAuthenticationError(response, "Access token has been revoked", "ACCESS_TOKEN_REVOKED");
					return;
				}
				String email = jwtTokenProvider.getEmailFromToken(jwt);
				String role = jwtTokenProvider.getRoleFromToken(jwt);

				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
					email,
					null,
					Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
				);
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
		} catch (ExpiredJwtException ex) {
			writeAuthenticationError(response, "JWT token has expired", "JWT_EXPIRED");
			return;
		} catch (JwtException | IllegalArgumentException ex) {
			writeAuthenticationError(response, "Invalid JWT token", "INVALID_JWT");
			return;
		} catch (Exception ex) {
			logger.error("Could not set user authentication in security context", ex);
			writeAuthenticationError(response, "JWT authentication failed", "JWT_AUTHENTICATION_FAILED");
			return;
		}

		filterChain.doFilter(request, response);
	}

	private String getJwtFromRequest(HttpServletRequest request) {
		String bearerToken = request.getHeader("Authorization");
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
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

	private void writeAuthenticationError(HttpServletResponse response, String message, String errorCode)
			throws IOException {
		SecurityContextHolder.clearContext();
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		ApiResponse<String> body = ApiResponse.<String>builder()
			.success(false)
			.message(message)
			.data(errorCode)
			.build();
		objectMapper.writeValue(response.getOutputStream(), body);
	}
}
