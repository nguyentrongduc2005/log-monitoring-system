import { useEffect, useMemo, useState } from "react";
import type { FormEvent } from "react";
import { PageHeader } from "@/shared/layouts/page-header-context";
import {
  getAlertRules,
  saveAlertRule,
  toggleAlertRule
} from "./alert-rules-adapter";
import type {
  AlertRule,
  AlertRuleDraft,
  AlertRuleStatus
} from "./alert-rules-types";
import AlertRuleBuilder from "./components/AlertRuleBuilder";
import AlertRuleInventory from "./components/AlertRuleInventory";
import AlertRuleSummary from "./components/AlertRuleSummary";

const emptyDraft: AlertRuleDraft = {
  name: "Auth-401-Flood",
  applicationName: "Payment Gateway",
  serviceName: "auth-service",
  severity: "CRITICAL",
  metric: "LOG_COUNT",
  operator: ">",
  threshold: 500,
  windowSeconds: 30,
  channelType: "Telegram",
  chatRoomId: "room-telegram-ops-critical",
  channelTarget: "#ops-critical"
};

export function Component() {
  const [rules, setRules] = useState<AlertRule[]>([]);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<"ALL" | AlertRuleStatus>(
    "ALL"
  );
  const [draft, setDraft] = useState<AlertRuleDraft>(emptyDraft);
  const [editingRule, setEditingRule] = useState<AlertRule | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function loadRules() {
    setLoading(true);
    setError(null);
    try {
      setRules(await getAlertRules());
    } catch {
      setError("Unable to load alert rules.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    queueMicrotask(() => {
      void loadRules();
    });
  }, []);

  const filteredRules = useMemo(() => {
    const normalized = search.trim().toLowerCase();
    return rules.filter(rule => {
      const matchesStatus = statusFilter === "ALL" || rule.status === statusFilter;
      const matchesSearch =
        !normalized ||
        `${rule.name} ${rule.applicationName} ${rule.serviceName} ${rule.metric} ${rule.channelTarget}`
          .toLowerCase()
          .includes(normalized);
      return matchesStatus && matchesSearch;
    });
  }, [rules, search, statusFilter]);

  function resetForm() {
    setDraft(emptyDraft);
    setEditingRule(null);
  }

  function editRule(rule: AlertRule) {
    setEditingRule(rule);
    setDraft({
      name: rule.name,
      applicationName: rule.applicationName,
      serviceName: rule.serviceName,
      severity: rule.severity,
      metric: rule.metric,
      operator: rule.operator,
      threshold: rule.threshold,
      windowSeconds: rule.windowSeconds,
      channelType: rule.channelType,
      chatRoomId: rule.chatRoomId,
      channelTarget: rule.channelTarget
    });
  }

  async function submitRule(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    try {
      const saved = await saveAlertRule(draft, editingRule?.id);
      setRules(current =>
        editingRule
          ? current.map(rule => (rule.id === saved.id ? saved : rule))
          : [saved, ...current]
      );
      resetForm();
    } catch {
      setError("Unable to save alert rule.");
    } finally {
      setSaving(false);
    }
  }

  async function toggleRule(rule: AlertRule) {
    setSaving(true);
    setError(null);
    try {
      const updated = await toggleAlertRule(rule.id);
      setRules(current =>
        current.map(item => (item.id === updated.id ? updated : item))
      );
    } catch {
      setError("Unable to update alert rule status.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="space-y-5">
      <PageHeader title="Alert Rules" />

      <div>
        <h1 className="text-2xl font-semibold text-text">Alert Rules</h1>
        <p className="mt-1 text-sm text-muted">
          Define and manage automated logic for monitoring your infrastructure.
        </p>
      </div>

      <AlertRuleSummary rules={rules} />

      <AlertRuleBuilder
        draft={draft}
        editingRule={editingRule}
        onDraftChange={setDraft}
        onReset={resetForm}
        onSubmit={event => void submitRule(event)}
        saving={saving}
      />

      <AlertRuleInventory
        error={error}
        filteredRules={filteredRules}
        loading={loading}
        onEdit={editRule}
        onRefresh={() => void loadRules()}
        onSearchChange={setSearch}
        onStatusFilterChange={setStatusFilter}
        onToggle={rule => void toggleRule(rule)}
        saving={saving}
        search={search}
        statusFilter={statusFilter}
      />
    </div>
  );
}
