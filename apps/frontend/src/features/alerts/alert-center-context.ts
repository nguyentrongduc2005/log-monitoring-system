import { createContext, useContext } from "react";
import type { Alert, AlertApplication, AlertConnectionState } from "./alerts-types";

export type AlertCenterValue = {
  alerts: Alert[];
  applications: AlertApplication[];
  connectionState: AlertConnectionState;
  error: string | null;
  loading: boolean;
  openCount: number;
  saving: boolean;
  refresh: () => Promise<void>;
  acknowledge: (id: string) => Promise<void>;
  resolve: (id: string) => Promise<void>;
};

export const AlertCenterContext = createContext<AlertCenterValue | null>(null);

export function useAlertCenter() {
  const context = useContext(AlertCenterContext);
  if (!context) throw new Error("useAlertCenter must be used within AlertCenterProvider");
  return context;
}
