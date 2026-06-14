# Identity Application Access Design

## Problem

The backend has an `identity` module for users, roles, authentication, and
profile management. The broader system requirements also place monitored
applications, application API keys, and user-to-application permissions inside
the same `identity` module. These capabilities are needed before log ingestion,
log search, dashboard filters, and realtime subscriptions can safely scope data
by application.

The current implementation does not yet provide application management,
application access assignment, or an internal API-key verification contract.
Frontend pages also need an endpoint that returns only the applications the
current user can see, so engineers cannot select applications outside their
scope.

## Goal

Extend the `identity` module design so it owns:

- admin user creation and user administration;
- monitored application records;
- application API keys;
- user-to-application access grants;
- public module facades for user/auth operations and application access checks.

The design keeps the backend as a modular monolith. It does not introduce a new
`applications` business module.

## Scope

### In Scope

- Backend REST contracts for admin user creation and application management.
- Backend REST contracts for assigning applications to users.
- Authenticated application list API for the current user.
- Internal facade for API-key verification and application access checks.
- PostgreSQL schema additions under `identity`.
- Security rules for admin-only and authenticated application routes.
- Testing expectations for service, facade, controller, and security behavior.

### Out Of Scope

- Implementing log ingestion, log search, WebSocket subscriptions, alert rules,
  or ClickHouse queries.
- Building the frontend application management UI.
- Introducing a separate `applications` module.
- Public REST endpoint for API-key verification.
- Redis caching for application metadata or permissions.
- Audit log/history UI for permission changes.

## Requirements

### Ownership

- `identity` owns users, roles, application records, application API keys, and
  user-to-application access.
- Other modules must call `identity` public facades and must not import
  identity repositories, entities, or persistence adapters.
- Cross-module contracts return DTO/value objects only.

### Roles And Permissions

- `ADMIN` can manage all users, all applications, API keys, and application
  access grants.
- `ADMIN` can see all active applications without explicit access rows.
- `ENGINEER` can see or use only applications assigned through
  `identity.user_application_access`.
- Application access levels are:
  - `VIEW`: can view/search/subscribe to application operational data.
  - `MANAGE`: includes `VIEW` and can manage application-scoped configuration
    in future features such as alert rules.

### API Keys

- Each application can have multiple API keys.
- API keys have a human-readable `name`, `key_prefix`, hash, status, optional
  expiry, and lifecycle timestamps.
- Raw API keys are returned only once when created or rotated.
- The database stores only API-key hashes, never raw API keys.
- API-key verification is internal Java facade behavior, not a public REST API.

### Frontend Visibility

- The frontend must use an authenticated "my applications" API to populate
  application dropdowns and filters.
- Engineers receive only active applications they can view.
- Admins receive all active applications.
- An engineer with no assigned applications receives an empty list, not an
  authorization error.

## Proposed Design

## Backend Module Layout

Applications remain inside `modules.identity`:

```text
com.vdt.log_monitoring
├── api
│   └── identity
│       ├── AuthenticationController.java
│       ├── UserController.java
│       ├── UserAdminController.java
│       ├── ApplicationController.java
│       ├── ApplicationAccessController.java
│       └── dto/
└── modules
    └── identity
        ├── api
        │   ├── IdentityFacade.java
        │   └── ApplicationAccessFacade.java
        ├── application
        │   ├── AuthService.java
        │   ├── UserService.java
        │   ├── ApplicationService.java
        │   ├── ApplicationAccessService.java
        │   ├── ApiKeyService.java
        │   └── IdentityException.java
        ├── model
        │   ├── User.java
        │   ├── MonitoredApplication.java
        │   ├── UserApplicationAccess.java
        │   ├── ApplicationApiKey.java
        │   ├── ApplicationStatus.java
        │   ├── ApplicationAccessLevel.java
        │   └── ApiKeyStatus.java
        └── infrastructure
            └── persistence
                ├── UserEntity.java
                ├── ApplicationEntity.java
                ├── UserApplicationAccessEntity.java
                ├── ApplicationApiKeyEntity.java
                └── repositories...
```

This keeps the module boundary aligned with the existing project requirement:
application identity and access policy belong to `identity`, not to a separate
business module.

## Public Facades

### `IdentityFacade`

`IdentityFacade` remains focused on user/auth/profile use cases:

```java
public interface IdentityFacade {
    UserDto createUser(String email, String rawPassword, String displayName, String role);
    List<UserDto> findUsers();
    UserDto findUserById(UUID id);
    UserDto findUserByEmail(String email);
    UserDto updateProfile(UUID id, String email, String displayName);
    void changePassword(UUID id, String oldPassword, String newPassword);
    UserDto changeRole(UUID id, String role);
    UserDto changeStatus(UUID id, String status);
    TokenPairDto authenticate(String email, String rawPassword);
    TokenPairDto refresh(String refreshToken);
}
```

