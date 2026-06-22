import { useEffect, useMemo, useState, type PropsWithChildren } from "react";
import { useAuth } from "@/features/auth/auth-context";
import { acknowledgeAlert, createAlertConnection, getAlertCenterSnapshot, resolveAlert } from "./alerts-api";
import { AlertCenterContext, type AlertCenterValue } from "./alert-center-context";
import type { Alert, AlertApplication, AlertConnectionState } from "./alerts-types";

export default function AlertCenterProvider({ children }: PropsWithChildren) {
  const { session } = useAuth();
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [applications, setApplications] = useState<AlertApplication[]>([]);
  const [connectionState, setConnectionState] = useState<AlertConnectionState>("connecting");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function refresh() {
    setLoading(true);
    setError(null);
    try {
      const snapshot = await getAlertCenterSnapshot();
      setApplications(snapshot.applications);
      setAlerts(snapshot.alerts);
    } catch {
      setError("Unable to load alerts.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { queueMicrotask(() => void refresh()); }, []);

  useEffect(() => {
    if (loading || !session?.accessToken || applications.length === 0) return;
    return createAlertConnection({
      accessToken: session.accessToken,
      applications,
      onAlert: alert => setAlerts(current => [alert, ...current.filter(item => item.id !== alert.id)].slice(0, 1000)),
      onStateChange: setConnectionState
    });
  }, [applications, loading, session?.accessToken]);

  async function perform(id: string, action: (id: string) => Promise<Alert>) {
    setSaving(true);
    setError(null);
    try {
      const updated = await action(id);
      setAlerts(current => current.map(alert => alert.id === updated.id ? updated : alert));
    } catch {
      setError("Unable to update alert status.");
    } finally {
      setSaving(false);
    }
  }

  const value = useMemo<AlertCenterValue>(() => ({
    alerts,
    applications,
    connectionState,
    error,
    loading,
    openCount: alerts.filter(alert => alert.status === "OPEN").length,
    saving,
    refresh,
    acknowledge: id => perform(id, acknowledgeAlert),
    resolve: id => perform(id, resolveAlert)
  }), [alerts, applications, connectionState, error, loading, saving]);

  return <AlertCenterContext.Provider value={value}>{children}</AlertCenterContext.Provider>;
}
