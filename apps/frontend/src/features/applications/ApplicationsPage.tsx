import { useEffect, useMemo, useState } from "react";
import { PageHeader } from "@/shared/layouts/page-header-context";
import {
  changeApplicationStatus,
  createApiKey,
  createApplication,
  getApplicationApiKeys,
  getApplicationError,
  getApplications,
  revokeApiKey,
  rotateApiKey,
  updateApplication
} from "./application-api";
import type {
  ApiKey,
  ApiKeyCreation,
  Application,
  ApplicationRequest,
  ApplicationStatus,
  CreateApiKeyRequest
} from "./application-types";
import ApiKeyFormDialog from "./components/ApiKeyFormDialog";
import ApiKeysPanel from "./components/ApiKeysPanel";
import ApplicationFormDialog from "./components/ApplicationFormDialog";
import ApplicationSummary from "./components/ApplicationSummary";
import ApplicationsTable from "./components/ApplicationsTable";
import MetricSourceConfig from "./components/MetricSourceConfig";
import ApplicationTabsToolbar from "./components/ApplicationTabsToolbar";
import type { ApplicationTab } from "./components/ApplicationTabsToolbar";
import RawApiKeyDialog from "./components/RawApiKeyDialog";
import ConfirmDialog from "@/features/user-access/components/ConfirmDialog";
import {
  managementPanelClass,
  managementPrimaryButtonClass,
  managementButtonClass
} from "@/shared/components/management-ui";

type ConfirmAction =
  | { type: "status"; status: ApplicationStatus }
  | { type: "rotate"; apiKey: ApiKey }
  | { type: "revoke"; apiKey: ApiKey };

