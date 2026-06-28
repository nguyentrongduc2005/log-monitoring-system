package com.vdt.log_monitoring.modules.anomaly.internal.log;

import com.vdt.log_monitoring.modules.anomaly.internal.rule.AnomalyLogRule;

public record AnomalyLogMatch(
	AnomalyLogRule rule,
	String dimensionType,
	String dimensionValue
) {}
