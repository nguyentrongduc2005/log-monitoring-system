package com.vdt.log_monitoring.modules.incident.internal.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.vdt.log_monitoring.modules.incident.api.IncidentException;
import com.vdt.log_monitoring.modules.incident.internal.evidence.IncidentEvidenceCandidate;
import com.vdt.log_monitoring.modules.incident.internal.incident.AiConfidence;
import com.vdt.log_monitoring.modules.incident.internal.incident.IncidentSeverity;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.incident.ai", name = "provider", havingValue = "gemini")
public class GeminiIncidentAnalysisClient implements AiIncidentAnalysisClient {

	private static final int MAX_EVIDENCE_ITEMS = 30;
	private static final int MAX_MESSAGE_LENGTH = 700;
	private static final int MAX_OUTPUT_TOKENS = 8192;

	private final RestClient restClient;
	private final ObjectMapper objectMapper;
	private final IncidentAiProperties properties;

	public GeminiIncidentAnalysisClient(
		RestClient.Builder restClientBuilder,
		ObjectMapper objectMapper,
		IncidentAiProperties properties
	) {
		this.restClient = restClientBuilder
			.baseUrl(properties.apiBaseUrl())
			.build();
		this.objectMapper = objectMapper;
		this.properties = properties;
	}

	@Override
	public AiIncidentAnalysisResponse analyze(AiIncidentAnalysisRequest request) {
		if (properties.apiKey() == null || properties.apiKey().isBlank()) {
			throw analysisFailed("Gemini API key is not configured", null);
		}
		GeminiGenerateContentRequest geminiRequest = buildRequest(request);
		GeminiGenerateContentResponse response;
		try {
			response = restClient.post()
				.uri(uriBuilder -> uriBuilder
					.path("/v1beta/models/{model}:generateContent")
					.queryParam("key", properties.apiKey())
					.build(properties.model()))
				.contentType(MediaType.APPLICATION_JSON)
				.body(geminiRequest)
				.retrieve()
				.body(GeminiGenerateContentResponse.class);
		} catch (RestClientException exception) {
			throw analysisFailed("Gemini incident analysis request failed", exception);
		}
		String text = extractText(response);
		GeminiAnalysisPayload payload = parsePayload(text);
		return new AiIncidentAnalysisResponse(
			"gemini",
			properties.model(),
			"v1",
			payload.summary(),
			payload.likelyCause(),
			parseSeverity(payload.severity()),
			payload.severityReason(),
			parseConfidence(payload.confidence()),
			nullToEmpty(payload.suggestedActions()),
			nullToEmpty(payload.evidenceRefs()),
			toJson(payload));
	}

	private GeminiGenerateContentRequest buildRequest(AiIncidentAnalysisRequest request) {
		return new GeminiGenerateContentRequest(
			List.of(new GeminiContent("user", List.of(new GeminiPart(buildPrompt(request))))),
			new GeminiGenerationConfig(
				0.2,
				MAX_OUTPUT_TOKENS,
				"application/json",
				analysisResponseSchema()));
	}

	private String buildPrompt(AiIncidentAnalysisRequest request) {
		return """
			You are an incident investigation assistant for a log monitoring system.
			Analyze only the provided evidence. Do not invent services, deployments, metrics, or facts.
			Use the evidence kind in metadataJson when present to understand the context:
			- TRIGGER_ALERT is the user-selected alert that started the incident.
			- ANOMALY_EVIDENCE is the raw AI anomaly detection signal (if this is an anomaly incident). Focus on explaining what this anomaly means and suggest further investigation steps.
			- TRIGGER_FINGERPRINT summarizes the trigger fingerprint in the incident window.
			- TOP_ERROR_FINGERPRINT shows other high-volume error fingerprints grouped by log signatures. Use these to find the broader root cause.
			- TRACE_CONTEXT shows events in the same trace and may be absent.
			
			If ANOMALY_EVIDENCE is present, provide a quick investigation suggestion based on the anomaly signals.
			If TOP_ERROR_FINGERPRINT is present, synthesize the log groups to determine the root cause of the incident.
			Prefer timeline reasoning from firstSeenAt/lastSeenAt/occurredAt over raw count alone.
			Return strict JSON with these fields:
			{
			  "summary": string,
			  "likelyCause": string,
			  "severity": "SEV1" | "SEV2" | "SEV3" | "UNKNOWN",
			  "severityReason": string,
			  "confidence": "LOW" | "MEDIUM" | "HIGH",
			  "suggestedActions": string[],
			  "evidenceRefs": string[]
			}

			Incident:
			- id: %s
			- title: %s
			- description: %s
			- applicationIds: %s
			- windowStart: %s
			- windowEnd: %s

			Evidence:
			%s
			""".formatted(
			request.incidentId(),
			safe(request.title()),
			safe(request.description()),
			request.applicationIds(),
			request.windowStart(),
			request.windowEnd(),
			formatEvidence(request.evidence()));
	}