export function Component() {
  const [applications, setApplications] = useState<Application[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [apiKeys, setApiKeys] = useState<ApiKey[]>([]);
  const [search, setSearch] = useState("");
  const [activeTab, setActiveTab] = useState<ApplicationTab>("applications");
  const [loading, setLoading] = useState(true);
  const [keysLoading, setKeysLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [keyError, setKeyError] = useState<string | null>(null);
  const [showCreateApp, setShowCreateApp] = useState(false);
  const [editApplication, setEditApplication] = useState<Application | null>(
    null
  );
  const [showCreateKey, setShowCreateKey] = useState(false);
  const [rawApiKey, setRawApiKey] = useState<ApiKeyCreation | null>(null);
  const [confirmAction, setConfirmAction] = useState<ConfirmAction | null>(
    null
  );

  const selectedApplication =
    applications.find(app => app.id === selectedId) ?? applications[0] ?? null;

  async function loadApplications() {
    setLoading(true);
    setError(null);
    try {
      const nextApplications = await getApplications();
      setApplications(nextApplications);
      if (nextApplications.length === 0) {
        setApiKeys([]);
      }
      setSelectedId(current =>
        current && nextApplications.some(app => app.id === current)
          ? current
          : nextApplications[0]?.id ?? null
      );
    } catch (loadError) {
      setError(getApplicationError(loadError, "Unable to load applications."));
    } finally {
      setLoading(false);
    }
  }

  async function loadApiKeys(applicationId: string) {
    setKeysLoading(true);
    setKeyError(null);
    try {
      setApiKeys(await getApplicationApiKeys(applicationId));
    } catch (loadError) {
      setKeyError(getApplicationError(loadError, "Unable to load API keys."));
    } finally {
      setKeysLoading(false);
    }
  }

  useEffect(() => {
    queueMicrotask(() => {
      void loadApplications();
    });
  }, []);

  useEffect(() => {
    if (!selectedApplication?.id) {
      return;
    }

    queueMicrotask(() => {
      void loadApiKeys(selectedApplication.id);
    });
  }, [selectedApplication?.id]);

  const filteredApplications = useMemo(() => {
    const normalizedSearch = search.trim().toLowerCase();
    if (!normalizedSearch) {
      return applications;
    }

    return applications.filter(application =>
      `${application.name ?? ""} ${application.displayName ?? ""} ${application.description ?? ""} ${application.status ?? ""}`
        .toLowerCase()
        .includes(normalizedSearch)
    );
  }, [applications, search]);

  function replaceApplication(updated: Application) {
    setApplications(current =>
      current.map(application =>
        application.id === updated.id ? updated : application
      )
    );
  }

  async function submitApplication(request: ApplicationRequest) {
    setSaving(true);
    setFormError(null);
    try {
      if (editApplication?.id) {
        const updated = await updateApplication(editApplication.id, request);
        replaceApplication(updated);
        setEditApplication(null);
      } else {
        const created = await createApplication(request);
        setApplications(current => [created, ...current]);
        setSelectedId(created.id ?? null);
        setShowCreateApp(false);
      }
    } catch (saveError) {
      setFormError(
        getApplicationError(saveError, "Unable to save application.")
      );
    } finally {
      setSaving(false);
    }
  }

  async function submitApiKey(request: CreateApiKeyRequest) {
    if (!selectedApplication?.id) {
      return;
    }

    setSaving(true);
    setKeyError(null);
    try {
      const created = await createApiKey(selectedApplication.id, request);
      setRawApiKey(created);
      setShowCreateKey(false);
      await loadApiKeys(selectedApplication.id);
    } catch (saveError) {
      setKeyError(getApplicationError(saveError, "Unable to create API key."));
    } finally {
      setSaving(false);
    }
  }

  async function performConfirmAction() {
    if (!confirmAction || !selectedApplication?.id) {
      return;
    }

    setSaving(true);
    setKeyError(null);
    try {
      if (confirmAction.type === "status") {
        const updated = await changeApplicationStatus(
          selectedApplication.id,
          confirmAction.status
        );
        replaceApplication(updated);
      }

      if (confirmAction.type === "rotate" && confirmAction.apiKey.id) {
        const rotated = await rotateApiKey(
          selectedApplication.id,
          confirmAction.apiKey.id
        );
        setRawApiKey(rotated);
        await loadApiKeys(selectedApplication.id);
      }

      if (confirmAction.type === "revoke" && confirmAction.apiKey.id) {
        await revokeApiKey(selectedApplication.id, confirmAction.apiKey.id);
        await loadApiKeys(selectedApplication.id);
      }

      setConfirmAction(null);
    } catch (actionError) {
      setKeyError(getApplicationError(actionError));
      setConfirmAction(null);
    } finally {
      setSaving(false);
    }
  }

  const confirmCopy = confirmAction
    ? {
        status: {
          title:
            confirmAction.type === "status" &&
            confirmAction.status === "INACTIVE"
              ? "Deactivate application?"
              : "Activate application?",
          description:
            confirmAction.type === "status" &&
            confirmAction.status === "INACTIVE"
              ? "Inactive applications cannot use API keys to ingest logs."
              : "The application will be allowed to use active API keys again.",
          confirmLabel:
            confirmAction.type === "status" &&
            confirmAction.status === "INACTIVE"
              ? "Deactivate app"
              : "Activate app"
        },
        rotate: {
          title: "Rotate API key?",
          description:
            "The current key will be revoked immediately and a new raw key will be shown once.",
          confirmLabel: "Rotate key"
        },
        revoke: {
          title: "Revoke API key?",
          description:
            "Requests using this API key will stop working immediately.",
          confirmLabel: "Revoke key"
        }
      }[confirmAction.type]
    : null;

  return (
    <div className="space-y-5">
      <PageHeader
        actions={
          <div className="flex flex-wrap gap-2">
            <button
              className={managementButtonClass}
              disabled={loading}
              onClick={() => void loadApplications()}
              type="button"
            >
              Refresh
            </button>
            <button
              className={managementPrimaryButtonClass}
              onClick={() => {
                setFormError(null);
                setShowCreateApp(true);
              }}
              type="button"
            >
              Create app
            </button>
          </div>
        }
        title="Applications"
      />

      <ApplicationSummary apiKeys={apiKeys} applications={applications} />

      <section className={managementPanelClass}>
        <ApplicationTabsToolbar
          activeTab={activeTab}
          applications={applications}
          onCreateKey={() => {
            setKeyError(null);
            setShowCreateKey(true);
          }}
          onSearchChange={setSearch}
          onSelectApplication={setSelectedId}
          onTabChange={setActiveTab}
          search={search}
          selectedApplication={selectedApplication}
        />

        {activeTab === "applications" ? (
          <ApplicationsTable
            applications={filteredApplications}
            error={error}
            loading={loading}
            onEdit={application => {
              setFormError(null);
              setEditApplication(application);
            }}
            onManageKeys={application => {
              setSelectedId(application.id ?? null);
              setActiveTab("api-keys");
            }}
            onToggleStatus={(application, status) => {
              setSelectedId(application.id ?? null);
              setConfirmAction({ type: "status", status });
            }}
            selectedApplication={selectedApplication}
          />
        ) : null}

        {activeTab === "api-keys" ? (
          <ApiKeysPanel
            apiKeys={apiKeys}
            error={keyError}
            loading={keysLoading}
            onRevoke={apiKey => setConfirmAction({ type: "revoke", apiKey })}
            onRotate={apiKey => setConfirmAction({ type: "rotate", apiKey })}
            selectedApplication={selectedApplication}
          />
        ) : null}

        {activeTab === "metric-sources" && selectedApplication ? (
          <MetricSourceConfig applicationId={selectedApplication.id!} />
        ) : null}
      </section>

      {showCreateApp ? (
        <ApplicationFormDialog
          error={formError}
          onClose={() => setShowCreateApp(false)}
          onSubmit={request => void submitApplication(request)}
          saving={saving}
        />
      ) : null}

      {editApplication ? (
        <ApplicationFormDialog
          application={editApplication}
          error={formError}
          onClose={() => setEditApplication(null)}
          onSubmit={request => void submitApplication(request)}
          saving={saving}
        />
      ) : null}

      {showCreateKey ? (
        <ApiKeyFormDialog
          error={keyError}
          onClose={() => setShowCreateKey(false)}
          onSubmit={request => void submitApiKey(request)}
          saving={saving}
        />
      ) : null}

      {rawApiKey ? (
        <RawApiKeyDialog apiKey={rawApiKey} onClose={() => setRawApiKey(null)} />
      ) : null}

      {confirmAction && confirmCopy ? (
        <ConfirmDialog
          confirmLabel={confirmCopy.confirmLabel}
          description={confirmCopy.description}
          loading={saving}
          onCancel={() => setConfirmAction(null)}
          onConfirm={() => void performConfirmAction()}
          title={confirmCopy.title}
          tone={confirmAction.type === "revoke" ? "danger" : "warning"}
        />
      ) : null}
    </div>
  );
}
