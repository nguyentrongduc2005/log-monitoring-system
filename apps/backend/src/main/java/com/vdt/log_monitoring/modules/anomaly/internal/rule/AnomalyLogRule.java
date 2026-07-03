package com.vdt.log_monitoring.modules.anomaly.internal.rule;

import java.time.Duration;
import java.util.List;

public enum AnomalyLogRule {
	RESOURCE_EXHAUSTED(
			Duration.ofMinutes(5),
			List.of("out of memory", "oom", "heap space", "disk full", "no space left"),
			List.of("host", "service")),
	SECURITY_AUTH_FAILURE(
			Duration.ofMinutes(5),
			List.of("failed login", "login failed", "authentication failed", "bad credentials",
					"invalid password", "invalid token", "ssh failed", "failed password"),
			List.of("user", "account", "ip", "service")),
	ACCESS_DENIED(
			Duration.ofMinutes(5),
			List.of("denied", "forbidden", "unauthorized", "permission denied"),
			List.of("user", "account", "ip", "service")),
	TIMEOUT(
			Duration.ofMinutes(2),
			List.of("timeout", "timed out", "deadline exceeded", "read timeout", "connection timeout"),
			List.of("service", "host")),
	EXCEPTION(
			Duration.ofMinutes(2),
			List.of("exception", "stacktrace", "nullpointer", "illegalstate", "runtimeexception"),
			List.of("service")),
	CRITICAL_LEVEL(
			Duration.ofMinutes(5),
			List.of(),
			List.of("service")),
	ERROR_LEVEL(
			Duration.ofMinutes(1),
			List.of(),
			List.of("service")),
	SUSPICIOUS_KEYWORD(
			Duration.ofMinutes(5),
			List.of("failed", "failure", "degraded", "unavailable", "retry exhausted", "circuit breaker open"),
			List.of("service"));

	private static final Duration GRACE = Duration.ofSeconds(60);

	private final Duration window;
	private final List<String> keywords;
	private final List<String> dimensionPriority;

	AnomalyLogRule(
			Duration window,
			List<String> keywords,
			List<String> dimensionPriority) {
		this.window = window;
		this.keywords = keywords;
		this.dimensionPriority = dimensionPriority;
	}

	public Duration window() {
		return window;
	}

	public Duration ttl() {
		return window.plus(GRACE);
	}

	public List<String> dimensionPriority() {
		return dimensionPriority;
	}

	public boolean matches(String level, String normalizedMessage) {
		return switch (this) {
			case CRITICAL_LEVEL -> "CRITICAL".equalsIgnoreCase(level);
			case ERROR_LEVEL -> "ERROR".equalsIgnoreCase(level);
			default -> keywords.stream().anyMatch(normalizedMessage::contains);
		};
	}

	public long thresholdCount() {
		return switch (this) {
			case RESOURCE_EXHAUSTED, CRITICAL_LEVEL -> 1;
			case SECURITY_AUTH_FAILURE, ACCESS_DENIED -> 5;
			case TIMEOUT, EXCEPTION -> 10;
			case ERROR_LEVEL -> 15;
			case SUSPICIOUS_KEYWORD -> 20;
		};
	}

	public boolean isBreached(long count) {
		return count >= thresholdCount();
	}

	public Duration cooldown() {
		return switch (this) {
			case SECURITY_AUTH_FAILURE, ACCESS_DENIED -> Duration.ofMinutes(10);
			case RESOURCE_EXHAUSTED, CRITICAL_LEVEL -> Duration.ofMinutes(5);
			default -> Duration.ofMinutes(3);
		};
	}

	public String severityFor(long count) {
		return switch (this) {
			case RESOURCE_EXHAUSTED, CRITICAL_LEVEL -> "CRITICAL";
			case SECURITY_AUTH_FAILURE, ACCESS_DENIED ->
				count >= thresholdCount() * 3 ? "CRITICAL" : "ERROR";
			case TIMEOUT, EXCEPTION, ERROR_LEVEL ->
				count >= thresholdCount() * 2 ? "CRITICAL" : "ERROR";
			case SUSPICIOUS_KEYWORD -> "WARN";
		};
	}

