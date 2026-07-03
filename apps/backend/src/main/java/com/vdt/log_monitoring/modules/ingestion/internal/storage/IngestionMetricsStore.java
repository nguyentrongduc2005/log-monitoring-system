package com.vdt.log_monitoring.modules.ingestion.internal.storage;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IngestionMetricsStore {

	private final StringRedisTemplate redisTemplate;
	
	private static final String KEY_PREFIX = "ingestion:logs:count";
	private static final DateTimeFormatter MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm").withZone(ZoneOffset.UTC);

	/**
	 * Tăng số đếm log tiếp nhận.
	 * Lưu counter theo từng phút, set TTL 24h để tự xóa.
	 */
	public void increment(UUID applicationId, long count) {
		if (count <= 0) return;
		
		String minuteStr = MINUTE_FORMATTER.format(Instant.now());
		String key = String.format("%s:%s:%s", KEY_PREFIX, applicationId.toString(), minuteStr);
		
		redisTemplate.opsForValue().increment(key, count);
		redisTemplate.expire(key, Duration.ofHours(24));
	}

	/**
	 * Lấy số log tiếp nhận (nội suy cửa sổ trượt 60 giây).
	 */
	public long getLogsCountLastMinute(List<UUID> applicationIds) {
		if (applicationIds == null || applicationIds.isEmpty()) return 0;

		Instant now = Instant.now();
		String currentMinuteStr = MINUTE_FORMATTER.format(now);
		String previousMinuteStr = MINUTE_FORMATTER.format(now.minusSeconds(60));
		
		long total = 0;
		for (UUID appId : applicationIds) {
			String currentKey = String.format("%s:%s:%s", KEY_PREFIX, appId.toString(), currentMinuteStr);
			String prevKey = String.format("%s:%s:%s", KEY_PREFIX, appId.toString(), previousMinuteStr);
			
			String currentVal = redisTemplate.opsForValue().get(currentKey);
			String prevVal = redisTemplate.opsForValue().get(prevKey);
			
			long currentCount = currentVal != null ? Long.parseLong(currentVal) : 0;
			long prevCount = prevVal != null ? Long.parseLong(prevVal) : 0;
			
			// Tính nội suy: Mượt mà chuyển đổi giữa phút trước và phút này
			double weight = (60.0 - now.atZone(ZoneOffset.UTC).getSecond()) / 60.0;
			total += currentCount + (long)(prevCount * weight);
		}
		
		return total;
	}
}
