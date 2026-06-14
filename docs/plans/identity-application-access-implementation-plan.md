# Identity Application Access Implementation Plan

**Source spec:** `docs/specs/identity-application-access-design.md`

**Goal:** Implement identity-owned application management, API keys, user-to-application access grants, and internal access/API-key verification contracts.

**Architecture:** Keep the system as a Spring Boot modular monolith. Application records, application API keys, and user-application permissions live inside the existing `identity` module and PostgreSQL `identity` schema. Split public identity contracts into `IdentityFacade` for user/auth operations and `ApplicationAccessFacade` for application, permission, and API-key checks.

**Tech stack:** Java 21, Spring Boot 3.5, Spring Web, Spring Security, Spring Data JPA, Flyway, PostgreSQL, Lombok, JUnit 5, AssertJ, MockMvc, Spring Security Test.

---

## File Map

- Create: `apps/backend/src/main/resources/db/migration/postgresql/V2__create_identity_applications_access_api_keys.sql` — create application, access-grant, and API-key tables in schema `identity`.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/model/ApplicationStatus.java` — application lifecycle enum.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/model/ApplicationAccessLevel.java` — application permission enum.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/model/ApiKeyStatus.java` — API-key lifecycle enum.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/model/MonitoredApplication.java` — domain model for monitored applications.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/model/UserApplicationAccess.java` — domain model for user application grants.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/model/ApplicationApiKey.java` — domain model for API-key metadata.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/infrastructure/persistence/ApplicationEntity.java` — JPA mapping for `identity.applications`.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/infrastructure/persistence/UserApplicationAccessId.java` — composite id for user application grants.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/infrastructure/persistence/UserApplicationAccessEntity.java` — JPA mapping for `identity.user_application_access`.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/infrastructure/persistence/ApplicationApiKeyEntity.java` — JPA mapping for `identity.application_api_keys`.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/infrastructure/persistence/ApplicationRepository.java` — Spring Data repository for applications.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/infrastructure/persistence/UserApplicationAccessRepository.java` — Spring Data repository for access grants.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/infrastructure/persistence/ApplicationApiKeyRepository.java` — Spring Data repository for API keys.
- Modify: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/application/IdentityException.java` — add application/access/API-key error codes.
- Modify: `apps/backend/src/main/java/com/vdt/log_monitoring/api/identity/IdentityExceptionHandler.java` — map duplicate errors to `409`, bad input to `400`, forbidden access to `403`, and missing resources to `404`.
- Modify: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/api/IdentityFacade.java` — add `findUsers()` to the user/auth facade.
- Modify: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/application/IdentityFacadeImpl.java` — implement `findUsers()`.
- Modify: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/application/UserService.java` — add unpaginated user listing.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/api/ApplicationAccessFacade.java` — public identity-module contract for applications, access grants, API keys, and permission checks.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/application/ApplicationService.java` — application CRUD and visible-application use cases.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/application/ApplicationAccessService.java` — grant, replace, remove, and permission-check use cases.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/application/ApiKeyService.java` — API-key create, rotate, revoke, list, and verification use cases.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/application/ApplicationAccessFacadeImpl.java` — facade implementation that composes application/access/API-key services.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/api/identity/UserAdminController.java` — admin user create/list/read/role/status routes.
- Modify: `apps/backend/src/main/java/com/vdt/log_monitoring/api/identity/UserController.java` — keep only authenticated self-service `/me` routes to avoid duplicate admin mappings.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/api/identity/ApplicationController.java` — application management and current-user visible application routes.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/api/identity/ApplicationAccessController.java` — admin routes for assigning applications to users.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/api/identity/ApplicationApiKeyController.java` — admin routes for application API-key management.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/api/identity/dto/ApplicationRequest.java` — create/update application request DTO.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/api/identity/dto/ApplicationStatusRequest.java` — application status request DTO.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/api/identity/dto/ApplicationResponse.java` — application response DTO.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/api/identity/dto/ApplicationAccessGrantRequest.java` — single grant request DTO.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/api/identity/dto/ReplaceApplicationAccessRequest.java` — replace-grants request DTO.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/api/identity/dto/ApplicationAccessResponse.java` — access-grant response DTO.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/api/identity/dto/CreateApiKeyRequest.java` — API-key create request DTO.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/api/identity/dto/ApiKeyResponse.java` — API-key metadata response DTO.
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/api/identity/dto/ApiKeyCreationResponse.java` — one-time raw API-key response DTO.
- Modify: `apps/backend/src/main/java/com/vdt/log_monitoring/shared/security/SecurityConfig.java` — allow authenticated `/api/v1/applications/me` before admin-only application routes.
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/identity/model/MonitoredApplicationTest.java` — application domain behavior.
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/identity/model/UserApplicationAccessTest.java` — access-level domain behavior.
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/identity/model/ApplicationApiKeyTest.java` — API-key lifecycle domain behavior.
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/identity/infrastructure/persistence/ApplicationEntityTest.java` — application entity mapping.
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/identity/infrastructure/persistence/UserApplicationAccessEntityTest.java` — access entity mapping.
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/identity/infrastructure/persistence/ApplicationApiKeyEntityTest.java` — API-key entity mapping.
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/identity/application/ApplicationAccessFacadeImplTest.java` — facade/service behavior for applications, grants, visible apps, and API keys.
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/api/identity/UserAdminControllerTest.java` — admin user REST contracts.
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/api/identity/ApplicationControllerTest.java` — application REST contracts and `/applications/me`.
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/api/identity/ApplicationAccessControllerTest.java` — user application assignment REST contracts.
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/api/identity/ApplicationApiKeyControllerTest.java` — API-key REST contracts.
- Modify: `apps/backend/src/test/java/com/vdt/log_monitoring/shared/security/SecurityConfigTest.java` — add application route matcher tests and update stubs for new facade methods.