The first implementation can use an unpaginated `findUsers()` method because
the project does not yet have a shared pagination contract. If pagination is
introduced later, it should be added consistently across admin list endpoints.

### `ApplicationAccessFacade`

`ApplicationAccessFacade` is a second public contract inside the same
`identity` module. Logs, alerting, realtime, and top-level identity controllers
use this facade instead of identity repositories.

Initial facade methods:

```java
public interface ApplicationAccessFacade {
    ApplicationDto createApplication(CreateApplicationCommand command);
    ApplicationDto updateApplication(UUID applicationId, UpdateApplicationCommand command);
    ApplicationDto changeApplicationStatus(UUID applicationId, String status);
    ApplicationDto findApplicationById(UUID applicationId);
    List<ApplicationDto> findAllApplications();
    List<ApplicationDto> findVisibleApplications(UUID userId, String role);

    List<ApplicationAccessDto> findUserApplicationAccess(UUID userId);
    List<ApplicationAccessDto> replaceUserApplicationAccess(
        UUID userId,
        List<ApplicationAccessGrantCommand> grants,
        UUID grantedBy
    );
    ApplicationAccessDto grantUserApplicationAccess(
        UUID userId,
        UUID applicationId,
        String accessLevel,
        UUID grantedBy
    );
    void removeUserApplicationAccess(UUID userId, UUID applicationId);

    ApiKeyCreationDto createApiKey(UUID applicationId, String name, Instant expiresAt, UUID createdBy);
    ApiKeyCreationDto rotateApiKey(UUID applicationId, UUID apiKeyId, UUID rotatedBy);
    void revokeApiKey(UUID applicationId, UUID apiKeyId, UUID revokedBy);

    ApiKeyVerificationDto verifyApplicationApiKey(String rawApiKey);
    boolean canViewApplication(UUID userId, UUID applicationId);
    boolean canManageApplication(UUID userId, UUID applicationId);
}
```

DTOs returned by the facade must not expose `key_hash` or raw API keys except
for `ApiKeyCreationDto.rawApiKey`, which is returned only on create/rotate.

## REST API Contracts

Use the existing route style rather than introducing `/api/v1/admin/**` in this
phase. Admin-only behavior is enforced by security and method authorization.

### Admin User APIs

```text
POST /api/v1/users
GET  /api/v1/users
GET  /api/v1/users/{id}
PUT  /api/v1/users/{id}/role
PUT  /api/v1/users/{id}/status
```

Notes:

- Existing `/api/v1/auth/register` can remain for open/self registration if the
  project keeps it, but admin user creation should be available through
  `POST /api/v1/users`.
- All routes in this admin user group require `ADMIN`.
- User status changes use existing statuses: `ACTIVE`, `DISABLED`, `LOCKED`.

### Application Management APIs

```text
POST /api/v1/applications
GET  /api/v1/applications
GET  /api/v1/applications/{id}
PUT  /api/v1/applications/{id}
PUT  /api/v1/applications/{id}/status
```

All routes above require `ADMIN`.

Suggested create request:

```json
{
  "name": "checkout-api",
  "displayName": "Checkout API",
  "description": "Checkout service logs"
}
```

Suggested response:

```json
{
  "id": "application-uuid",
  "name": "checkout-api",
  "displayName": "Checkout API",
  "description": "Checkout service logs",
  "status": "ACTIVE",
  "createdAt": "2026-06-10T00:00:00Z",
  "updatedAt": "2026-06-10T00:00:00Z"
}
```

### Current User Application APIs

```text
GET /api/v1/applications/me
```

Requires authentication.

Behavior:

- `ADMIN`: returns all active applications.
- `ENGINEER`: returns active applications assigned to the user.
- No assigned applications: returns `[]`.

This endpoint is the source for frontend filters and selectors. The frontend
must not infer allowed applications from local auth session data.

### User Application Access APIs

```text
GET    /api/v1/users/{userId}/applications
PUT    /api/v1/users/{userId}/applications
POST   /api/v1/users/{userId}/applications/{applicationId}
DELETE /api/v1/users/{userId}/applications/{applicationId}
```

All routes require `ADMIN`.

`PUT /api/v1/users/{userId}/applications` replaces the user's application
access list. This is convenient for an admin UI with multi-select or checkboxes.

Suggested replace request:

```json
{
  "grants": [
    {
      "applicationId": "application-uuid-1",
      "accessLevel": "VIEW"
    },
    {
      "applicationId": "application-uuid-2",
      "accessLevel": "MANAGE"
    }
  ]
}
```

