import type { components } from "@/api/generated/api-types";

export type Application = components["schemas"]["ApplicationResponse"];
export type ApplicationRequest = components["schemas"]["ApplicationRequest"];
export type ApiKey = components["schemas"]["ApiKeyResponse"];
export type ApiKeyCreation = components["schemas"]["ApiKeyCreationResponse"];
export type CreateApiKeyRequest =
  components["schemas"]["CreateApiKeyRequest"];

export type ApplicationStatus = "ACTIVE" | "INACTIVE";

export type ApiEnvelope<T> = {
  success?: boolean;
  message?: string;
  data?: T;
  timestamp?: string;
};

export type MetricSource = {
  id: string;
  applicationId: string;
  targetHost: string;
  targetPort: number;
  metricsPath: string;
  scrapeInterval: string;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
};

export type MetricSourceRequest = {
  targetHost: string;
  targetPort: number;
  metricsPath: string;
  scrapeInterval: string;
  enabled: boolean;
};