## Task 1: Add Identity Storage Migration

**Files:**

- Create: `apps/backend/src/main/resources/db/migration/postgresql/V2__create_identity_applications_access_api_keys.sql`

- [x] **Step 1: Create the migration**

Create `V2__create_identity_applications_access_api_keys.sql` with:

```sql
CREATE TABLE identity.applications (
    id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    display_name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_applications PRIMARY KEY (id),
    CONSTRAINT fk_applications_created_by
        FOREIGN KEY (created_by)
        REFERENCES identity.users (id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_applications_name_not_blank
        CHECK (BTRIM(name) <> ''),
    CONSTRAINT ck_applications_display_name_not_blank
        CHECK (BTRIM(display_name) <> ''),
    CONSTRAINT ck_applications_status
        CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE UNIQUE INDEX uk_applications_name
    ON identity.applications (LOWER(name));

CREATE INDEX idx_applications_status
    ON identity.applications (status);

CREATE TABLE identity.user_application_access (
    user_id UUID NOT NULL,
    application_id UUID NOT NULL,
    access_level VARCHAR(32) NOT NULL,
    granted_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_user_application_access PRIMARY KEY (user_id, application_id),
    CONSTRAINT fk_user_application_access_user
        FOREIGN KEY (user_id)
        REFERENCES identity.users (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_user_application_access_application
        FOREIGN KEY (application_id)
        REFERENCES identity.applications (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_user_application_access_granted_by
        FOREIGN KEY (granted_by)
        REFERENCES identity.users (id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_user_application_access_level
        CHECK (access_level IN ('VIEW', 'MANAGE'))
);

CREATE INDEX idx_user_application_access_application_user
    ON identity.user_application_access (application_id, user_id);

CREATE TABLE identity.application_api_keys (
    id UUID NOT NULL,
    application_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    key_prefix VARCHAR(32) NOT NULL,
    key_hash VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    expires_at TIMESTAMPTZ,
    last_used_at TIMESTAMPTZ,
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMPTZ,

    CONSTRAINT pk_application_api_keys PRIMARY KEY (id),
    CONSTRAINT fk_application_api_keys_application
        FOREIGN KEY (application_id)
        REFERENCES identity.applications (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_application_api_keys_created_by
        FOREIGN KEY (created_by)
        REFERENCES identity.users (id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_application_api_keys_name_not_blank
        CHECK (BTRIM(name) <> ''),
    CONSTRAINT ck_application_api_keys_prefix_not_blank
        CHECK (BTRIM(key_prefix) <> ''),
    CONSTRAINT ck_application_api_keys_hash_not_blank
        CHECK (BTRIM(key_hash) <> ''),
    CONSTRAINT ck_application_api_keys_status
        CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED'))
);

CREATE UNIQUE INDEX uk_application_api_keys_prefix
    ON identity.application_api_keys (key_prefix);

CREATE INDEX idx_application_api_keys_application_status
    ON identity.application_api_keys (application_id, status);
```

- [x] **Step 2: Validate the project still compiles**

Run:

```sh
cd apps/backend && ./mvnw -DskipTests compile
```

Expected: PASS. This does not execute Flyway against PostgreSQL; migration execution is verified in Task 8 after starting local PostgreSQL.

## Task 2: Add Application Domain Models And Persistence Mapping

**Files:**

- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/model/ApplicationStatus.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/model/ApplicationAccessLevel.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/model/ApiKeyStatus.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/model/MonitoredApplication.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/model/UserApplicationAccess.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/model/ApplicationApiKey.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/infrastructure/persistence/ApplicationEntity.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/infrastructure/persistence/UserApplicationAccessId.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/infrastructure/persistence/UserApplicationAccessEntity.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/infrastructure/persistence/ApplicationApiKeyEntity.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/infrastructure/persistence/ApplicationRepository.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/infrastructure/persistence/UserApplicationAccessRepository.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/infrastructure/persistence/ApplicationApiKeyRepository.java`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/identity/model/MonitoredApplicationTest.java`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/identity/model/UserApplicationAccessTest.java`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/identity/model/ApplicationApiKeyTest.java`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/identity/infrastructure/persistence/ApplicationEntityTest.java`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/identity/infrastructure/persistence/UserApplicationAccessEntityTest.java`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/identity/infrastructure/persistence/ApplicationApiKeyEntityTest.java`

- [x] **Step 1: Add focused failing domain and mapping tests**

Create domain tests that assert:

- `MonitoredApplication.create(...)` trims `name`, `displayName`, and optional `description`; sets status `ACTIVE`; sets `createdAt` and `updatedAt` to the provided `Instant`.
- `MonitoredApplication.changeStatus(ApplicationStatus.INACTIVE, now)` updates status and `updatedAt`.
- `UserApplicationAccess.create(userId, applicationId, VIEW, grantedBy, now)` stores all ids, access level, and timestamps.
- `UserApplicationAccess.changeAccessLevel(MANAGE, now)` updates access level and `updatedAt`.
- `ApplicationApiKey.create(applicationId, name, keyPrefix, keyHash, expiresAt, createdBy, now)` stores metadata, status `ACTIVE`, and does not store a raw key.
- `ApplicationApiKey.revoke(now)` sets status `REVOKED` and `revokedAt`.
- `ApplicationApiKey.isUsable(now)` is false for `REVOKED`, `EXPIRED`, or expired timestamps.

Create entity mapping tests matching the style of `UserEntityTest`:

```java
MonitoredApplication restored = ApplicationEntity.fromModel(original).toModel();
assertThat(restored.getId()).isEqualTo(original.getId());
assertThat(restored.getName()).isEqualTo(original.getName());
assertThat(restored.getStatus()).isEqualTo(original.getStatus());
```

Repeat the same model-to-entity-to-model assertion pattern for access grants and API keys.

- [x] **Step 2: Verify expected failure**

Run:

```sh
cd apps/backend && ./mvnw -Dtest=MonitoredApplicationTest,UserApplicationAccessTest,ApplicationApiKeyTest,ApplicationEntityTest,UserApplicationAccessEntityTest,ApplicationApiKeyEntityTest test
```

Expected: FAIL because the new domain, entity, id, and repository classes do not exist yet.

- [x] **Step 3: Implement enums and domain models**

Create enums:

```java
public enum ApplicationStatus {
	ACTIVE,
	INACTIVE
}

public enum ApplicationAccessLevel {
	VIEW,
	MANAGE
}

public enum ApiKeyStatus {
	ACTIVE,
	REVOKED,
	EXPIRED
}
```

Implement domain models following `User` style:

- private constructor with Lombok `@Getter` and `@AllArgsConstructor(access = AccessLevel.PRIVATE)`;
- static `create(...)` and `restore(...)`;
- `requireText(...)` helper for nonblank strings;
- `touch(Instant now)` helper for updated timestamps.

Required domain behavior:

- `MonitoredApplication.create(name, displayName, description, createdBy, now)` uses `UUID.randomUUID()`, trims fields, status `ACTIVE`, and records `createdBy`.
- `MonitoredApplication.update(name, displayName, description, now)` updates editable fields and timestamp.
- `MonitoredApplication.changeStatus(status, now)` updates status and timestamp.
- `UserApplicationAccess.create(userId, applicationId, accessLevel, grantedBy, now)` records a composite identity and grant metadata.
- `UserApplicationAccess.changeAccessLevel(accessLevel, grantedBy, now)` updates access level, `grantedBy`, and `updatedAt`.
- `ApplicationApiKey.create(applicationId, name, keyPrefix, keyHash, expiresAt, createdBy, now)` stores only prefix/hash metadata.
- `ApplicationApiKey.revoke(now)` marks revoked.
- `ApplicationApiKey.markExpired(now)` marks expired.
- `ApplicationApiKey.isUsable(Instant now)` returns true only when status is `ACTIVE` and `expiresAt` is null or after `now`.

- [x] **Step 4: Implement JPA entities and repositories**

Create entities using the same JPA/Lombok style as `UserEntity`:

- `ApplicationEntity` maps table `applications`, schema `identity`, indexes `idx_applications_status`, a unique constraint named `uk_applications_name` on column `name`, `@Enumerated(EnumType.STRING)` status, and static `fromModel/toModel`.
- `UserApplicationAccessId` is `@Embeddable`, implements `Serializable`, and contains `UUID userId` and `UUID applicationId`.
- `UserApplicationAccessEntity` uses `@EmbeddedId`, columns `access_level`, `granted_by`, `created_at`, `updated_at`, and static `fromModel/toModel`.
- `ApplicationApiKeyEntity` maps table `application_api_keys`, schema `identity`, `@Enumerated(EnumType.STRING)` status, all timestamp columns, and static `fromModel/toModel`.

Create repositories:

```java
public interface ApplicationRepository extends JpaRepository<ApplicationEntity, UUID> {
	boolean existsByNameIgnoreCase(String name);
	Optional<ApplicationEntity> findByNameIgnoreCase(String name);
	List<ApplicationEntity> findByStatus(ApplicationStatus status);
}

public interface UserApplicationAccessRepository
		extends JpaRepository<UserApplicationAccessEntity, UserApplicationAccessId> {
	List<UserApplicationAccessEntity> findByIdUserId(UUID userId);
	List<UserApplicationAccessEntity> findByIdApplicationId(UUID applicationId);
	void deleteByIdUserId(UUID userId);
}

