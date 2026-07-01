import { useEffect, useMemo, useState } from "react";
import { PageHeader } from "@/shared/layouts/page-header-context";
import { getRetentionJobs, saveRetentionJobs } from "./retention-adapter";
import type { RetentionJob, RetentionJobDraft } from "./retention-types";
import RetentionControls from "./components/RetentionControls";
import RetentionInsights from "./components/RetentionInsights";
import RetentionMetrics from "./components/RetentionMetrics";

function toDrafts(jobs: RetentionJob[]): RetentionJobDraft[] {
  return jobs.map(job => ({
    id: job.id,
    retentionDays: job.retentionDays,
    enabled: job.enabled
  }));
}

export function Component() {
  const [jobs, setJobs] = useState<RetentionJob[]>([]);
  const [drafts, setDrafts] = useState<RetentionJobDraft[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function loadJobs() {
    setLoading(true);
    setError(null);
    try {
      const loadedJobs = await getRetentionJobs();
      setJobs(loadedJobs);
      setDrafts(toDrafts(loadedJobs));
    } catch {
      setError("Unable to load retention jobs.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    queueMicrotask(() => {
      void loadJobs();
    });
  }, []);

  const previewJobs = useMemo(
    () =>
      jobs.map(job => {
        const draft = drafts.find(item => item.id === job.id);
        return draft ? { ...job, ...draft } : job;
      }),
    [drafts, jobs]
  );

  function updateDraft(nextDraft: RetentionJobDraft) {
    setDrafts(current =>
      current.map(draft => (draft.id === nextDraft.id ? nextDraft : draft))
    );
  }

  async function saveChanges() {
    setSaving(true);
    setError(null);
    try {
      const savedJobs = await saveRetentionJobs(drafts);
      setJobs(savedJobs);
      setDrafts(toDrafts(savedJobs));
    } catch {
      setError("Unable to save retention settings.");
    } finally {
      setSaving(false);
    }
  }

  function resetChanges() {
    setDrafts(toDrafts(jobs));
  }

  return (
    <div className="space-y-5">
      <PageHeader title="Retention Policy" />

      <p className="text-sm text-muted">
        Manage fixed log retention policies. Expired logs are deleted from ClickHouse by level.
      </p>

      <div className="grid gap-5 xl:grid-cols-[minmax(0,1fr)_22rem]">
        <div className="space-y-3">
          <RetentionControls
            drafts={drafts}
            error={error}
            jobs={jobs}
            loading={loading}
            onReset={resetChanges}
            onSave={() => void saveChanges()}
            onUpdateDraft={updateDraft}
            saving={saving}
          />
          <RetentionMetrics jobs={previewJobs} />
        </div>

        <RetentionInsights jobs={previewJobs} />
      </div>
    </div>
  );
}
