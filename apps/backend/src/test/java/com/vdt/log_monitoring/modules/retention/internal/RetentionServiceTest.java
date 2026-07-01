package com.vdt.log_monitoring.modules.retention.internal;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vdt.log_monitoring.modules.retention.api.RetentionFacade;

@ExtendWith(MockitoExtension.class)
class RetentionServiceTest {

	private static final UUID INFO_POLICY_ID = UUID.fromString("00000000-0000-0000-0000-000000000501");
	private static final Instant NOW = Instant.parse("2026-07-01T00:00:00Z");

	@Mock
	private RetentionPolicyRepository policyRepository;

	@Mock
	private RetentionRunRepository runRepository;

	@Mock
	private ClickHouseRetentionRepository clickHouseRetentionRepository;

	@Test
	void updatesExistingFixedPolicySettings() {
		RetentionPolicyEntity policy = infoPolicy(7, true);
		when(policyRepository.findAllById(List.of(INFO_POLICY_ID))).thenReturn(List.of(policy));
		when(policyRepository.findAllByOrderBySortOrderAsc()).thenReturn(List.of(policy));
		when(runRepository.findFirstByPolicyIdOrderByStartedAtDesc(INFO_POLICY_ID)).thenReturn(Optional.empty());

		RetentionService service = new RetentionService(
			policyRepository,
			runRepository,
			clickHouseRetentionRepository,
			3_600_000
		);

		service.updatePolicies(List.of(
			new RetentionFacade.UpdateRetentionPolicyCommand(INFO_POLICY_ID, 14, false)
		));

		org.assertj.core.api.Assertions.assertThat(policy.getRetentionDays()).isEqualTo(14);
		org.assertj.core.api.Assertions.assertThat(policy.isEnabled()).isFalse();
	}

	@Test
	void scheduledRunLoadsEnabledPoliciesAndDeletesExpiredLogs() {
		RetentionPolicyEntity policy = infoPolicy(14, true);
		when(policyRepository.findByEnabledTrueOrderBySortOrderAsc()).thenReturn(List.of(policy));
		when(clickHouseRetentionRepository.deleteExpiredLogs(
			org.mockito.ArgumentMatchers.eq("INFO"),
			org.mockito.ArgumentMatchers.any()
		)).thenReturn(120L);
		when(runRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

		RetentionService service = new RetentionService(
			policyRepository,
			runRepository,
			clickHouseRetentionRepository,
			3_600_000
		);

		service.runDuePolicies();

		ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
		verify(clickHouseRetentionRepository).deleteExpiredLogs(
			org.mockito.ArgumentMatchers.eq("INFO"),
			cutoffCaptor.capture()
		);
		org.assertj.core.api.Assertions.assertThat(cutoffCaptor.getValue()).isBefore(Instant.now().minusSeconds(13 * 86_400));
		verify(runRepository).save(org.mockito.ArgumentMatchers.argThat(run ->
			run.getStatus() == RetentionRunStatus.SUCCESS && run.getAffectedRows() == 120L
		));
	}

	private static RetentionPolicyEntity infoPolicy(int retentionDays, boolean enabled) {
		return RetentionPolicyEntity.existing(
			INFO_POLICY_ID,
			"INFO",
			"INFO logs",
			"Routine application logs and request traces.",
			retentionDays,
			1,
			365,
			enabled,
			10,
			NOW,
			NOW
		);
	}
}