public interface ApplicationApiKeyRepository extends JpaRepository<ApplicationApiKeyEntity, UUID> {
	Optional<ApplicationApiKeyEntity> findByKeyPrefix(String keyPrefix);
	List<ApplicationApiKeyEntity> findByApplicationId(UUID applicationId);
	List<ApplicationApiKeyEntity> findByApplicationIdAndStatus(UUID applicationId, ApiKeyStatus status);
}
```

- [x] **Step 5: Verify task**

Run:

```sh
cd apps/backend && ./mvnw -Dtest=MonitoredApplicationTest,UserApplicationAccessTest,ApplicationApiKeyTest,ApplicationEntityTest,UserApplicationAccessEntityTest,ApplicationApiKeyEntityTest test
```

Expected: PASS.

## Task 3: Extend Identity Exceptions And User Admin Facade

**Files:**

- Modify: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/application/IdentityException.java`
- Modify: `apps/backend/src/main/java/com/vdt/log_monitoring/api/identity/IdentityExceptionHandler.java`
- Modify: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/api/IdentityFacade.java`
- Modify: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/application/IdentityFacadeImpl.java`
- Modify: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/application/UserService.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/api/identity/UserAdminController.java`
- Modify: `apps/backend/src/main/java/com/vdt/log_monitoring/api/identity/UserController.java`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/api/identity/UserAdminControllerTest.java`

- [x] **Step 1: Add focused failing admin user controller tests**

Create `UserAdminControllerTest` with `@WebMvcTest(UserAdminController.class)` and `@Import(IdentityExceptionHandler.class)`. Use `@MockBean IdentityFacade`.

Cover:

- `POST /api/v1/users` returns a `UserResponse` from `identityFacade.createUser(...)`.
- `GET /api/v1/users` returns a list from `identityFacade.findUsers()`.
- `GET /api/v1/users/{id}` returns a single user from `identityFacade.findUserById(...)`.
- `PUT /api/v1/users/{id}/role` returns updated user.
- `PUT /api/v1/users/{id}/status` returns updated user.
- `IdentityException.EMAIL_ALREADY_EXISTS` maps to `409 Conflict`.

Use `.with(user("admin@example.com").roles("ADMIN"))` in controller tests unless Spring Security filters are disabled in the test setup.

- [x] **Step 2: Verify expected failure**

Run:

```sh
cd apps/backend && ./mvnw -Dtest=UserAdminControllerTest test
```

Expected: FAIL because `UserAdminController` and `IdentityFacade.findUsers()` do not exist.

- [x] **Step 3: Extend exception codes and handler mapping**

Add error codes:

```java
APPLICATION_NOT_FOUND,
APPLICATION_NAME_ALREADY_EXISTS,
APPLICATION_INACTIVE,
APPLICATION_ACCESS_NOT_FOUND,
INVALID_APPLICATION_STATUS,
INVALID_APPLICATION_ACCESS_LEVEL,
INVALID_APPLICATION_ACCESS_GRANT,
API_KEY_NOT_FOUND,
INVALID_API_KEY,
API_KEY_REVOKED,
API_KEY_EXPIRED
```

Update `IdentityExceptionHandler` mapping:

- `USER_NOT_FOUND`, `APPLICATION_NOT_FOUND`, `APPLICATION_ACCESS_NOT_FOUND`, `API_KEY_NOT_FOUND` -> `404 NOT_FOUND`
- `EMAIL_ALREADY_EXISTS`, `APPLICATION_NAME_ALREADY_EXISTS` -> `409 CONFLICT`
- `INVALID_APPLICATION_STATUS`, `INVALID_APPLICATION_ACCESS_LEVEL`, `INVALID_APPLICATION_ACCESS_GRANT` -> `400 BAD_REQUEST`
- `INVALID_CREDENTIALS`, `INVALID_API_KEY` -> `401 UNAUTHORIZED`
- `ACCOUNT_DISABLED`, `ACCOUNT_LOCKED`, `UNAUTHORIZED`, `APPLICATION_INACTIVE`, `API_KEY_REVOKED`, `API_KEY_EXPIRED` -> `403 FORBIDDEN`

Keep the existing `ApiResponse` error envelope.

- [x] **Step 4: Add user listing to service and facade**

Add to `IdentityFacade`:

```java
List<UserDto> findUsers();
```

Add to `UserService`:

```java
public List<User> listUsers() {
	return userRepository.findAll().stream()
		.map(UserEntity::toModel)
		.toList();
}
```

Implement `IdentityFacadeImpl.findUsers()` by mapping `User` to `UserDto` with the existing `mapToDto`.

- [x] **Step 5: Split admin user controller from self-service controller**

Move admin routes out of `UserController` into `UserAdminController`.

`UserController` keeps:

- `GET /api/v1/users/me`
- `PUT /api/v1/users/me`
- `PUT /api/v1/users/me/password`

`UserAdminController` uses `@RequestMapping("/api/v1/users")` and contains:

- `POST ""`
- `GET ""`
- `GET "/{id}"`
- `PUT "/{id}/role"`
- `PUT "/{id}/status"`

Annotate the controller class or every handler with `@PreAuthorize("hasRole('ADMIN')")`. Keep role/status request bodies as raw strings to preserve compatibility with the existing `UserController` behavior.

- [x] **Step 6: Verify task**

Run:

```sh
cd apps/backend && ./mvnw -Dtest=UserAdminControllerTest test
```

Expected: PASS.

Run:

```sh
cd apps/backend && ./mvnw -Dtest=SecurityConfigTest test
```

Expected: PASS after updating its imports/stubs if `UserController` no longer owns admin routes.

## Task 4: Implement Application Access Facade And Services

**Files:**

- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/api/ApplicationAccessFacade.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/application/ApplicationService.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/application/ApplicationAccessService.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/application/ApplicationAccessFacadeImpl.java`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/identity/application/ApplicationAccessFacadeImplTest.java`

