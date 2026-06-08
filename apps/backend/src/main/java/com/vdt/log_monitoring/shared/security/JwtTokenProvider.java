package com.vdt.log_monitoring.shared.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

	private final SecretKey key;
	private final long jwtExpirationInMs;

	public JwtTokenProvider(
		@Value("${app.security.jwt.secret:default-secret-key-that-must-be-very-long-and-secure-for-hmac-sha-256}") String secret,
		@Value("${app.security.jwt.expiration-ms:86400000}") long jwtExpirationInMs
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
			Claims claims = getAllClaimsFromToken(token);
			return !claims.getExpiration().before(new Date());
		} catch (Exception e) {
			return false;
		}
	}
}
