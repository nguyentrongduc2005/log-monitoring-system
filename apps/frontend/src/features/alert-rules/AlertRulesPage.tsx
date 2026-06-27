import { useEffect, useMemo, useState } from "react";
import type { FormEvent } from "react";
import { Button } from "@/shared/components/ui";
import { getApplications } from "@/features/applications/application-api";
import type { Application } from "@/features/applications/application-types";
import ConfirmDialog from "@/features/user-access/components/ConfirmDialog";
import { PageHeader } from "@/shared/layouts/page-header-context";
import {
  deleteAlertRule,
  getAlertingError,
  getAlertRules,
  getTelegramChatRooms,
  saveAlertRule,
  toggleAlertRule
} from "./alert-rules-adapter";
import type {
  AlertRule,
  AlertRuleDraft,
  AlertRuleStatus,
  ChatRoom
} from "./alert-rules-types";
import AlertRuleBuilder from "./components/AlertRuleBuilder";
import AlertRuleInventory from "./components/AlertRuleInventory";
import AlertRuleSummary from "./components/AlertRuleSummary";

function emptyDraft(applicationId = ""): AlertRuleDraft {
  return {
    applicationId,
    name: "",
    description: "",
    minSeverity: "ERROR",
    severity: "CRITICAL",
    keywordPattern: "",
    thresholdCount: 1,
    thresholdWindowSeconds: 60,
    cooldownSeconds: 300,
    websocketEnabled: true,
    telegramChatRoomIds: []
  };
}

export function Component() {
  const [rules, setRules] = useState<AlertRule[]>([]);
  const [applications, setApplications] = useState<Application[]>([]);
  const [chatRooms, setChatRooms] = useState<ChatRoom[]>([]);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<"ALL" | AlertRuleStatus>("ALL");
  const [draft, setDraft] = useState<AlertRuleDraft>(emptyDraft());
  const [editingRule, setEditingRule] = useState<AlertRule | null>(null);
  const [deleteRule, setDeleteRule] = useState<AlertRule | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [builderOpen, setBuilderOpen] = useState(false);

  async function loadData() {
    setLoading(true);
    setError(null);
    try {
      const [nextRules, nextApplications, nextRooms] = await Promise.all([
        getAlertRules(),
        getApplications(),
        getTelegramChatRooms()
      ]);
      setRules(nextRules);
      setApplications(nextApplications);
      setChatRooms(nextRooms);
      setDraft(current => ({
        ...current,
        applicationId: current.applicationId || nextApplications[0]?.id || ""
      }));
    } catch (loadError) {
      setError(getAlertingError(loadError, "Unable to load alert rules."));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    queueMicrotask(() => void loadData());
  }, []);

  const applicationNames = useMemo(
    () => new Map(applications.map(app => [app.id || "", app.displayName || app.name || ""])),
    [applications]
  );

  const filteredRules = useMemo(() => {
    const normalized = search.trim().toLowerCase();
    return rules.filter(rule => {
      const matchesStatus = statusFilter === "ALL" || rule.status === statusFilter;
      const applicationName = applicationNames.get(rule.applicationId) || "";
      return (
        matchesStatus &&
        (!normalized ||
          `${rule.name} ${rule.description || ""} ${applicationName} ${rule.minSeverity}`
            .toLowerCase()
            .includes(normalized))
      );
    });
  }, [applicationNames, rules, search, statusFilter]);

  function resetForm() {
    setDraft(emptyDraft(applications[0]?.id || ""));
    setEditingRule(null);
    setBuilderOpen(false);
  }

  function editRule(rule: AlertRule) {
    setEditingRule(rule);
    setDraft({
      applicationId: rule.applicationId,
      name: rule.name,
      description: rule.description || "",
      minSeverity: rule.minSeverity,
      severity: rule.severity,
      keywordPattern: rule.keywordPattern || "",
      thresholdCount: rule.thresholdCount,
      thresholdWindowSeconds: rule.thresholdWindowSeconds,
      cooldownSeconds: rule.cooldownSeconds,
      websocketEnabled: rule.deliveryTargets.some(target => target.channel === "WEBSOCKET"),
      telegramChatRoomIds: rule.deliveryTargets.flatMap(target =>
        target.channel === "TELEGRAM" && target.chatRoomId ? [target.chatRoomId] : []
      )
    });
    setBuilderOpen(true);
    window.scrollTo({ top: 0, behavior: "smooth" });
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
    } catch (saveError) {
      setError(getAlertingError(saveError, "Unable to save alert rule."));
    } finally {
      setSaving(false);
    }
  }

  async function changeStatus(rule: AlertRule) {
    setSaving(true);
    setError(null);
    try {
      const updated = await toggleAlertRule(rule);
      setRules(current => current.map(item => (item.id === updated.id ? updated : item)));
    } catch (actionError) {
      setError(getAlertingError(actionError, "Unable to update alert rule status."));
    } finally {
      setSaving(false);
    }
  }

  async function confirmDelete() {
    if (!deleteRule) return;
    setSaving(true);
    setError(null);
    try {
      await deleteAlertRule(deleteRule.id);
      setRules(current => current.filter(rule => rule.id !== deleteRule.id));
      if (editingRule?.id === deleteRule.id) resetForm();
      setDeleteRule(null);
    } catch (actionError) {
      setError(getAlertingError(actionError, "Unable to delete alert rule."));
      setDeleteRule(null);
    } finally {
      setSaving(false);
    }
  }

  const selectableRooms = chatRooms.filter(
    room => room.status === "ACTIVE" || draft.telegramChatRoomIds.includes(room.id)
  );

  return (
    <div className="space-y-5">
      <PageHeader
        actions={
          <div className="flex gap-2">
            <Button onClick={() => void loadData()} variant="outline">
              Refresh
            </Button>
            <Button onClick={() => setBuilderOpen(!builderOpen)}>
              {builderOpen ? "Hide Rule Builder" : "New Alert Rule"}
            </Button>
          </div>
        }
        title="Alert Rules"
      />
      <p className="text-sm text-muted">
        Define log thresholds and notification destinations for each application.
      </p>

      <AlertRuleSummary rules={rules} />
      
      {builderOpen ? (
        <AlertRuleBuilder
          applications={applications}
          chatRooms={selectableRooms}
          draft={draft}
          editingRule={editingRule}
          onDraftChange={setDraft}
          onReset={resetForm}
          onSubmit={event => void submitRule(event)}
          saving={saving}
        />
      ) : null}
      <AlertRuleInventory
        applicationNames={applicationNames}
        chatRooms={chatRooms}
        error={error}
        filteredRules={filteredRules}
        loading={loading}
        onDelete={setDeleteRule}
        onEdit={editRule}
        onRefresh={() => void loadData()}
        onSearchChange={setSearch}
        onStatusFilterChange={setStatusFilter}
        onToggle={rule => void changeStatus(rule)}
        saving={saving}
        search={search}
        statusFilter={statusFilter}
      />

      {deleteRule ? (
        <ConfirmDialog
          confirmLabel="Delete rule"
          description={`Deleting “${deleteRule.name}” also removes its stored alerts. This cannot be undone.`}
          loading={saving}
          onCancel={() => setDeleteRule(null)}
          onConfirm={() => void confirmDelete()}
          title="Delete alert rule?"
          tone="danger"
        />
      ) : null}
    </div>
  );
}