- [x] **Step 1: Add focused failing facade tests for application and grant behavior**

Create `ApplicationAccessFacadeImplTest` by instantiating `ApplicationService`, `ApplicationAccessService`, `ApiKeyService` when needed, and `ApplicationAccessFacadeImpl` with mocked repositories and `PasswordEncoder`. Cover:

- `createApplication` normalizes unique `name` and rejects duplicates with `APPLICATION_NAME_ALREADY_EXISTS`.
- `findVisibleApplications(adminId, "ADMIN")` returns all active applications.
- `findVisibleApplications(engineerId, "ENGINEER")` returns only active applications from the engineer's grants.
- `replaceUserApplicationAccess` rejects duplicate `applicationId` values in one request with `INVALID_APPLICATION_ACCESS_GRANT`.
- `replaceUserApplicationAccess(userId, emptyList, grantedBy)` removes all explicit grants.
- `grantUserApplicationAccess` creates a new row and updates access level when the row already exists.
- `canViewApplication` is true for `ADMIN`, true for `VIEW`, and true for `MANAGE`.
- `canManageApplication` is true for `ADMIN` and true for explicit `MANAGE`.

- [x] **Step 2: Verify expected failure**

Run:

```sh
cd apps/backend && ./mvnw -Dtest=ApplicationAccessFacadeImplTest test
```

Expected: FAIL because `ApplicationAccessFacade`, services, and command/DTO records do not exist.

- [x] **Step 3: Create `ApplicationAccessFacade` contract**

Create `modules/identity/api/ApplicationAccessFacade.java` with command and DTO records nested in the interface to match the existing `IdentityFacade` style.

Include:

```java
ApplicationDto createApplication(CreateApplicationCommand command);
ApplicationDto updateApplication(UUID applicationId, UpdateApplicationCommand command);
ApplicationDto changeApplicationStatus(UUID applicationId, String status);
ApplicationDto findApplicationById(UUID applicationId);
List<ApplicationDto> findAllApplications();
List<ApplicationDto> findVisibleApplications(UUID userId, String role);
List<ApplicationAccessDto> findUserApplicationAccess(UUID userId);
List<ApplicationAccessDto> replaceUserApplicationAccess(UUID userId, List<ApplicationAccessGrantCommand> grants, UUID grantedBy);
ApplicationAccessDto grantUserApplicationAccess(UUID userId, UUID applicationId, String accessLevel, UUID grantedBy);
void removeUserApplicationAccess(UUID userId, UUID applicationId);
boolean canViewApplication(UUID userId, UUID applicationId);
boolean canManageApplication(UUID userId, UUID applicationId);
```

Add nested records:

- `CreateApplicationCommand(String name, String displayName, String description, UUID createdBy)`
- `UpdateApplicationCommand(String name, String displayName, String description)`
- `ApplicationAccessGrantCommand(UUID applicationId, String accessLevel)`
- `ApplicationDto(UUID id, String name, String displayName, String description, String status, Instant createdAt, Instant updatedAt)`
- `ApplicationAccessDto(UUID userId, UUID applicationId, String applicationName, String applicationDisplayName, String accessLevel, UUID grantedBy, Instant createdAt, Instant updatedAt)`

API-key records are added in Task 5.

- [x] **Step 4: Implement application service**

`ApplicationService` responsibilities:

- create application after trimming name/display name and checking `existsByNameIgnoreCase`;
- update name/display name/description and reject duplicate name when changed;
- change status from string using `ApplicationStatus.valueOf(status.toUpperCase())`, mapping invalid values to `INVALID_APPLICATION_STATUS`;
- find application by id or throw `APPLICATION_NOT_FOUND`;
- list all applications;
- list active applications.

- [x] **Step 5: Implement application access service**

`ApplicationAccessService` responsibilities:

- validate target user exists through `UserRepository.findById`;
- validate `grantedBy` exists through `UserRepository.findById`;
- validate applications exist and are `ACTIVE` before granting;
- parse `ApplicationAccessLevel` from request strings;
- reject duplicate application ids in replace request with `400 BAD_REQUEST` mapping;
- replace all grants for a user using `deleteByIdUserId(userId)` then save the requested grants;
- grant/update one access row;
- remove one access row as an idempotent no-op when the row is already absent;
- compute `canViewApplication` and `canManageApplication` by loading the user role first; `ADMIN` always returns true for existing applications, while `ENGINEER` depends on explicit grants.

- [x] **Step 6: Implement facade mapping**

`ApplicationAccessFacadeImpl` composes `ApplicationService` and `ApplicationAccessService`.

Mapping rules:

- map `ApplicationStatus` and `ApplicationAccessLevel` to `name()`;
- resolve `applicationName` and `applicationDisplayName` for access DTOs by loading the application;
- `findVisibleApplications(userId, "ADMIN")` returns active applications from `ApplicationService`;
- `findVisibleApplications(userId, "ENGINEER")` returns active applications from access grants;
- unknown role throws `IdentityException` with `UNAUTHORIZED`.

- [x] **Step 7: Verify task**

Run:

```sh
cd apps/backend && ./mvnw -Dtest=ApplicationAccessFacadeImplTest test
```

Expected: PASS.

