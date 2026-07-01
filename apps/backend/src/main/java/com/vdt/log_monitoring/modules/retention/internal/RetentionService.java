package com.vdt.log_monitoring.modules.retention.internal;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

import com.vdt.log_monitoring.modules.retention.api.RetentionException;
import com.vdt.log_monitoring.modules.retention.api.RetentionFacade;

@Slf4j
@Service
public class RetentionService implements RetentionFacade {

	private final RetentionPolicyRepository policyRepository;
	private final RetentionRunRepository runRepository;
	private final ClickHouseRetentionRepository clickHouseRetentionRepository;
	private final Duration schedulerInterval;

	public RetentionService(
		RetentionPolicyRepository policyRepository,
		RetentionRunRepository runRepository,
		ClickHouseRetentionRepository clickHouseRetentionRepository,
		@Value("${app.retention.interval-ms:3600000}") long schedulerIntervalMs
	) {
		this.policyRepository = policyRepository;
		this.runRepository = runRepository;
		this.clickHouseRetentionRepository = clickHouseRetentionRepository;
		this.schedulerInterval = Duration.ofMillis(schedulerIntervalMs);
	}

	@Override
	@Transactional(readOnly = true)
	public List<RetentionPolicyDto> findPolicies() {
		return policyRepository.findAllByOrderBySortOrderAsc().stream()
			.map(this::toPolicyDto)
			.toList();
	}

	@Override
	@Transactional
	public List<RetentionPolicyDto> updatePolicies(List<UpdateRetentionPolicyCommand> commands) {
		if (commands == null || commands.isEmpty()) {
			throw new RetentionException(
				RetentionException.ErrorCode.INVALID_POLICY,
				"Retention policy updates must not be empty"
			);
		}

		Map<UUID, RetentionPolicyEntity> policiesById = policyRepository.findAllById(
				commands.stream().map(UpdateRetentionPolicyCommand::id).toList()
			).stream()
			.collect(Collectors.toMap(RetentionPolicyEntity::getId, Function.identity()));

		for (UpdateRetentionPolicyCommand command : commands) {
			RetentionPolicyEntity policy = policiesById.get(command.id());
			if (policy == null) {
				throw new RetentionException(
					RetentionException.ErrorCode.POLICY_NOT_FOUND,
					"Retention policy not found: " + command.id()
				);
			}
			policy.updateSettings(command.retentionDays(), command.enabled());
		}

		return policyRepository.findAllByOrderBySortOrderAsc().stream()
			.map(this::toPolicyDto)
			.toList();
	}

	@Override
	@Transactional
	public RetentionRunDto runPolicy(UUID policyId) {
		RetentionPolicyEntity policy = policyRepository.findById(policyId)
			.orElseThrow(() -> new RetentionException(
				RetentionException.ErrorCode.POLICY_NOT_FOUND,
				"Retention policy not found: " + policyId
			));
		return toRunDto(executeDeletePolicy(policy));
	}

	@Override
	public void runDuePolicies() {
		List<RetentionPolicyEntity> policies = policyRepository.findByEnabledTrueOrderBySortOrderAsc();
		for (RetentionPolicyEntity policy : policies) {
			try {
				executeDeletePolicy(policy);
			} catch (RuntimeException exception) {
				log.error("Retention policy {} failed", policy.getId(), exception);
			}
		}
	}

	@Transactional
	public RetentionRunEntity executeDeletePolicy(RetentionPolicyEntity policy) {
		Instant startedAt = Instant.now();
		Instant cutoff = startedAt.minus(Duration.ofDays(policy.getRetentionDays()));

		try {
			long affectedRows = clickHouseRetentionRepository.deleteExpiredLogs(policy.getLogLevel(), cutoff);
			RetentionRunEntity run = RetentionRunEntity.success(
				policy.getId(),
				startedAt,
				Instant.now(),
				affectedRows,
				"Deleted " + affectedRows + " expired " + policy.getLogLevel() + " logs older than " + cutoff + "."
			);
			return runRepository.save(run);
		} catch (RuntimeException exception) {
			RetentionRunEntity run = RetentionRunEntity.failed(
				policy.getId(),
				startedAt,
				Instant.now(),
				exception.getMessage()
			);
			runRepository.save(run);
			throw exception;
		}
	}

	private RetentionPolicyDto toPolicyDto(RetentionPolicyEntity policy) {
		RetentionRunDto recentOperation = runRepository.findFirstByPolicyIdOrderByStartedAtDesc(policy.getId())
			.map(this::toRunDto)
			.orElse(null);

		return new RetentionPolicyDto(
			policy.getId(),
			policy.getLogLevel(),
			policy.getLabel(),
			policy.getDescription(),
			policy.getRetentionDays(),
			policy.getMinDays(),
			policy.getMaxDays(),
			policy.isEnabled(),
			Instant.now().plus(schedulerInterval),
			recentOperation
		);
	}

	private RetentionRunDto toRunDto(RetentionRunEntity run) {
		return new RetentionRunDto(
			run.getId(),
			run.getPolicyId(),
			run.getStatus().name(),
			run.getStartedAt(),
			run.getFinishedAt(),
			run.getAffectedRows(),
			run.getMessage()
		);
	}
}
