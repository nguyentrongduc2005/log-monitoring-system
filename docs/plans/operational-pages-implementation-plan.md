# Operational Pages Implementation Plan

**Source spec:** `docs/specs/operational-pages-design.md`

**Goal:** Implement the authenticated Overview, Live Logs, and Profile pages with contract-ready adapters, mock operational data, and correct self-service profile authorization.

**Architecture:** Keep all page UI inside the existing React feature-based frontend and render through the existing `AppLayout` and `PageHeader` shell. Overview and Live Logs read through feature-local mock adapters that can later be replaced by HTTP/WebSocket adapters. Profile uses the existing generated identity DTOs and `apiClient`; backend security is minimally aligned so `/api/v1/users/me/**` self-service endpoints are authenticated for every user while `/api/v1/users/{id}` management remains admin-only.

**Tech stack:** React 19, React Router 7, TypeScript 5, Tailwind CSS v4, Axios, Vitest, Testing Library, Spring Boot 3.5, Spring Security, JUnit 5, MockMvc.

---

## File Map

- Modify: `apps/backend/pom.xml` — add `spring-security-test` as a test dependency for focused security matcher tests.
- Modify: `apps/backend/src/main/java/com/vdt/log_monitoring/shared/security/SecurityConfig.java` — authorize `/api/v1/users/me/**` for any authenticated user before the admin-only `/api/v1/users/**` matcher.
- Create: `apps/backend/src/test/java/com/vdt/log_monitoring/shared/security/SecurityConfigTest.java` — verify self-service profile endpoints are accessible to `ENGINEER` while user management endpoints remain admin-only.
- Modify: `apps/frontend/src/features/auth/auth-context.ts` — expose an auth-context function for replacing the current session user after profile save.
- Modify: `apps/frontend/src/features/auth/AuthProvider.tsx` — implement current-session user updates and persist them to localStorage.
- Modify: `apps/frontend/src/shared/layouts/AppLayout.test.tsx` — add the new auth-context function to test fixtures.
- Create: `apps/frontend/src/features/profile/profile-api.ts` — wrap `GET/PUT /users/me` and `PUT /users/me/password` using generated API types.
- Create: `apps/frontend/src/features/profile/ProfilePage.tsx` — render identity summary, metadata, profile edit form, password form, and role/status explanations.
- Create: `apps/frontend/src/features/profile/ProfilePage.test.tsx` — cover profile rendering, validation, save, password change, missing timestamp behavior, and permission-safe error states.
- Modify: `apps/frontend/src/app/router.tsx` — add the authenticated `/profile` route.
- Modify: `apps/frontend/src/shared/layouts/UserMenu.tsx` — navigate the profile menu item to `/profile` and use English label `Profile`.
- Modify: `apps/frontend/src/shared/layouts/Topbar.test.tsx` — wrap tests in a router and verify profile menu navigation.
- Modify: `apps/frontend/src/shared/layouts/AppLayout.test.tsx` — keep account-menu logout test compiling with the updated user menu and auth fixture.
- Create: `apps/frontend/src/features/dashboard/overview-types.ts` — define Overview page adapter data shapes.
- Create: `apps/frontend/src/features/dashboard/overview-adapter.ts` — provide a mock snapshot adapter for KPI, pipeline, trend, noisy apps, alerts, and demo readiness data.
- Create: `apps/frontend/src/features/dashboard/components/OverviewMetricCard.tsx` — render compact KPI cards.
- Create: `apps/frontend/src/features/dashboard/components/PipelineStrip.tsx` — render the architecture flow summary.
- Create: `apps/frontend/src/features/dashboard/components/LogVolumeChart.tsx` — render an accessible lightweight trend visualization without adding dependencies.
- Create: `apps/frontend/src/features/dashboard/components/NoisyApplicationsTable.tsx` — render top noisy applications.
- Create: `apps/frontend/src/features/dashboard/components/RecentCriticalAlerts.tsx` — render deduplicated critical alert summaries.
- Create: `apps/frontend/src/features/dashboard/components/DemoReadinessPanel.tsx` — render 500 logs / 2 seconds readiness metrics without claiming real verification.
- Modify: `apps/frontend/src/features/dashboard/DashboardPage.tsx` — compose the Overview page from the adapter and components.
- Create: `apps/frontend/src/features/dashboard/DashboardPage.test.tsx` — verify Overview sections, loading/error/empty/degraded states, and adapter-backed content.
- Create: `apps/frontend/src/features/live-logs/live-logs-types.ts` — define log row, filters, connection state, and adapter data shapes.
- Create: `apps/frontend/src/features/live-logs/live-logs-adapter.ts` — provide mock application options and a bounded mock stream source.
- Create: `apps/frontend/src/features/live-logs/components/LiveLogsToolbar.tsx` — render connection status, pause/resume, clear, and stream counts.
- Create: `apps/frontend/src/features/live-logs/components/LiveLogFilters.tsx` — render application, level, keyword, and trace ID filters.
- Create: `apps/frontend/src/features/live-logs/components/LiveLogTable.tsx` — render the hybrid dense table/stream.
- Create: `apps/frontend/src/features/live-logs/components/LogDetailDrawer.tsx` — render selected row details and key-value attributes.
- Modify: `apps/frontend/src/features/live-logs/LiveLogsPage.tsx` — compose Live Logs with filters, capped visible buffer, pause/resume, clear, and drawer state.
- Create: `apps/frontend/src/features/live-logs/LiveLogsPage.test.tsx` — cover filters, pause/resume, clear, drawer details, empty states, and status labels.