	private String formatEvidence(List<IncidentEvidenceCandidate> evidence) {
		if (evidence == null || evidence.isEmpty()) {
			return "[]";
		}
		List<Map<String, Object>> items = evidence.stream()
			.sorted(Comparator.comparing(
				IncidentEvidenceCandidate::occurredAt,
				Comparator.nullsLast(Comparator.naturalOrder())).reversed())
			.limit(MAX_EVIDENCE_ITEMS)
			.map(item -> Map.<String, Object>of(
				"type", item.type().name(),
				"sourceId", safe(item.sourceId()),
				"applicationId", String.valueOf(item.applicationId()),
				"fingerprint", safe(item.fingerprint()),
				"severity", safe(item.severity()),
				"summary", truncate(safe(item.summary()), MAX_MESSAGE_LENGTH),
				"sampleMessage", truncate(sanitize(safe(item.sampleMessage())), MAX_MESSAGE_LENGTH),
				"occurredAt", String.valueOf(item.occurredAt()),
				"metadataJson", safe(item.metadataJson())))
			.toList();
		return toJson(items);
	}

	private String extractText(GeminiGenerateContentResponse response) {
		if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
			throw analysisFailed("Gemini returned an empty analysis response", null);
		}
		GeminiCandidate candidate = response.candidates().getFirst();
		if ("MAX_TOKENS".equalsIgnoreCase(safe(candidate.finishReason()))) {
			throw analysisFailed("Gemini incident analysis response was truncated by the output token limit", null);
		}
		List<GeminiPart> parts = candidate.content() == null
			? List.of()
			: candidate.content().parts();
		if (parts == null || parts.isEmpty() || parts.getFirst().text() == null) {
			throw analysisFailed("Gemini returned an analysis response without text", null);
		}
		return parts.getFirst().text();
	}

	private GeminiAnalysisPayload parsePayload(String text) {
		try {
			return objectMapper.readValue(text, GeminiAnalysisPayload.class);
		} catch (JsonProcessingException exception) {
			log.warn("Failed to parse Gemini incident analysis JSON: {}", truncate(safe(text), 1_000), exception);
			throw analysisFailed("Gemini returned invalid analysis JSON", exception);
		}
	}

	private Map<String, Object> analysisResponseSchema() {
		return Map.of(
			"type", "OBJECT",
			"required", List.of(
				"summary",
				"likelyCause",
				"severity",
				"severityReason",
				"confidence",
				"suggestedActions",
				"evidenceRefs"),
			"properties", Map.of(
				"summary", stringSchema(),
				"likelyCause", stringSchema(),
				"severity", enumSchema("SEV1", "SEV2", "SEV3", "UNKNOWN"),
				"severityReason", stringSchema(),
				"confidence", enumSchema("LOW", "MEDIUM", "HIGH"),
				"suggestedActions", stringArraySchema(),
				"evidenceRefs", stringArraySchema()));
	}

	private Map<String, Object> stringSchema() {
		return Map.of("type", "STRING");
	}

	private Map<String, Object> enumSchema(String... values) {
		return Map.of("type", "STRING", "enum", List.of(values));
	}

	private Map<String, Object> stringArraySchema() {
		return Map.of("type", "ARRAY", "items", stringSchema());
	}

	private IncidentSeverity parseSeverity(String value) {
		if (value == null || value.isBlank()) {
			return IncidentSeverity.UNKNOWN;
		}
		try {
			return IncidentSeverity.valueOf(value.trim().toUpperCase());
		} catch (IllegalArgumentException exception) {
			return IncidentSeverity.UNKNOWN;
		}
	}

	private AiConfidence parseConfidence(String value) {
		if (value == null || value.isBlank()) {
			return AiConfidence.LOW;
		}
		try {
			return AiConfidence.valueOf(value.trim().toUpperCase());
		} catch (IllegalArgumentException exception) {
			return AiConfidence.LOW;
		}
	}

	private List<String> nullToEmpty(List<String> values) {
		return values == null ? List.of() : new ArrayList<>(values);
	}

	private String toJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Failed to serialize incident analysis JSON", exception);
		}
	}

	private String sanitize(String value) {
		return value
			.replaceAll("(?i)(password|passwd|pwd|token|api[_-]?key|secret|authorization)\\s*[:=]\\s*[^\\s,;]+", "$1=<redacted>")
			.replaceAll("(?i)(bearer)\\s+[a-z0-9._~+/=-]+", "$1 <redacted>");
	}

	private String truncate(String value, int maxLength) {
		if (value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength - 1) + "...";
	}

	private String safe(String value) {
		return value == null ? "" : value;
	}

	private IncidentException analysisFailed(String message, Exception cause) {
		IncidentException exception = new IncidentException(
			IncidentException.ErrorCode.INCIDENT_ANALYSIS_FAILED,
			message);
		if (cause != null) {
			exception.initCause(cause);
		}
		return exception;
	}

	private record GeminiGenerateContentRequest(
		List<GeminiContent> contents,
		GeminiGenerationConfig generationConfig
	) {}

	private record GeminiGenerationConfig(
		Double temperature,
		Integer maxOutputTokens,
		String responseMimeType,
		Map<String, Object> responseSchema
	) {}

	private record GeminiContent(String role, List<GeminiPart> parts) {}

	private record GeminiPart(String text) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record GeminiGenerateContentResponse(List<GeminiCandidate> candidates) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record GeminiCandidate(GeminiContent content, String finishReason) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record GeminiAnalysisPayload(
		String summary,
		String likelyCause,
		String severity,
		String severityReason,
		String confidence,
		List<String> suggestedActions,
		List<String> evidenceRefs
	) {}
}
