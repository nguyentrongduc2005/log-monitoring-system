# Log Monitoring System AGENTS

## Project

Log Monitoring System is a monorepo for real-time log ingestion, monitoring,
alerting, and analytics.

Current state: the Spring Boot application, React shell, local infrastructure,
Docker images, and OpenAPI generation workflow are scaffolded. Persistence,
ingestion, processing, live streaming, authentication, alerting, and analytics
are roadmap work unless current source code proves otherwise.

Stack:

- Backend: Java 21, Spring Boot 3.5, Maven
- Frontend: React 19, TypeScript 5, Vite 8
- Infrastructure: PostgreSQL 17, ClickHouse 25.3, Kafka 4, Redis 8, Docker

## Load Context

Read only files relevant to the current task:

- `.agents/context/project-overview.md`: purpose, users, scope, and current
  implementation status
- `.agents/context/architecture-overview.md`: components, ownership, and
  target data flow
- `.agents/context/glossary.md`: domain terminology and naming

Repository sources remain authoritative:

- `README.md`: setup, target architecture, and roadmap
- `Makefile`: common commands
- `compose.yml`: local infrastructure
- `apps/backend/pom.xml`: backend dependencies
- `apps/frontend/package.json`: frontend dependencies and scripts

## Load Rules

Read the matching mandatory rule file before editing:

- Any source or test code: `.agents/rules/coding-style.md`
- Module boundaries or integrations: `.agents/rules/architecture.md`
- HTTP, events, WebSocket, or contracts: `.agents/rules/api-design.md`
- Schema, persistence, queries, or retention: `.agents/rules/database.md`
- Any change: `.agents/rules/security.md`
- Behavior changes: `.agents/rules/testing.md`
- Behavior, architecture, API, data, configuration, or workflow docs:
  `.agents/rules/documentation.md`

Do not load unrelated context or rule files.

## Workflow

Before changing files:

1. Inspect the relevant implementation and working tree.
2. Read only the applicable context and rules.
3. Distinguish implemented behavior from target architecture and roadmap.
4. Use a workflow skill when its trigger conditions are met.
5. Do not implement work that requires an approved specification or plan
   until those artifacts exist and implementation is requested.

After changing files:

1. Run focused verification for the changed area.
2. Run relevant regression checks.
3. Regenerate API types when controller or DTO contracts change.
4. Update affected official documentation.
5. Report unrun checks, failures, deviations, and risks.

## Skills

- `.agents/skills/brainstorming/SKILL.md`: requirements and design
- `.agents/skills/writing-plans/SKILL.md`: implementation planning from an
  approved specification
- `.agents/skills/executing-plans/SKILL.md`: approved plan execution
- `.agents/skills/fix/SKILL.md`: localized defects and failing checks
- `.agents/skills/loop/SKILL.md`: repeated review, fix, and verification cycles
- `.agents/skills/docs/SKILL.md`: architecture, API, and database documentation
- `.agents/skills/git/SKILL.md`: final review and logical commit

Read a skill's `SKILL.md` before using it. Do not automatically chain workflow
stages unless the skill and user request permit it.

## Commands

- Infrastructure: `make infra-up`, `make infra-down`
- Development: `make backend`, `make frontend`
- Backend tests: `make test`
- Frontend lint: `make lint`
- Full build: `make build`
- API type generation: `make api` while the backend is running
- Infrastructure status: `docker compose ps`

Local endpoints:

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui`
- OpenAPI: `http://localhost:8080/v3/api-docs`

## Constraints

- Keep changes scoped and preserve unrelated user work.
- Follow existing code structure and conventions.
- Treat source code and executable configuration as the source of truth.
- Never hardcode, expose, log, or commit secrets.
- Never edit generated OpenAPI or TypeScript API artifacts manually.
- Do not claim verification passed unless it was run.
- Ask before changing an approved API, schema, security rule, architecture,
  dependency strategy, or acceptance criterion.
