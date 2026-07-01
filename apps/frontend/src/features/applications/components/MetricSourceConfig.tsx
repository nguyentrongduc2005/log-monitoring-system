import { useEffect, useState } from "react";
import {
  managementButtonClass,
  managementInputClass,
  managementLabelClass,
  managementPanelClass
} from "@/shared/components/management-ui";
import {
  getMetricSource,
  saveMetricSource,
  updateMetricSource,
  testMetricSourceConnection
} from "../application-api";
import type { MetricSource, MetricSourceRequest } from "../application-types";

type MetricSourceConfigProps = {
  applicationId: string;
};

export default function MetricSourceConfig({ applicationId }: MetricSourceConfigProps) {
  const [config, setConfig] = useState<MetricSource | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [testResult, setTestResult] = useState<boolean | null>(null);
  const [testing, setTesting] = useState(false);

  // Form state
  const [targetHost, setTargetHost] = useState("");
  const [targetPort, setTargetPort] = useState(8080);
  const [metricsPath, setMetricsPath] = useState("/actuator/prometheus");
  const [scrapeInterval, setScrapeInterval] = useState("15s");
  const [enabled, setEnabled] = useState(true);

  const loadConfig = async () => {
    try {
      setLoading(true);
      setError(null);
      const source = await getMetricSource(applicationId);
      if (source) {
        setConfig(source);
        setTargetHost(source.targetHost);
        setTargetPort(source.targetPort);
        setMetricsPath(source.metricsPath);
        setScrapeInterval(source.scrapeInterval);
        setEnabled(source.enabled);
      }
    } catch (err: any) {
      setError(err.message || "Failed to load metric source config");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadConfig();
  }, [applicationId]);

  async function handleSave(e: React.FormEvent) {
    e.preventDefault();
    try {
      setSaving(true);
      setError(null);
      
      const request: MetricSourceRequest = {
        targetHost,
        targetPort,
        metricsPath,
        scrapeInterval,
        enabled
      };

      let saved: MetricSource;
      if (config) {
        saved = await updateMetricSource(applicationId, request);
      } else {
        saved = await saveMetricSource(applicationId, request);
      }
      setConfig(saved);
      // Show success briefly or just rely on the updated state
    } catch (err: any) {
      setError(err.message || "Failed to save config");
    } finally {
      setSaving(false);
    }
  }

  async function handleTest() {
    try {
      setTesting(true);
      setTestResult(null);
      const request: MetricSourceRequest = {
        targetHost,
        targetPort,
        metricsPath,
        scrapeInterval,
        enabled
      };
      const result = await testMetricSourceConnection(request);
      setTestResult(result);
    } catch (err) {
      setTestResult(false);
    } finally {
      setTesting(false);
    }
  }

  if (loading) {
    return <div className="p-4 text-sm text-muted">Loading config...</div>;
  }

  return (
    <section className={managementPanelClass}>
      <div className="border-b border-border bg-surface-raised/35 p-4">
        <h2 className="text-base font-semibold text-text">Prometheus Metric Source</h2>
        <p className="mt-1 text-sm text-muted">
          Configure how Prometheus should scrape metrics from this application.
        </p>
      </div>

      <div className="p-4">
        {error ? (
          <p className="mb-4 rounded-md border border-error/30 bg-error/10 px-3 py-2 text-sm text-error">
            {error}
          </p>
        ) : null}

        <form onSubmit={handleSave} className="space-y-4">
          <div className="grid gap-4 md:grid-cols-2">
            <div>
              <label className={managementLabelClass}>Target Host</label>
              <input
                type="text"
                className={managementInputClass}
                value={targetHost}
                onChange={e => setTargetHost(e.target.value)}
                placeholder="e.g. host.docker.internal or app-service"
                required
              />
            </div>
            <div>
              <label className={managementLabelClass}>Target Port</label>
              <input
                type="number"
                className={managementInputClass}
                value={targetPort}
                onChange={e => setTargetPort(Number(e.target.value))}
                placeholder="e.g. 8080"
                required
              />
            </div>
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            <div>
              <label className={managementLabelClass}>Metrics Path</label>
              <input
                type="text"
                className={managementInputClass}
                value={metricsPath}
                onChange={e => setMetricsPath(e.target.value)}
                placeholder="/actuator/prometheus"
                required
              />
            </div>
            <div>
              <label className={managementLabelClass}>Scrape Interval</label>
              <input
                type="text"
                className={managementInputClass}
                value={scrapeInterval}
                onChange={e => setScrapeInterval(e.target.value)}
                placeholder="15s"
                required
              />
            </div>
          </div>

          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              id="enabled"
              checked={enabled}
              onChange={e => setEnabled(e.target.checked)}
              className="rounded border-border bg-surface text-primary"
            />
            <label htmlFor="enabled" className="text-sm font-medium text-text">
              Enable Scraping
            </label>
          </div>

          <div className="mt-4 flex items-center justify-between border-t border-border pt-4">
            <div className="flex items-center gap-2">
              <button
                type="button"
                className={managementButtonClass}
                onClick={handleTest}
                disabled={testing || !targetHost || !targetPort}
              >
                {testing ? "Testing..." : "Test Connection"}
              </button>
              {testResult !== null && (
                <span className={`text-sm font-medium ${testResult ? "text-success" : "text-error"}`}>
                  {testResult ? "UP" : "DOWN"}
                </span>
              )}
            </div>

            <button
              type="submit"
              className={`${managementButtonClass} bg-primary text-primary-content hover:bg-primary/90`}
              disabled={saving}
            >
              {saving ? "Saving..." : "Save Configuration"}
            </button>
          </div>
        </form>
      </div>
    </section>
  );
}
