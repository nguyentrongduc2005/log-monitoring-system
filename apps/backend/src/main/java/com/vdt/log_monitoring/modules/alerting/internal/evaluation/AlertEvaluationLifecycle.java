package com.vdt.log_monitoring.modules.alerting.internal.evaluation;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class AlertEvaluationLifecycle {

	public void afterCommit(Runnable callback) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			callback.run();
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				callback.run();
			}
		});
	}

	public void afterRollback(Runnable callback) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int status) {
				if (status == STATUS_ROLLED_BACK) {
					callback.run();
				}
			}
		});
	}
}