## Task 1: Align Self-Service Profile Authorization

**Files:**

- Modify: `apps/backend/pom.xml`
- Modify: `apps/backend/src/main/java/com/vdt/log_monitoring/shared/security/SecurityConfig.java`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/shared/security/SecurityConfigTest.java`

- [ ] **Step 1: Add Spring Security test support**

Add the following test dependency beside the existing `spring-boot-starter-test` dependency in `apps/backend/pom.xml`:

```xml
<dependency>
	<groupId>org.springframework.security</groupId>
	<artifactId>spring-security-test</artifactId>
	<scope>test</scope>
</dependency>
```

- [ ] **Step 2: Add focused security tests**

Create `apps/backend/src/test/java/com/vdt/log_monitoring/shared/security/SecurityConfigTest.java`:

```java
package com.vdt.log_monitoring.shared.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.vdt.log_monitoring.api.identity.UserController;
import com.vdt.log_monitoring.modules.identity.api.IdentityFacade;

@WebMvcTest(UserController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
class SecurityConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private JwtTokenProvider jwtTokenProvider;

	@MockBean
	private IdentityFacade identityFacade;

	@Test
	void engineerCanReadOwnProfile() throws Exception {
		when(identityFacade.findUserByEmail("engineer@example.com")).thenReturn(userDto());

		mockMvc.perform(get("/api/v1/users/me")
				.with(user("engineer@example.com").roles("ENGINEER")))
			.andExpect(status().isOk());
	}

	@Test
	void engineerCanUpdateOwnProfile() throws Exception {
		IdentityFacade.UserDto user = userDto();
		when(identityFacade.findUserByEmail("engineer@example.com")).thenReturn(user);
		when(identityFacade.updateProfile(user.id(), "new@example.com", "New Name")).thenReturn(
			new IdentityFacade.UserDto(
				user.id(),
				"new@example.com",
				"New Name",
				user.role(),
				user.status(),
				user.lastLoginAt(),
				user.createdAt(),
				Instant.parse("2026-06-09T11:00:00Z")
			)
		);

		mockMvc.perform(put("/api/v1/users/me")
				.with(user("engineer@example.com").roles("ENGINEER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email": "new@example.com",
					  "displayName": "New Name"
					}
					"""))
			.andExpect(status().isOk());
	}

	@Test
	void engineerCanChangeOwnPassword() throws Exception {
		IdentityFacade.UserDto user = userDto();
		when(identityFacade.findUserByEmail("engineer@example.com")).thenReturn(user);
		doNothing().when(identityFacade).changePassword(user.id(), "old-password", "new-password");

		mockMvc.perform(put("/api/v1/users/me/password")
				.with(user("engineer@example.com").roles("ENGINEER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "oldPassword": "old-password",
					  "newPassword": "new-password"
					}
					"""))
			.andExpect(status().isOk());
	}

	@Test
	void engineerCannotAccessAdminUserManagement() throws Exception {
		mockMvc.perform(get("/api/v1/users/{id}", UUID.randomUUID())
				.with(user("engineer@example.com").roles("ENGINEER")))
			.andExpect(status().isForbidden());
	}

	@Test
	void adminCanAccessAdminUserManagement() throws Exception {
		when(identityFacade.findUserById(any(UUID.class))).thenReturn(userDto());

		mockMvc.perform(get("/api/v1/users/{id}", UUID.randomUUID())
				.with(user("admin@example.com").roles("ADMIN")))
			.andExpect(status().isOk());
	}

	private static IdentityFacade.UserDto userDto() {
		Instant now = Instant.parse("2026-06-09T10:00:00Z");
		return new IdentityFacade.UserDto(
			UUID.fromString("00000000-0000-0000-0000-000000000001"),
			"engineer@example.com",
			"Engineer",
			"ENGINEER",
			"ACTIVE",
			now,
			now,
			now
		);
	}
}
```

- [ ] **Step 3: Verify the expected failure**

Run:

```sh
cd apps/backend && ./mvnw -Dtest=SecurityConfigTest test
```

Expected: FAIL before the matcher change because at least `PUT /api/v1/users/me/password` is covered by `/api/v1/users/**` and requires `ADMIN`.

- [ ] **Step 4: Update the authorization matcher order**

Change `SecurityConfig.securityFilterChain` so self-service routes are authenticated before the admin-only user management matcher:

```java
.requestMatchers("/api/v1/users/me", "/api/v1/users/me/**").authenticated()
.requestMatchers("/api/v1/users/**").hasRole("ADMIN")
```

Keep `/api/v1/auth/**`, OpenAPI, and Swagger as `permitAll`.

- [ ] **Step 5: Verify the task**

Run:

```sh
cd apps/backend && ./mvnw -Dtest=SecurityConfigTest test
```

Expected: PASS. `ENGINEER` can read own profile, update own profile, and change own password; `ENGINEER` cannot access `/api/v1/users/{id}`; `ADMIN` can access `/api/v1/users/{id}`.

Then run:

```sh
cd apps/backend && ./mvnw test
```

Expected: PASS for existing backend tests plus `SecurityConfigTest`.

## Task 2: Add Auth Session User Update Support

**Files:**

- Modify: `apps/frontend/src/features/auth/auth-context.ts`
- Modify: `apps/frontend/src/features/auth/AuthProvider.tsx`
- Modify: `apps/frontend/src/shared/layouts/AppLayout.test.tsx`

- [ ] **Step 1: Extend the auth context contract**

In `auth-context.ts`, introduce `UserResponse` from generated API types and add `updateSessionUser`:

```ts
type UserResponse = components["schemas"]["UserResponse"];

export type AuthContextValue = {
  session: AuthSession | null;
  isInitializing: boolean;
  login: (credentials: LoginRequest) => Promise<AuthSession>;
  logout: () => void;
  updateSessionUser: (user: UserResponse) => void;
};
```

- [ ] **Step 2: Implement persistent user replacement**

In `AuthProvider.tsx`, add:

```ts
const updateSessionUser = useCallback((user: UserResponse) => {
  setSession(currentSession => {
    if (!currentSession) {
      return currentSession;
    }

    const nextSession = { ...currentSession, user };
    storeSession(nextSession);
    return nextSession;
  });
}, []);
```

Add `UserResponse` type alias from `components["schemas"]["UserResponse"]`, include `updateSessionUser` in the context value, and include it in the `useMemo` dependency list.

- [ ] **Step 3: Update existing auth test fixtures**

In `AppLayout.test.tsx`, add `updateSessionUser: vi.fn()` to each `AuthContextValue` fixture. Keep the session user values unchanged.

- [ ] **Step 4: Verify the task**

Run:

```sh
cd apps/frontend && npm run test:run -- AppLayout.test.tsx
```

Expected: PASS. Existing AppLayout tests compile and continue to pass with the extended context.

## Task 3: Implement Profile API Adapter And Page

**Files:**

- Create: `apps/frontend/src/features/profile/profile-api.ts`
- Create: `apps/frontend/src/features/profile/ProfilePage.tsx`
- Create: `apps/frontend/src/features/profile/ProfilePage.test.tsx`
- Modify: `apps/frontend/src/app/router.tsx`
- Modify: `apps/frontend/src/shared/layouts/UserMenu.tsx`
- Modify: `apps/frontend/src/shared/layouts/Topbar.test.tsx`
- Modify: `apps/frontend/src/shared/layouts/AppLayout.test.tsx`

- [ ] **Step 1: Add the profile API adapter**

Create `profile-api.ts` with generated DTO aliases and response-envelope parsing:

```ts
import { apiClient } from "@/api/client";
import type { components } from "@/api/generated/api-types";

export type UserResponse = components["schemas"]["UserResponse"];
export type UpdateUserRequest = components["schemas"]["UpdateUserRequest"];
export type ChangePasswordRequest =
  components["schemas"]["ChangePasswordRequest"];

type ApiEnvelope<T> = {
  data?: T;
  message?: string;
  success?: boolean;
};

function requireData<T>(response: ApiEnvelope<T>, fallbackMessage: string): T {
  if (response.data === undefined || response.data === null) {
    throw new Error(response.message || fallbackMessage);
  }

  return response.data;
}

export async function getProfile(): Promise<UserResponse> {
  const response = await apiClient.get<ApiEnvelope<UserResponse>>("/users/me");
  return requireData(response.data, "The profile response is incomplete.");
}

export async function updateProfile(
  request: UpdateUserRequest
): Promise<UserResponse> {
  const response = await apiClient.put<ApiEnvelope<UserResponse>>(
    "/users/me",
    request
  );
  return requireData(response.data, "The profile update response is incomplete.");
}

export async function changePassword(
  request: ChangePasswordRequest
): Promise<void> {
  await apiClient.put<ApiEnvelope<null>>("/users/me/password", request);
}
```

- [ ] **Step 2: Add focused Profile page tests first**

Create `ProfilePage.test.tsx` with `vi.mock("@/features/profile/profile-api", () => ({ getProfile, updateProfile, changePassword }))` and an `AuthContext.Provider` wrapper. Cover these cases:

- renders display name, email, role, status, user ID, `lastLoginAt`, `createdAt`, and `updatedAt`;
- renders `Not available` for missing date fields;
- validates display name and email are non-empty before save;
- submits `PUT /users/me` through `updateProfile`, calls `updateSessionUser`, and updates visible identity;
- validates password confirmation before API call;
- calls `changePassword` with `{ oldPassword, newPassword }` and clears password fields after success.
- renders a retryable non-secret error state when `getProfile` rejects.
- renders a permission-safe `403` state when profile access is forbidden.
- treats `401` as owned by the existing auth/interceptor flow; the page should not show token contents or raw backend details.

Use a fake user:

```ts
const profile = {
  id: "00000000-0000-0000-0000-000000000001",
  email: "engineer@example.com",
  displayName: "Engineer",
  role: "ENGINEER",
  status: "ACTIVE",
  lastLoginAt: "2026-06-09T10:00:00Z",
  createdAt: "2026-06-01T09:00:00Z",
  updatedAt: "2026-06-08T11:00:00Z"
};
```

- [ ] **Step 3: Verify the expected failure**

Run:

```sh
cd apps/frontend && npm run test:run -- ProfilePage.test.tsx
```

Expected: FAIL because `ProfilePage.tsx` does not exist yet.

- [ ] **Step 4: Implement `ProfilePage.tsx`**

Create a route component that:

- exports `Component` for React Router lazy route compatibility, matching existing route modules such as `DashboardPage.tsx` and `LiveLogsPage.tsx`;
- renders `<PageHeader title="Profile" />`;
- calls `getProfile()` on mount;
- stores loading, error, profile, save status, and password status in local state;
- renders identity summary, account metadata, profile edit form, password form, and role/status explanation panel;
- formats missing date values as `Not available`;
- validates display name, email, and password confirmation client-side;
- calls `updateSessionUser(updatedUser)` after successful profile update;
- clears password fields after successful password change;
- uses non-secret generic error messages from caught errors.
- maps forbidden profile reads to a permission-safe message such as `You do not have permission to view this profile.`;
- keeps unauthorized/session-expired errors generic because redirect/logout handling belongs to the existing auth layer.

Use CSS classes based on existing tokens such as `bg-surface`, `bg-surface-raised`, `border-border`, `text-text`, `text-muted`, `text-primary`, `text-success`, `text-warning`, and `text-error`.

- [ ] **Step 5: Add the authenticated route**

In `apps/frontend/src/app/router.tsx`, add the profile route under `AppLayout`:

```tsx
{
  path: "/profile",
  lazy: () => import("@/features/profile/ProfilePage")
}
```

- [ ] **Step 6: Wire the account menu to Profile**

In `UserMenu.tsx`:

- import `Link` from `react-router-dom`;
- replace the inert profile `<button>` with a `<Link to="/profile" role="menuitem">`;
- change the label from `Hồ sơ cá nhân` to `Profile`;
- close the menu on link click.

- [ ] **Step 7: Update layout tests for router context and navigation**

In `Topbar.test.tsx`, wrap the rendered topbar in `MemoryRouter` because `UserMenu` now renders a `Link`. Add an assertion that clicking `Profile` changes the test route to `/profile` or renders a location output with `/profile`.

In `AppLayout.test.tsx`, add `updateSessionUser: vi.fn()` to the auth fixture from Task 2 and update any profile menu label expectation from Vietnamese to `Profile`.

- [ ] **Step 8: Verify the task**

Run:

```sh
cd apps/frontend && npm run test:run -- ProfilePage.test.tsx Topbar.test.tsx AppLayout.test.tsx
```

Expected: PASS. Profile behavior works, the account menu navigates to `/profile`, logout still works, and AppLayout tests still pass.

## Task 4: Implement Overview Data Contract And Dashboard UI

**Files:**

- Create: `apps/frontend/src/features/dashboard/overview-types.ts`
- Create: `apps/frontend/src/features/dashboard/overview-adapter.ts`
- Create: `apps/frontend/src/features/dashboard/components/OverviewMetricCard.tsx`
- Create: `apps/frontend/src/features/dashboard/components/PipelineStrip.tsx`
- Create: `apps/frontend/src/features/dashboard/components/LogVolumeChart.tsx`
- Create: `apps/frontend/src/features/dashboard/components/NoisyApplicationsTable.tsx`
- Create: `apps/frontend/src/features/dashboard/components/RecentCriticalAlerts.tsx`
- Create: `apps/frontend/src/features/dashboard/components/DemoReadinessPanel.tsx`
- Modify: `apps/frontend/src/features/dashboard/DashboardPage.tsx`
- Create: `apps/frontend/src/features/dashboard/DashboardPage.test.tsx`

- [ ] **Step 1: Define Overview types**

Create `overview-types.ts`:

```ts
export type LogLevel = "INFO" | "WARN" | "ERROR" | "CRITICAL";

export type PipelineState =
  | "healthy"
  | "degraded"
  | "delayed"
  | "offline"
  | "unknown";

export type OverviewMetric = {
  id: string;
  label: string;
  value: string;
  trend?: string;
  tone: "neutral" | "success" | "warning" | "error";
};

export type PipelineStep = {
  id: string;
  label: string;
  state: PipelineState;
  detail: string;
};

export type LogVolumePoint = {
  time: string;
  INFO: number;
  WARN: number;
  ERROR: number;
  CRITICAL: number;
};

export type NoisyApplication = {
  id: string;
  name: string;
  environment: string;
  totalLogs: number;
  errorCount: number;
  criticalCount: number;
  errorRate: string;
  lastSeen: string;
};

export type CriticalAlertSummary = {
  id: string;
  severity: "ERROR" | "CRITICAL";
  application: string;
  fingerprint: string;
  message: string;
  occurrences: number;
  lastSeen: string;
  deliveryState: string;
};

export type DemoReadiness = {
  accepted: number;
  rejected: number;
  duration: string;
  p95AckLatency: string;
  streamStatus: string;
  buffered: number;
  dropped: number;
};

export type OverviewSnapshot = {
  metrics: OverviewMetric[];
  pipeline: PipelineStep[];
  volume: LogVolumePoint[];
  noisyApplications: NoisyApplication[];
  criticalAlerts: CriticalAlertSummary[];
  demoReadiness: DemoReadiness;
  authorizedApplications: number;
};
```

- [ ] **Step 2: Add the mock adapter**

Create `overview-adapter.ts` with `getOverviewSnapshot(): Promise<OverviewSnapshot>`. Return deterministic fake operational data using fake application names such as `checkout-api`, `billing-worker`, and `identity-service`. Do not include real credentials, API keys, personal data, connection strings, or raw request bodies.

- [ ] **Step 3: Add focused Dashboard tests first**

Create `DashboardPage.test.tsx` with tests that render `Component` and assert:

- page title is `Overview`;
- KPI labels include `Logs accepted/min`, `Error rate`, `Critical alerts`, `Active applications`, and `Processing lag`;
- pipeline labels include `Ingestion API`, `Kafka logs.raw`, `Worker`, `ClickHouse`, `WebSocket`, and `Alerting / Telegram`;
- chart section has accessible label `Log volume by level`;
- noisy applications and recent critical alerts render adapter data;
- demo readiness renders `500 logs / 2 seconds`.
- adapter rejection renders a non-secret error message and a retry button;
- an empty adapter snapshot renders useful empty states for noisy applications and critical alerts without removing the KPI and pipeline sections.
- a loading state renders before the adapter resolves.
- a no-authorized-applications snapshot renders a clear empty state without implying backend failure.
- pipeline states `degraded`, `offline`, and `unknown` render distinct labels or tones.
- KPI and pipeline sections stay visible when operational lists are empty.

- [ ] **Step 4: Verify the expected failure**

Run:

```sh
cd apps/frontend && npm run test:run -- DashboardPage.test.tsx
```

Expected: FAIL because the dashboard still renders the placeholder content.

- [ ] **Step 5: Implement reusable Overview components**

Implement the components with these responsibilities:

- `OverviewMetricCard.tsx`: render one compact metric card.
- `PipelineStrip.tsx`: render ordered flow cards and state labels.
- `LogVolumeChart.tsx`: render an accessible lightweight bar/area representation from `LogVolumePoint[]`.
- `NoisyApplicationsTable.tsx`: render the top noisy applications table.
- `RecentCriticalAlerts.tsx`: render alert summary cards.
- `DemoReadinessPanel.tsx`: render assignment demo metrics without claiming a real load test passed.

Use shared Tailwind tokens and semantic colors. Keep chart rendering dependency-free unless using existing `echarts-for-react` is clearly simpler during execution.

- [ ] **Step 6: Compose the Dashboard page**

Update `DashboardPage.tsx` so it:

- renders `<PageHeader title="Overview" actions={<button type="button">Refresh</button>} />` or an equivalent concrete refresh action;
- loads the snapshot from `getOverviewSnapshot()` in `useEffect`;
- shows a refresh button in the page header;
- renders loading, error, and content states;
- keeps KPI and pipeline structure visible for an empty snapshot while rendering empty messages for noisy applications and recent alerts;
- renders a no-authorized-applications state when `authorizedApplications === 0`;
- renders `degraded`, `offline`, and `unknown` pipeline states with distinct labels or tones;
- composes the KPI row, pipeline strip, chart, noisy applications table, alerts panel, and demo readiness panel.

- [ ] **Step 7: Verify the task**

Run:

```sh
cd apps/frontend && npm run test:run -- DashboardPage.test.tsx
```

Expected: PASS. The Overview page renders all spec-defined sections from adapter data.

## Task 5: Implement Live Logs Data Contract And Hybrid Stream UI

**Files:**

- Create: `apps/frontend/src/features/live-logs/live-logs-types.ts`
- Create: `apps/frontend/src/features/live-logs/live-logs-adapter.ts`
- Create: `apps/frontend/src/features/live-logs/components/LiveLogsToolbar.tsx`
- Create: `apps/frontend/src/features/live-logs/components/LiveLogFilters.tsx`
- Create: `apps/frontend/src/features/live-logs/components/LiveLogTable.tsx`
- Create: `apps/frontend/src/features/live-logs/components/LogDetailDrawer.tsx`
- Modify: `apps/frontend/src/features/live-logs/LiveLogsPage.tsx`
- Create: `apps/frontend/src/features/live-logs/LiveLogsPage.test.tsx`

- [ ] **Step 1: Define Live Logs types**

Create `live-logs-types.ts`:

```ts
export type LogLevel = "INFO" | "WARN" | "ERROR" | "CRITICAL";
export type LiveConnectionState =
  | "live"
  | "paused"
  | "connecting"
  | "reconnecting"
  | "disconnected"
  | "error";

export type ApplicationOption = {
  id: string;
  name: string;
};

export type LiveLogEntry = {
  id: string;
  timestamp: string;
  applicationId: string;
  applicationName: string;
  level: LogLevel;
  message: string;
  traceId?: string;
  eventId?: string;
  ingestionId?: string;
  source?: string;
  host?: string;
  environment?: string;
  attributes?: Record<string, string>;
};

export type LiveLogFilters = {
  applicationId: string;
  level: "ALL" | LogLevel;
  keyword: string;
  traceId: string;
};

export type LiveLogSnapshot = {
  applications: ApplicationOption[];
  entries: LiveLogEntry[];
  connectionState: LiveConnectionState;
  buffered: number;
  dropped: number;
};
```

- [ ] **Step 2: Add the mock adapter**

Create `live-logs-adapter.ts` with:

- `getInitialLiveLogSnapshot(): Promise<LiveLogSnapshot>`;
- `filterLiveLogEntries(entries: LiveLogEntry[], filters: LiveLogFilters): LiveLogEntry[]`;
- `MAX_VISIBLE_LOGS = 300`;
- deterministic fake entries with varied levels, applications, trace IDs, optional missing fields, and safe non-sensitive messages.

The adapter should not claim a real WebSocket connection exists.

- [ ] **Step 3: Add focused Live Logs tests first**

Create `LiveLogsPage.test.tsx` to verify:

- page title is `Live Logs`;
- connection status label renders;
- application, level, keyword, and trace ID filters reduce visible rows;
- pause/resume toggles status and does not clear rows;
- clear removes visible rows;
- clicking a row opens the detail drawer with full message, event ID, ingestion ID, trace ID, and attributes;
- empty filter state renders a reset action.
- zero application options render the no-authorized-applications state;
- `disconnected` and `error` connection states render distinct status labels and keep the current rows visible;
- a rejected initial snapshot renders a non-secret error message and retry action.
- over-cap adapter input renders no more than `MAX_VISIBLE_LOGS` rows;
- buffered and dropped counts render in the toolbar when the adapter reports non-zero values.
- input with `MAX_VISIBLE_LOGS + N` entries and zero adapter-reported counts still displays the client-side overflow count so hidden rows are visible to the user.

- [ ] **Step 4: Verify the expected failure**

Run:

```sh
cd apps/frontend && npm run test:run -- LiveLogsPage.test.tsx
```

Expected: FAIL because the page still renders placeholder content.

- [ ] **Step 5: Implement Live Logs components**

Implement:

- `LiveLogsToolbar.tsx`: status badge, pause/resume button, clear button, buffered and dropped counts.
- `LiveLogFilters.tsx`: controlled application select, level select, keyword input, trace ID input, and reset button.
- `LiveLogTable.tsx`: dense table with severity bar, timestamp, application, level badge, monospaced message, trace ID, and source/host.
- `LogDetailDrawer.tsx`: right-side drawer with full row detail and close button.

Use `button` semantics and accessible labels for all actions.

- [ ] **Step 6: Compose `LiveLogsPage.tsx`**

Update `LiveLogsPage.tsx` so it:

- renders `<PageHeader title="Live Logs" actions={<LiveLogsToolbar connectionState={connectionState} paused={paused} buffered={buffered} dropped={dropped} onTogglePause={handleTogglePause} onClear={handleClear} />} />`;
- loads the initial snapshot from the adapter;
- tracks filters, selected row, paused state, visible rows, and cleared state;
- caps visible rows with `MAX_VISIBLE_LOGS`;
- combines adapter-reported `buffered`/`dropped` counts with client-side overflow from rows hidden by `MAX_VISIBLE_LOGS`;
- renders no-authorized-applications state when `applications.length === 0`;
- renders empty filter state with reset action;
- renders disconnected and error states without clearing already loaded rows;
- renders a retryable non-secret error state when the initial snapshot fails;
- keeps existing rows visible when paused.

- [ ] **Step 7: Verify the task**

Run:

```sh
cd apps/frontend && npm run test:run -- LiveLogsPage.test.tsx
```

Expected: PASS. Live Logs supports the hybrid stream interactions and edge states from the spec.

## Task 6: Run Frontend Regression Tests And Polish Integration

**Files:**

- Modify: files from Tasks 2 through 5 only when a verification failure shows an integration issue in those files.

- [ ] **Step 1: Run focused frontend page tests**

Run:

```sh
cd apps/frontend && npm run test:run -- ProfilePage.test.tsx DashboardPage.test.tsx LiveLogsPage.test.tsx Topbar.test.tsx AppLayout.test.tsx
```

Expected: PASS.

- [ ] **Step 2: Run all frontend tests**

Run:

```sh
cd apps/frontend && npm run test:run
```

Expected: PASS.

- [ ] **Step 3: Run frontend lint**

Run:

```sh
cd apps/frontend && npm run lint
```

Expected: PASS with no lint errors.

- [ ] **Step 4: Run frontend build**

Run:

```sh
cd apps/frontend && npm run build
```

Expected: PASS. TypeScript and Vite build the frontend successfully.

- [ ] **Step 5: Manually verify shell integration**

Run:

```sh
make frontend
```

Expected: Vite starts on `http://localhost:5173`. While signed in with a locally available identity session:

- `/` shows the new Overview page inside `AppLayout`;
- `/logs` shows Live Logs inside `AppLayout`;
- account menu `Profile` navigates to `/profile`;
- `/profile` shows identity metadata, edit form, and change-password form;
- no page renders a second sidebar or header.

Stop the dev server after verification.

## Task 7: Run Backend Regression And Full Project Checks

**Files:**

- Modify: `apps/backend/pom.xml` only when Task 1 verification shows the test dependency is missing or misplaced.
- Modify: `apps/backend/src/main/java/com/vdt/log_monitoring/shared/security/SecurityConfig.java` only when Task 1 verification shows matcher order is wrong.
- Modify: `apps/backend/src/test/java/com/vdt/log_monitoring/shared/security/SecurityConfigTest.java` only when the focused security test has a compile or assertion issue.

- [ ] **Step 1: Run focused backend security test**

Run:

```sh
cd apps/backend && ./mvnw -Dtest=SecurityConfigTest test
```

Expected: PASS.

- [ ] **Step 2: Run backend tests**

Run:

```sh
cd apps/backend && ./mvnw test
```

Expected: PASS.

- [ ] **Step 3: Run root-level available checks**

Run:

```sh
make test
make lint
```

Expected: `make test` passes backend tests and `make lint` passes frontend ESLint.

- [ ] **Step 4: Run the full build**

```sh
make build
```

Expected: backend package and frontend production build both pass. If external services or dependency downloads block this check, report the blocker and the completed focused checks.

## Final Verification

Run these commands before marking execution complete:

```sh
cd apps/backend && ./mvnw -Dtest=SecurityConfigTest test
cd apps/backend && ./mvnw test
cd apps/frontend && npm run test:run
cd apps/frontend && npm run lint
cd apps/frontend && npm run build
```

Expected final result:

- all backend tests pass;
- all frontend tests pass;
- frontend lint passes;
- frontend build passes;
- Profile self-service password route is authenticated for `ENGINEER` and admin user management remains admin-only;
- Overview, Live Logs, and Profile render inside the existing shared shell.

## Spec Coverage

| Spec requirement / acceptance criterion | Covered by tasks | Verification |
| --- | --- | --- |
| Use existing `AppLayout`, `PageHeader`, sidebar, topbar, and auth context | Tasks 3, 4, 5 | `AppLayout.test.tsx`, `Topbar.test.tsx`, page tests |
| Do not create a second page shell | Tasks 3, 4, 5 | Manual shell integration in Task 6 |
| Use high-density dark enterprise visuals and shared tokens | Tasks 3, 4, 5 | Page tests plus visual/manual verification |
| Data through feature-local adapters | Tasks 4, 5 and `profile-api.ts` in Task 3 | `DashboardPage.test.tsx`, `LiveLogsPage.test.tsx`, `ProfilePage.test.tsx` |
| Mock data isolated behind adapters | Tasks 4, 5 | Adapter files and page tests |
| Backend auth allows `/api/v1/users/me/**` for authenticated users | Task 1 | `SecurityConfigTest` |
| Admin-only `/api/v1/users/{id}` remains admin-only | Task 1 | `SecurityConfigTest` |
| Overview focuses on realtime ingestion demo readiness and application error visibility | Task 4 | `DashboardPage.test.tsx` |
| Overview does not become detailed System Operations | Task 4 | Component scope and manual review |
| Overview KPI, pipeline, trend, noisy apps, critical alerts, demo readiness | Task 4 | `DashboardPage.test.tsx` |
| Live Logs hybrid dense table/stream design | Task 5 | `LiveLogsPage.test.tsx` |
| Live Logs filters application, level, keyword, trace ID | Task 5 | `LiveLogsPage.test.tsx` |
| Live Logs pause/resume, clear, connection status, row drawer | Task 5 | `LiveLogsPage.test.tsx` |
| Live Logs high-volume smoothness by bounded visible buffer | Task 5 | `MAX_VISIBLE_LOGS` adapter/page assertions |
| Profile reachable from account menu and rendered inside shell | Task 3 | `Topbar.test.tsx`, `AppLayout.test.tsx`, manual verification |
| Profile uses only `UserResponse` fields for account metadata | Task 3 | `ProfilePage.test.tsx` |
| Profile supports display name/email update and password change | Tasks 1, 2, 3 | `ProfilePage.test.tsx`, `SecurityConfigTest` |
| No device/session/IP/browser/logout-other-devices feature | Task 3 | Profile page test scope and manual review |
| Loading, empty, error, authorization, degraded states | Tasks 3, 4, 5 | Page tests and manual verification |
| No claim that logs/realtime/alerting backend is implemented | Tasks 4, 5 | Mock adapter naming and UI copy review |
| No sensitive mock data | Tasks 4, 5 | Manual fixture review and final verification |