	public boolean shouldTriggerAi(long count, long repeatedDedupCount) {
		return "CRITICAL".equals(severityFor(count))
				|| count >= thresholdCount() * 3
				|| repeatedDedupCount >= 2
				|| this == SECURITY_AUTH_FAILURE
				|| this == RESOURCE_EXHAUSTED;
	}

	public double confidenceScore(long count) {
		double ratio = thresholdCount() <= 0 ? 1.0 : (double) count / thresholdCount();
		return Math.min(0.98, 0.45 + (ratio * 0.15));
	}

	public String hypothesis(String dimensionType, String dimensionValue) {
		return switch (this) {
			case SECURITY_AUTH_FAILURE ->
				"Có thể đang xảy ra tấn công brute-force, dò mật khẩu hoặc lạm dụng xác thực đối với "
						+ dimensionType + " " + dimensionValue + ".";
			case ACCESS_DENIED -> "Có thể có vấn đề về phân quyền hoặc kiểm soát truy cập đối với "
					+ dimensionType + " " + dimensionValue + ".";
			case RESOURCE_EXHAUSTED -> "Có thể đang xảy ra tình trạng cạn kiệt tài nguyên đối với "
					+ dimensionType + " " + dimensionValue + ".";
			case TIMEOUT -> "Có thể có độ trễ cao hoặc lỗi timeout từ dịch vụ phụ thuộc đối với "
					+ dimensionType + " " + dimensionValue + ".";
			case EXCEPTION -> "Phát hiện mẫu ngoại lệ (exception) lặp lại đối với "
					+ dimensionType + " " + dimensionValue + ".";
			case CRITICAL_LEVEL ->
				"Các log cấp độ nghiêm trọng (CRITICAL) cho thấy tình trạng lỗi nặng đang xảy ra đối với "
						+ dimensionType + " " + dimensionValue + ".";
			case ERROR_LEVEL -> "Số lượng lỗi (ERROR) vượt quá ngưỡng bình thường đối với "
					+ dimensionType + " " + dimensionValue + ".";
			case SUSPICIOUS_KEYWORD -> "Phát hiện các từ khóa nghi ngờ lỗi/suy giảm hiệu năng lặp lại đối với "
					+ dimensionType + " " + dimensionValue + ".";
		};
	}

	public List<String> recommendedActions() {
		return switch (this) {
			case SECURITY_AUTH_FAILURE -> List.of(
					"Kiểm tra các lần đăng nhập thành công và thất bại của cùng một tài khoản hoặc IP.",
					"Xem xét độ tin cậy của IP nguồn và các hoạt động gần đây của tài khoản.",
					"Áp dụng thử thách MFA, giới hạn tốc độ (rate limiting) hoặc khóa tạm thời nếu tiếp tục có cảnh báo.");
			case RESOURCE_EXHAUSTED -> List.of(
					"Kiểm tra các chỉ số CPU, bộ nhớ, đĩa cứng và tần suất khởi động lại container.",
					"Kiểm tra các bản triển khai gần đây và sự gia tăng đột biến của lưu lượng truy cập.",
					"Mở rộng quy mô (scale) hoặc khởi động lại dịch vụ bị ảnh hưởng nếu tình trạng quá tải tiếp diễn.");
			case ACCESS_DENIED -> List.of(
					"Kiểm tra các thay đổi gần đây về quyền, role hoặc token.",
					"Xác nhận xem việc từ chối truy cập này có nằm trong dự tính đối với người dùng/dịch vụ này không.",
					"Kiểm tra các log kiểm toán (audit logs) liên quan trong cùng khoảng thời gian.");
			case TIMEOUT -> List.of(
					"Kiểm tra tình trạng sức khỏe và độ trễ của các dịch vụ phụ thuộc.",
					"Kiểm tra request traces (dấu vết yêu cầu) của dịch vụ bị ảnh hưởng.",
					"Xem xét lại cấu hình retry, circuit breaker và timeout.");
			default -> List.of(
					"Kiểm tra các log liên quan trong khoảng thời gian xảy ra bất thường.",
					"So sánh với các thay đổi về triển khai, lưu lượng và số liệu (metrics).",
					"Leo thang sự cố (escalate) nếu biểu hiện lỗi tiếp diễn hoặc ảnh hưởng đến người dùng.");
		};
	}
}