`POST /api/v1/users/{userId}/applications/{applicationId}` grants or updates a
single application access row. It supports small admin actions without forcing
the frontend to send the full list.

Suggested single grant request:

```json
{
  "accessLevel": "VIEW"
}
```

### API Key Management APIs

```text
POST /api/v1/applications/{applicationId}/api-keys
GET  /api/v1/applications/{applicationId}/api-keys
POST /api/v1/applications/{applicationId}/api-keys/{apiKeyId}/rotate
POST /api/v1/applications/{applicationId}/api-keys/{apiKeyId}/revoke
```

All routes require `ADMIN`.

Suggested create request:

```json
{
  "name": "production-ingestion",
  "expiresAt": null
}
```

Suggested create/rotate response:

```json
{
  "id": "api-key-uuid",
  "applicationId": "application-uuid",
  "name": "production-ingestion",
  "keyPrefix": "lms_live_abc123",
  "rawApiKey": "lms_live_abc123.secret-value-shown-once",
  "status": "ACTIVE",
  "expiresAt": null,
  "createdAt": "2026-06-10T00:00:00Z"
}
```

List responses must omit `rawApiKey` and `keyHash`.

## Security Rules

Matcher order matters:

```text
/api/v1/auth/**                 permitAll
/v3/api-docs/**                 permitAll
/swagger-ui/**                  permitAll
/api/v1/users/me                authenticated
/api/v1/users/me/**             authenticated
/api/v1/applications/me         authenticated
/api/v1/users/**                ADMIN only
/api/v1/applications/**         ADMIN only
any other request               authenticated
```

Controllers should also use method-level authorization for admin operations so
tests cover both route matching and use-case intent.

## Data Model

Use PostgreSQL schema `identity`.

### `identity.applications`

Stores monitored applications that can send logs.

- `id UUID PRIMARY KEY`
- `name VARCHAR(100) NOT NULL`
- `display_name VARCHAR(150) NOT NULL`
- `description TEXT`
- `status VARCHAR(32) NOT NULL`
- `created_by UUID NOT NULL`
- `created_at TIMESTAMPTZ NOT NULL`
- `updated_at TIMESTAMPTZ NOT NULL`

Constraints:

- unique index on `LOWER(name)`;
- status check: `ACTIVE`, `INACTIVE`;
- `created_by` references `identity.users(id)` with `ON DELETE RESTRICT`.

### `identity.user_application_access`

Stores explicit access grants for non-admin users.

- `user_id UUID NOT NULL`
- `application_id UUID NOT NULL`
- `access_level VARCHAR(32) NOT NULL`
- `granted_by UUID NOT NULL`
- `created_at TIMESTAMPTZ NOT NULL`
- `updated_at TIMESTAMPTZ NOT NULL`

Constraints:

- primary key: `(user_id, application_id)`;
- index: `(application_id, user_id)`;
- access-level check: `VIEW`, `MANAGE`;
- user/application/granted_by FKs remain inside the `identity` schema;
- deleting a user or application cascades related access rows.

### `identity.application_api_keys`

Stores API-key metadata and hashes.

- `id UUID PRIMARY KEY`
- `application_id UUID NOT NULL`
- `name VARCHAR(100) NOT NULL`
- `key_prefix VARCHAR(32) NOT NULL`
- `key_hash VARCHAR(255) NOT NULL`
- `status VARCHAR(32) NOT NULL`
- `expires_at TIMESTAMPTZ`
- `last_used_at TIMESTAMPTZ`
- `created_by UUID NOT NULL`
- `created_at TIMESTAMPTZ NOT NULL`
- `revoked_at TIMESTAMPTZ`

Constraints:

- unique index on `key_prefix`;
- index on `(application_id, status)`;
- status check: `ACTIVE`, `REVOKED`, `EXPIRED`;
- `application_id` and `created_by` FKs remain inside `identity`.

`last_used_at` should not be updated for every log event. The first
implementation may leave it null or update it at a controlled interval.

## Data Flow

### Admin Creates Application

```text
Admin
  -> POST /api/v1/applications
  -> ApplicationController
  -> ApplicationAccessFacade.createApplication
  -> ApplicationService
  -> identity.applications
```

### Admin Assigns Applications To User

```text
Admin
  -> PUT /api/v1/users/{userId}/applications
  -> ApplicationAccessController
  -> ApplicationAccessFacade.replaceUserApplicationAccess
  -> validate target user exists
  -> validate applications exist
  -> replace identity.user_application_access rows
```

### Frontend Loads Visible Applications

