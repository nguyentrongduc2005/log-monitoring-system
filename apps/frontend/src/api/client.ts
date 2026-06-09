import axios, {
  AxiosError,
  type InternalAxiosRequestConfig
} from "axios";

declare module "axios" {
  export interface AxiosRequestConfig {
    skipAuthRefresh?: boolean;
  }

  export interface InternalAxiosRequestConfig {
    skipAuthRefresh?: boolean;
    authRetryAttempted?: boolean;
  }
}

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15_000
});

type AuthInterceptorOptions = {
  getAccessToken: () => string | null;
  refresh: () => Promise<string>;
  onUnauthorized: () => void;
};

let refreshRequest: Promise<string> | null = null;

export function installAuthInterceptors({
  getAccessToken,
  refresh,
  onUnauthorized
}: AuthInterceptorOptions) {
  const requestInterceptor = apiClient.interceptors.request.use(config => {
    const accessToken = getAccessToken();

    if (accessToken && !config.skipAuthRefresh) {
      config.headers.set("Authorization", `Bearer ${accessToken}`);
    }

    return config;
  });

  const responseInterceptor = apiClient.interceptors.response.use(
    response => response,
    async (error: AxiosError) => {
      const request = error.config as InternalAxiosRequestConfig | undefined;

      if (
        error.response?.status !== 401 ||
        !request ||
        request.skipAuthRefresh ||
        request.authRetryAttempted
      ) {
        return Promise.reject(error);
      }

      request.authRetryAttempted = true;

      try {
        refreshRequest ??= refresh().finally(() => {
          refreshRequest = null;
        });

        const accessToken = await refreshRequest;
        request.headers.set("Authorization", `Bearer ${accessToken}`);
        return apiClient(request);
      } catch (refreshError) {
        onUnauthorized();
        return Promise.reject(refreshError);
      }
    }
  );

  return () => {
    apiClient.interceptors.request.eject(requestInterceptor);
    apiClient.interceptors.response.eject(responseInterceptor);
  };
}
