# Log Monitoring System Agents

## Project

Backend target: one Spring Boot Modular Monolith. Initial modules are
`identity`, `logs`, `alerting`, and `realtime`. Add `incidents`, `analytics`,
`retention`, and `ai` only when their use cases begin.

Required infrastructure:

- PostgreSQL: transactional/configuration data.
- ClickHouse: high-volume logs and analytical datasets.
- Kafka: mandatory buffer for every accepted raw log.
- Redis: mandatory atomic alert deduplication.

Current implementation is an early scaffold. Verify source before claiming a
target behavior exists.

Stack:

- Backend: Java 21, Spring Boot 3.5, Maven
- Frontend: React 19, TypeScript 5, Vite 8
- Required infrastructure: PostgreSQL 17, ClickHouse 25.3, Kafka 4, Redis 8
- Runtime: one Spring Boot application and Docker-based local infrastructure

## Load Context

Read only context relevant to the current task:

- `.agents/context/project-overview.md`: purpose, users, scope, and current
  implementation status
- `.agents/context/architecture-overview.md`: modules, ownership, package
  boundaries, communication, and guardrails
- `.agents/context/glossary.md`: domain terminology and naming

Repository sources:

- `DETAI.md`: authoritative assignment scope and required/bonus behavior
- `README.md`: setup, commands, current gaps, and roadmap
- `Makefile`: supported development commands
- `compose.yml`: executable local infrastructure
- `apps/backend/pom.xml`: backend dependencies
- `apps/frontend/package.json`: frontend dependencies and scripts
- Source code, migrations, and executable configuration: implementation truth

`docs/module-requirement.md` and `docs/store-requirement.md` are temporary
planning drafts. Do not load, enforce, or synchronize them unless the user
explicitly names one of those files.

## Load Rules

Read the matching rule before editing:

- Any source or test code: `.agents/rules/coding-style.md`
- Module boundaries or integrations: `.agents/rules/architecture.md`
- HTTP, events, WebSocket, or contracts: `.agents/rules/api-design.md`
- Schema, persistence, queries, or retention: `.agents/rules/database.md`
- Every change: `.agents/rules/security.md`
- Behavior changes or fixes: `.agents/rules/testing.md`
- Documentation changes: `.agents/rules/documentation.md`

Do not load unrelated rules or planning drafts.

## Architecture Constraints

- HTTP/WebSocket controllers belong under top-level `api/<module>`.
- `modules/<module>/api` contains public module facades/contracts only.
- A controller calls one module facade and contains no business/storage logic.
- Modules never import another module's entity, repository, domain, or
  infrastructure and never access another module's storage directly.
- `shared` contains technical primitives only.
- `logs.ingestion`, `logs.processing`, and `logs.query` are components of the
  single `logs` data owner, not separate modules.
- Preserve the existing frontend architecture.

## Workflow

Before changing files:

1. Inspect relevant implementation, configuration, and `git status`.
2. Read the matching context and mandatory rule files.
3. Distinguish implemented behavior from target architecture and roadmap.
4. Preserve unrelated user changes.
5. Use a workflow skill when its trigger conditions are met.

After changing files:

1. Run focused verification for the changed area.
2. Run relevant regression checks.
3. Regenerate OpenAPI/frontend types when controller or DTO contracts change.
4. Update affected official documentation.
5. Report unrun checks, failures, deviations, and residual risks.

## Skills

- `.agents/skills/brainstorming/SKILL.md`: explicit design exploration
- `.agents/skills/writing-plans/SKILL.md`: implementation plan from an approved
  design
- `.agents/skills/executing-plans/SKILL.md`: execute an approved plan
- `.agents/skills/fix/SKILL.md`: localized defects and failing checks
- `.agents/skills/loop/SKILL.md`: repeated assess/fix/verify cycles
- `.agents/skills/docs/SKILL.md`: official architecture/API/database docs
- `.agents/skills/git/SKILL.md`: review, stage, commit, and optional push

Read a skill's `SKILL.md` before using it. Do not automatically chain workflow
stages.

## Commands

- Infrastructure: `make infra-up`, `make infra-down`
- Development: `make backend`, `make frontend`
- Verification: `make test`, `make lint`, `make build`
- API generation: `make api` while backend is running
- Infrastructure status: `docker compose ps`

Local endpoints:

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui`
- OpenAPI: `http://localhost:8080/v3/api-docs`

## Constraints

Never hardcode or expose secrets. Never edit generated API artifacts manually.
Ask before changing approved architecture, API, schema, security, dependency
strategy, or acceptance criteria.

- Keep changes scoped and preserve unrelated user work.
- Treat `.agents/context/architecture-overview.md` and matching rule files as
  active agent guidance.
- Treat source code and executable configuration as implementation truth.
- Never claim verification passed unless it was actually run.
- Preserve the frontend architecture unless the user explicitly changes it.