```text
Authenticated user
  -> GET /api/v1/applications/me
  -> ApplicationController
  -> IdentityFacade.findUserByEmail(principal.name)
  -> ApplicationAccessFacade.findVisibleApplications(user.id, user.role)
  -> ADMIN: all active applications
  -> ENGINEER: assigned active applications
```

### Log Ingestion Verifies API Key

```text
External application
  -> Ingestion API with application API key
  -> logs module
  -> ApplicationAccessFacade.verifyApplicationApiKey(rawApiKey)
  -> valid result includes application id/name
  -> logs module accepts and publishes raw log to Kafka
```

### Log Query Or Realtime Checks User Access

```text
Authenticated user selects application
  -> logs/realtime module
  -> ApplicationAccessFacade.canViewApplication(userId, applicationId)
  -> allowed: query/subscribe
  -> denied: reject or filter application out
```

## Error Handling

Suggested identity error codes:

- `APPLICATION_NOT_FOUND`
- `APPLICATION_NAME_ALREADY_EXISTS`
- `APPLICATION_INACTIVE`
- `APPLICATION_ACCESS_NOT_FOUND`
- `INVALID_APPLICATION_ACCESS_LEVEL`
- `API_KEY_NOT_FOUND`
- `INVALID_API_KEY`
- `API_KEY_REVOKED`
- `API_KEY_EXPIRED`
- `USER_NOT_FOUND`
- `EMAIL_ALREADY_EXISTS`

Suggested HTTP mapping:

- duplicate user/application: `409 Conflict`
- invalid role/status/access level: `400 Bad Request`
- unknown user/application/API key id: `404 Not Found`
- authenticated user lacks application access: `403 Forbidden`
- invalid application API key at ingestion boundary: `401 Unauthorized`
- inactive application at ingestion boundary: `403 Forbidden`

Do not expose key hashes, raw secrets, or implementation details in error
messages.

## Edge Cases

- Admin lists applications when no application exists: return `[]`.
- Engineer has no assigned applications: `GET /api/v1/applications/me` returns
  `[]`.
- Admin does not need access rows to see or manage applications.
- Assigning access to an admin user is allowed but unnecessary; it must not
  reduce admin global access.
- Replacing a user's grants with an empty list removes all explicit
  application access.
- Assigning the same application twice in one request is rejected as
  `400 Bad Request`.
- Assigning access to an inactive application is allowed only if the admin API
  explicitly supports `includeInactive`; otherwise reject or require the
  application to be active. The first implementation should require active
  applications for grants.
- API key is valid but application is inactive: verification fails with
  `APPLICATION_INACTIVE`.
- API key is revoked or expired: verification fails and does not reveal whether
  the prefix exists.
- Raw API key shown on create/rotate is not recoverable later.

## Testing Notes

Backend service/facade tests:

- Admin can create application with unique `name`.
- Duplicate application `name` fails.
- Admin can create multiple API keys for one application.
- API key create/rotate returns raw key once and stores only hash.
- Revoked/expired API keys fail verification.
- Inactive applications fail API-key verification.
- `ADMIN` can see all active applications without access rows.
- `ENGINEER` sees only assigned active applications.
- `VIEW` allows `canViewApplication`; `MANAGE` allows both view and manage.
- Replace grants removes grants omitted from the request.
- Single grant can create or update access level.

Controller/security tests:

- `GET /api/v1/applications/me` is authenticated.
- `GET /api/v1/applications/me` matches before admin-only
  `/api/v1/applications/**`.
- Engineer cannot call admin application or user access routes.
- Admin can call application management and assignment routes.
- Existing `/api/v1/users/me/**` self-service access remains authenticated for
  all users.

Integration boundary tests:

- A logs ingestion unit test can mock `ApplicationAccessFacade` to verify API
  key behavior without depending on identity repositories.
- A realtime/log query unit test can mock `ApplicationAccessFacade` to verify
  application access filtering.

## Acceptance Criteria

- No new `applications` module is introduced.
- `identity` owns application records, API keys, and user-application access.
- Public contracts are split into `IdentityFacade` and
  `ApplicationAccessFacade`.
- Admin can create users through an admin-protected user route.
- Admin can create, update, list, activate, and deactivate applications.
- Admin can create, rotate, revoke, and list application API keys.
- Admin can grant, replace, list, and remove user application access.
- `GET /api/v1/applications/me` returns all active applications for admins and
  only assigned active applications for engineers.
- API-key verification is available through `ApplicationAccessFacade` and is
  not exposed as a public REST endpoint.
- Other modules can verify API keys and application access through the facade
  without importing identity persistence types.
- API keys and password values are never stored or returned in plain text
  outside one-time API-key create/rotate responses.