## Task 5: Implement API Key Lifecycle And Verification

**Files:**

- Modify: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/api/ApplicationAccessFacade.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/application/ApiKeyService.java`
- Modify: `apps/backend/src/main/java/com/vdt/log_monitoring/modules/identity/application/ApplicationAccessFacadeImpl.java`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/modules/identity/application/ApplicationAccessFacadeImplTest.java`

- [x] **Step 1: Add focused failing API-key tests**

Extend `ApplicationAccessFacadeImplTest`. Cover:

- `createApiKey` returns `rawApiKey` once, stores only `keyHash`, and returns a public `keyPrefix`.
- one application can have multiple active API keys.
- `rotateApiKey` revokes the old key and returns a new raw key/prefix/hash record.
- `revokeApiKey` marks key `REVOKED`.
- `verifyApplicationApiKey(rawKey)` returns valid result with application id/name when the key matches and application is `ACTIVE`.
- revoked key returns invalid result with failure reason `API_KEY_REVOKED`.
- expired key returns invalid result with failure reason `API_KEY_EXPIRED`.
- valid key for inactive application returns invalid result with failure reason `APPLICATION_INACTIVE`.
- invalid prefix/hash returns invalid result with failure reason `INVALID_API_KEY`.

- [x] **Step 2: Verify expected failure**

Run:

```sh
cd apps/backend && ./mvnw -Dtest=ApplicationAccessFacadeImplTest test
```

Expected: FAIL because API-key facade methods and service behavior do not exist.

- [x] **Step 3: Extend `ApplicationAccessFacade` with API-key records and methods**

Add methods:

```java
ApiKeyCreationDto createApiKey(UUID applicationId, String name, Instant expiresAt, UUID createdBy);
List<ApiKeyDto> findApiKeys(UUID applicationId);
ApiKeyCreationDto rotateApiKey(UUID applicationId, UUID apiKeyId, UUID rotatedBy);
void revokeApiKey(UUID applicationId, UUID apiKeyId, UUID revokedBy);
ApiKeyVerificationDto verifyApplicationApiKey(String rawApiKey);
```

Add records:

- `ApiKeyDto(UUID id, UUID applicationId, String name, String keyPrefix, String status, Instant expiresAt, Instant lastUsedAt, Instant createdAt, Instant revokedAt)`
- `ApiKeyCreationDto(UUID id, UUID applicationId, String name, String keyPrefix, String rawApiKey, String status, Instant expiresAt, Instant createdAt)`
- `ApiKeyVerificationDto(boolean valid, UUID applicationId, String applicationName, String applicationDisplayName, String failureReason)`

- [x] **Step 4: Implement API-key generation and hashing**

Use existing `PasswordEncoder` for API-key hashes to avoid adding dependencies.

Generation rules:

- generate prefix like `lms_live_` plus a short random alphanumeric segment;
- generate secret using `SecureRandom` URL-safe bytes;
- raw key format: `{prefix}.{secret}`;
- store only prefix and `passwordEncoder.encode(rawKey)`;
- verify by splitting raw key at the first `.` and loading by prefix, then `passwordEncoder.matches(rawKey, keyHash)`.

Do not log raw keys.

- [x] **Step 5: Implement verification result behavior**

`verifyApplicationApiKey(rawApiKey)` should not throw for expected verification failures. It returns `ApiKeyVerificationDto` with `valid=false` and one of:

- `INVALID_API_KEY`
- `API_KEY_REVOKED`
- `API_KEY_EXPIRED`
- `APPLICATION_INACTIVE`

For a valid key, return `valid=true`, application id/name/display name, and `failureReason=null`.

Do not update `lastUsedAt` on every verification. Leave it unchanged in this implementation.

- [x] **Step 6: Verify task**

Run:

```sh
cd apps/backend && ./mvnw -Dtest=ApplicationAccessFacadeImplTest test
```

Expected: PASS.

## Task 6: Add Application, Access, And API-Key REST Controllers

**Files:**

- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/api/identity/ApplicationController.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/api/identity/ApplicationAccessController.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/api/identity/ApplicationApiKeyController.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/api/identity/dto/ApplicationRequest.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/api/identity/dto/ApplicationStatusRequest.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/api/identity/dto/ApplicationResponse.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/api/identity/dto/ApplicationAccessGrantRequest.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/api/identity/dto/ReplaceApplicationAccessRequest.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/api/identity/dto/ApplicationAccessResponse.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/api/identity/dto/CreateApiKeyRequest.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/api/identity/dto/ApiKeyResponse.java`
- Create: `apps/backend/src/main/java/com/vdt/log_monitoring/api/identity/dto/ApiKeyCreationResponse.java`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/api/identity/ApplicationControllerTest.java`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/api/identity/ApplicationAccessControllerTest.java`
- Test: `apps/backend/src/test/java/com/vdt/log_monitoring/api/identity/ApplicationApiKeyControllerTest.java`

- [x] **Step 1: Add focused failing controller tests**

Create `@WebMvcTest` tests for each controller with `@MockBean ApplicationAccessFacade` and, where needed for `/applications/me`, `@MockBean IdentityFacade`.

Application controller tests:

- admin `POST /api/v1/applications` maps request to `CreateApplicationCommand`.
- admin `GET /api/v1/applications` returns list from `findAllApplications`.
- admin `GET /api/v1/applications/{id}` returns application by id.
- admin `PUT /api/v1/applications/{id}` maps update request.
- admin `PUT /api/v1/applications/{id}/status` maps status request.
- authenticated `GET /api/v1/applications/me` resolves principal email through `IdentityFacade.findUserByEmail` and calls `findVisibleApplications(user.id(), user.role())`.

Access controller tests:

- admin `GET /api/v1/users/{userId}/applications` returns access grants.
- admin `PUT /api/v1/users/{userId}/applications` maps replace request.
- admin `POST /api/v1/users/{userId}/applications/{applicationId}` maps single grant request.
- admin `DELETE /api/v1/users/{userId}/applications/{applicationId}` returns success.

API-key controller tests:

- admin `POST /api/v1/applications/{applicationId}/api-keys` returns raw key exactly once in create response.
- admin `GET /api/v1/applications/{applicationId}/api-keys` omits raw key.
- admin `POST /api/v1/applications/{applicationId}/api-keys/{apiKeyId}/rotate` returns raw key.
- admin `POST /api/v1/applications/{applicationId}/api-keys/{apiKeyId}/revoke` returns success.

- [x] **Step 2: Verify expected failure**

Run:

```sh
cd apps/backend && ./mvnw -Dtest=ApplicationControllerTest,ApplicationAccessControllerTest,ApplicationApiKeyControllerTest test
```

Expected: FAIL because controllers and DTOs do not exist.

- [x] **Step 3: Implement DTOs**

Follow existing DTO style with Lombok `@Data`, validation annotations, and static `from(...)` mappers where useful.

Validation:

- `ApplicationRequest.name`: `@NotBlank`, `@Size(max = 100)`
- `ApplicationRequest.displayName`: `@NotBlank`, `@Size(max = 150)`
- `ApplicationRequest.description`: `@Size(max = 2000)`
- `ApplicationStatusRequest.status`: `@NotBlank`
- `ApplicationAccessGrantRequest.accessLevel`: `@NotBlank`
- `ReplaceApplicationAccessRequest.grants`: `@NotNull`
- `CreateApiKeyRequest.name`: `@NotBlank`, `@Size(max = 100)`
- `CreateApiKeyRequest.expiresAt`: optional `Instant`

Response mapping:

- `ApplicationResponse.from(ApplicationDto dto)`
- `ApplicationAccessResponse.from(ApplicationAccessDto dto)`
- `ApiKeyResponse.from(ApiKeyDto dto)` must not include raw key.
- `ApiKeyCreationResponse.from(ApiKeyCreationDto dto)` includes raw key.

- [x] **Step 4: Implement controllers**

`ApplicationController`:

- `@RequestMapping("/api/v1/applications")`
- Admin methods use `@PreAuthorize("hasRole('ADMIN')")`.
- `GET /me` is authenticated through security config and does not use admin preauthorize.
- Wrap responses in `ApiResponse.success(...)`.

`ApplicationAccessController`:

- `@RequestMapping("/api/v1/users/{userId}/applications")`
- class-level `@PreAuthorize("hasRole('ADMIN')")`.
- Pass current admin id as `grantedBy` where needed. Resolve it from `Principal` by calling `IdentityFacade.findUserByEmail(principal.getName())`.

`ApplicationApiKeyController`:

- `@RequestMapping("/api/v1/applications/{applicationId}/api-keys")`
- class-level `@PreAuthorize("hasRole('ADMIN')")`.
- Resolve `createdBy`/`rotatedBy`/`revokedBy` from `Principal` through `IdentityFacade`.

- [x] **Step 5: Verify task**

Run:

```sh
cd apps/backend && ./mvnw -Dtest=ApplicationControllerTest,ApplicationAccessControllerTest,ApplicationApiKeyControllerTest test
```

Expected: PASS.

## Task 7: Update Security Matchers And Security Tests

**Files:**

- Modify: `apps/backend/src/main/java/com/vdt/log_monitoring/shared/security/SecurityConfig.java`
- Modify: `apps/backend/src/test/java/com/vdt/log_monitoring/shared/security/SecurityConfigTest.java`

- [x] **Step 1: Add focused failing security tests**

Update `SecurityConfigTest` to import the required controllers:

- `UserController`
- `UserAdminController`
- `ApplicationController`
- `ApplicationAccessController`
- `ApplicationApiKeyController`

Provide test beans for both `IdentityFacade` and `ApplicationAccessFacade`.

Add tests:

- engineer can call `GET /api/v1/applications/me`.
- engineer cannot call `GET /api/v1/applications`.
- engineer cannot call `POST /api/v1/applications`.
- engineer cannot call `PUT /api/v1/users/{userId}/applications`.
- engineer cannot call `POST /api/v1/applications/{applicationId}/api-keys`.
- admin can call `GET /api/v1/applications`.
- existing engineer self-service `/api/v1/users/me/password` remains authenticated and not admin-only.

- [x] **Step 2: Verify expected failure**

Run:

```sh
cd apps/backend && ./mvnw -Dtest=SecurityConfigTest test
```

Expected: FAIL before matcher update because `/api/v1/applications/me` is covered by the admin-only `/api/v1/applications/**` matcher.

- [x] **Step 3: Update matcher order**

Change authorization rules to:

```java
.requestMatchers("/api/v1/users/me", "/api/v1/users/me/**").authenticated()
.requestMatchers("/api/v1/applications/me").authenticated()
.requestMatchers("/api/v1/users/**").hasRole("ADMIN")
.requestMatchers("/api/v1/applications/**").hasRole("ADMIN")
.anyRequest().authenticated()
```

Keep auth, OpenAPI, and Swagger permit-all matchers unchanged.

- [x] **Step 4: Verify task**

Run:

```sh
cd apps/backend && ./mvnw -Dtest=SecurityConfigTest test
```

Expected: PASS.

## Task 8: Run Focused And Full Backend Verification

**Files:**

- No production files.

- [x] **Step 1: Run focused identity tests**

Run:

```sh
cd apps/backend && ./mvnw -Dtest=UserTest,UserEntityTest,MonitoredApplicationTest,UserApplicationAccessTest,ApplicationApiKeyTest,ApplicationEntityTest,UserApplicationAccessEntityTest,ApplicationApiKeyEntityTest,ApplicationAccessFacadeImplTest test
```

Expected: PASS.

- [x] **Step 2: Run focused API/security tests**

Run:

```sh
cd apps/backend && ./mvnw -Dtest=UserAdminControllerTest,ApplicationControllerTest,ApplicationAccessControllerTest,ApplicationApiKeyControllerTest,SecurityConfigTest test
```

Expected: PASS.

- [x] **Step 3: Start PostgreSQL for Spring Boot integration tests**

Run:

```sh
docker compose up -d postgres
```

Expected: PostgreSQL is healthy on port `5432` using the project `.env` values.

- [x] **Step 4: Run full backend tests with PostgreSQL available**

Run:

```sh
cd apps/backend && ./mvnw test
```

Expected: PASS. `LogMonitoringApplicationTests` can start the Spring context, and Flyway can apply `V1` and `V2` to PostgreSQL.

- [x] **Step 5: Run local app startup smoke test**

Run:

```sh
cd apps/backend && ./mvnw spring-boot:run
```

Expected: Spring Boot starts with no identity bean wiring errors. Stop the app after confirming startup.

## Final Verification

Run these commands after all implementation tasks:

```sh
docker compose up -d postgres
cd apps/backend && ./mvnw test
```

Expected: PASS with PostgreSQL available for Spring context and Flyway tests.

Then run:

```sh
cd apps/backend && ./mvnw spring-boot:run
```

Expected: app starts, Flyway migration succeeds, and no identity bean wiring errors occur.

Manual API smoke checks after the app is running with an admin account:

- `POST /api/v1/users` creates a user when called by `ADMIN`.
- `POST /api/v1/applications` creates an active application.
- `POST /api/v1/applications/{applicationId}/api-keys` returns `rawApiKey`.
- `GET /api/v1/applications/{applicationId}/api-keys` does not return `rawApiKey`.
- `PUT /api/v1/users/{userId}/applications` assigns application access.
- `GET /api/v1/applications/me` returns all active applications for admin and only assigned active applications for engineer.

## Spec Coverage

| Spec requirement / acceptance criterion | Covered by tasks | Verification |
| --- | --- | --- |
| No new `applications` module | Tasks 2-7 keep files under `modules/identity` and `api/identity` | File map and package paths |
| `identity` owns applications, API keys, and user-application access | Tasks 1-5 | Migration, domain, entity, repository, service tests |
| Split `IdentityFacade` and `ApplicationAccessFacade` | Tasks 3-5 | Facade signatures and `ApplicationAccessFacadeImplTest` |
| Admin can create users through admin-protected route | Task 3 | `UserAdminControllerTest`, `SecurityConfigTest` |
| Admin can create/update/list/activate/deactivate applications | Tasks 4 and 6 | `ApplicationAccessFacadeImplTest`, `ApplicationControllerTest` |
| Admin can create/rotate/revoke/list application API keys | Tasks 5 and 6 | `ApplicationAccessFacadeImplTest`, `ApplicationApiKeyControllerTest` |
| Admin can grant/replace/list/remove user application access | Tasks 4 and 6 | `ApplicationAccessFacadeImplTest`, `ApplicationAccessControllerTest` |
| `GET /api/v1/applications/me` returns role-scoped active apps | Tasks 4, 6, 7 | `ApplicationAccessFacadeImplTest`, `ApplicationControllerTest`, `SecurityConfigTest` |
| API-key verification is internal facade behavior only | Task 5 | `ApplicationAccessFacadeImplTest`; no verify REST controller is created |
| Other modules can verify API keys/access without identity persistence imports | Task 5 | Public `ApplicationAccessFacade` contract and integration-boundary test guidance |
| API keys/passwords are not stored or returned plain text outside create/rotate | Tasks 2, 5, 6 | API-key service tests and API-key controller tests |
| Engineer cannot call admin application/access/API-key routes | Task 7 | `SecurityConfigTest` |
| Engineer with no assigned applications gets empty list | Tasks 4 and 6 | `ApplicationAccessFacadeImplTest`, `ApplicationControllerTest` |
| Duplicate app names fail | Task 4 | `ApplicationAccessFacadeImplTest` |
| Revoked/expired/inactive API-key verification fails safely | Task 5 | `ApplicationAccessFacadeImplTest` |
