import type { RetentionJob, RetentionJobDraft } from "./retention-types";

let jobs: RetentionJob[] = [
  {
    id: "ret-info",
    logLevel: "INFO",
    label: "INFO logs",
    description: "Routine application logs and request traces.",
    retentionDays: 30,
    action: "DELETE",
    minDays: 7,
    maxDays: 365,
    storageTb: 2.1,
    storagePercent: 45,
    projectedDeletionTbPerMonth: 0.72,
    compressionSavingsGbPerMonth: 120,
    nextRunAt: "2026-06-12T04:00:00Z",
    recentOperation: {
      status: "SUCCESS",
      message: "Purged 1.1TB of expired INFO logs from production cluster.",
      occurredAt: "2026-06-11T01:00:00Z"
    }
  },
  {
    id: "ret-warn",
    logLevel: "WARN",
    label: "WARN logs",
    description: "Potentially degraded behavior that still needs short-term review.",
    retentionDays: 90,
    action: "COMPRESS",
    minDays: 14,
    maxDays: 365,
    storageTb: 1.5,
    storagePercent: 32,
    projectedDeletionTbPerMonth: 0.18,
    compressionSavingsGbPerMonth: 240,
    nextRunAt: "2026-06-12T04:00:00Z",
    recentOperation: {
      status: "WARNING",
      message: "Compression ratio fell below 20% for api-gateway warnings.",
      occurredAt: "2026-06-10T22:00:00Z"
    }
  },
  {
    id: "ret-error",
    logLevel: "ERROR",
    label: "ERROR logs",
    description: "Application errors retained longer for incident investigation.",
    retentionDays: 180,
    action: "ARCHIVE",
    minDays: 30,
    maxDays: 365,
    storageTb: 0.6,
    storagePercent: 16,
    projectedDeletionTbPerMonth: 0.3,
    compressionSavingsGbPerMonth: 60,
    nextRunAt: "2026-06-12T04:00:00Z",
    recentOperation: {
      status: "SUCCESS",
      message: "Archived 240GB of ERROR logs to S3 coldline in us-east.",
      occurredAt: "2026-06-10T19:00:00Z"
    }
  },
  {
    id: "ret-critical",
    logLevel: "CRITICAL",
    label: "CRITICAL logs",
    description: "High-severity failures kept longest for root cause analysis.",
    retentionDays: 365,
    action: "ARCHIVE",
    minDays: 90,
    maxDays: 730,
    storageTb: 0.3,
    storagePercent: 7,
    projectedDeletionTbPerMonth: 0.05,
    compressionSavingsGbPerMonth: 30,
    nextRunAt: "2026-06-12T04:00:00Z",
    recentOperation: {
      status: "SUCCESS",
      message: "Archived 80GB of CRITICAL logs for long-term incident review.",
      occurredAt: "2026-06-10T18:30:00Z"
    }
  }
];

function delay() {
  return new Promise(resolve => window.setTimeout(resolve, 120));
}

export async function getRetentionJobs() {
  await delay();
  return jobs.map(job => ({ ...job, recentOperation: { ...job.recentOperation } }));
}

export async function saveRetentionJobs(drafts: RetentionJobDraft[]) {
  await delay();
  const draftById = new Map(drafts.map(draft => [draft.id, draft]));

  jobs = jobs.map(job => {
    const draft = draftById.get(job.id);
    if (!draft) {
      return job;
    }

    return {
      ...job,
      retentionDays: draft.retentionDays,
      action: draft.action,
      recentOperation: {
        status: "SUCCESS",
        message: `Updated ${job.logLevel} retention to ${draft.retentionDays} days with ${draft.action.toLowerCase()} action.`,
        occurredAt: new Date().toISOString()
      }
    };
  });

  return getRetentionJobs();
}
