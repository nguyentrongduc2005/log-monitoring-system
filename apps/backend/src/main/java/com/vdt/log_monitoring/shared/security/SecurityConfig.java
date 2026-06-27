package com.vdt.log_monitoring.shared.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	private static final String[] PUBLIC_ENDPOINTS = {
		"/api/v1/auth/**",
		"/ws",
		"/v3/api-docs/**",
		"/swagger-ui/**",
		"/swagger-ui.html",
		"/internal/prometheus/**"
	};

	private static final String[] PUBLIC_LOG_INGESTION_ENDPOINTS = {
		"/api/v1/logs",
		"/api/v1/logs/batch"
	};

	private static final String[] PRIVATE_ENDPOINTS = {
		"/api/v1/users/me",
		"/api/v1/users/me/**",
		"/api/v1/applications/me"
	};

	private static final String[] ADMIN_ENDPOINTS = {
		"/api/v1/users/**",
		"/api/v1/applications/**"
	};

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final List<String> allowedOriginPatterns;

	public SecurityConfig(
		JwtAuthenticationFilter jwtAuthenticationFilter,
		@Value("${app.security.cors.allowed-origin-patterns}") List<String> allowedOriginPatterns
	) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.allowedOriginPatterns = allowedOriginPatterns;
	}

	@Bean
	public PasswordEncoder passwordEncoder(@Value("${app.security.password.bcrypt-strength}") int bcryptStrength) {
		return new BCryptPasswordEncoder(bcryptStrength);
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.csrf(csrf -> csrf.disable())
			.cors(cors -> cors.configure(http))
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(PUBLIC_ENDPOINTS).permitAll()
				.requestMatchers(HttpMethod.POST, PUBLIC_LOG_INGESTION_ENDPOINTS).permitAll()
				.requestMatchers(PRIVATE_ENDPOINTS).authenticated()
				.requestMatchers(ADMIN_ENDPOINTS).hasRole("ADMIN")
				.anyRequest().authenticated()
			)
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public CorsFilter corsFilter() {
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowCredentials(true);
		config.setAllowedOriginPatterns(allowedOriginPatterns);
		config.addAllowedHeader("*");
		config.addAllowedMethod("*");
		source.registerCorsConfiguration("/**", config);
		return new CorsFilter(source);
	}
}
