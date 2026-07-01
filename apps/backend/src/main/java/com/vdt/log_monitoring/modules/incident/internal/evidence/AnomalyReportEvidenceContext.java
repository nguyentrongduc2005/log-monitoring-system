package com.vdt.log_monitoring.modules.incident.internal.evidence;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt.log_monitoring.modules.anomaly.api.AnomalyFacade;

public record AnomalyReportEvidenceContext(
	List<String> traceIds,
	List<String> fingerprints,
	List<LogSample> logSamples,
	Long observedCount,
	Long thresholdCount,
	String dimensionType,
	String dimensionValue
) {

	public static AnomalyReportEvidenceContext from(AnomalyFacade.AnomalyReportDto report, ObjectMapper objectMapper) {
		if (report == null || report.evidencePayloadJson() == null || report.evidencePayloadJson().isBlank()) {
			return empty();
		}
		try {
			JsonNode root = objectMapper.readTree(report.evidencePayloadJson());
			return new AnomalyReportEvidenceContext(
				textArray(root.path("traceIds")),
				textArray(root.path("fingerprints")),
				logSamples(root.path("logSamples")),
				longValue(root.path("observedCount")),
				longValue(root.path("thresholdCount")),
				textValue(root.path("dimensionType")),
				textValue(root.path("dimensionValue")));
		} catch (Exception exception) {
			return empty();
		}
	}

	public String firstSampleMessage() {
		return logSamples.stream()
			.map(LogSample::message)
			.filter(value -> value != null && !value.isBlank())
			.findFirst()
			.orElse("");
	}

	public String countSummary() {
		if (observedCount == null || thresholdCount == null) {
			return "";
		}
		return observedCount + " matching events exceeded threshold " + thresholdCount;
	}

	private static AnomalyReportEvidenceContext empty() {
		return new AnomalyReportEvidenceContext(List.of(), List.of(), List.of(), null, null, "", "");
	}

	private static List<String> textArray(JsonNode node) {
		if (node == null || !node.isArray()) {
			return List.of();
		}
		List<String> values = new ArrayList<>();
		node.forEach(item -> {
			String value = textValue(item);
			if (!value.isBlank()) {
				values.add(value);
			}
		});
		return values.stream().distinct().limit(20).toList();
	}

	private static List<LogSample> logSamples(JsonNode node) {
		if (node == null || !node.isArray()) {
			return List.of();
		}
		List<LogSample> values = new ArrayList<>();
		node.forEach(item -> {
			if (item == null || !item.isObject()) {
				return;
			}
			String message = textValue(item.path("message"));
			if (!message.isBlank()) {
				values.add(new LogSample(textValue(item.path("level")), message));
			}
		});
		return values.stream().limit(10).toList();
	}

	private static String textValue(JsonNode node) {
		return node == null || node.isMissingNode() || node.isNull() ? "" : node.asText("").trim();
	}

	private static Long longValue(JsonNode node) {
		return node == null || node.isMissingNode() || node.isNull() ? null : node.asLong();
	}

	public record LogSample(String level, String message) {}
}
