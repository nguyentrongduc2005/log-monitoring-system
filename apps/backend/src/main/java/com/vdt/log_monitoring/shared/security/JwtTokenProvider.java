package com.vdt.log_monitoring.shared.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

	private final SecretKey key;
	private final long jwtExpirationInMs;

	public JwtTokenProvider(
		@Value("${app.security.jwt.secret}") String secret,
		@Value("${app.security.jwt.expiration-ms}") long jwtExpirationInMs
	) {
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.jwtExpirationInMs = jwtExpirationInMs;
	}

	public String generateToken(String email, String role, String displayName, String userId) {
		Map<String, Object> claims = new HashMap<>();
		claims.put("role", role);
		claims.put("displayName", displayName);
		claims.put("userId", userId);

		return Jwts.builder()
			.claims(claims)
			.subject(email)
			.issuedAt(new Date())
			.expiration(new Date(System.currentTimeMillis() + jwtExpirationInMs))
			.signWith(key)
			.compact();
	}

	public String getEmailFromToken(String token) {
		return getClaimFromToken(token, Claims::getSubject);
	}

	public String getRoleFromToken(String token) {
		return getClaimFromToken(token, claims -> claims.get("role", String.class));
	}

	public String getUserIdFromToken(String token) {
		return getClaimFromToken(token, claims -> claims.get("userId", String.class));
	}

	public String getDisplayNameFromToken(String token) {
		return getClaimFromToken(token, claims -> claims.get("displayName", String.class));
	}

	public Date getExpirationFromToken(String token) {
		return getClaimFromToken(token, Claims::getExpiration);
	}

	public long getRemainingValidityMillis(String token) {
		Date expiration = getExpirationFromToken(token);
		if (expiration == null) {
			return 0;
		}
		return Math.max(0, expiration.getTime() - System.currentTimeMillis());
	}

	public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = getAllClaimsFromToken(token);
		return claimsResolver.apply(claims);
	}

	private Claims getAllClaimsFromToken(String token) {
		return Jwts.parser()
			.verifyWith(key)
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}

	public boolean validateToken(String token) {
		try {
			validateTokenOrThrow(token);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public void validateTokenOrThrow(String token) {
		Claims claims = getAllClaimsFromToken(token);
		if (claims.getExpiration() == null) {
			throw new JwtException("JWT token is missing expiration");
		}
		if (claims.getExpiration().before(new Date())) {
			throw new ExpiredJwtException(null, claims, "JWT token is expired");
		}
	}
}
