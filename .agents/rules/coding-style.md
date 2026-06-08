# Coding Style Rules

Apply to all source and test changes.

## General

- Follow the style of surrounding code.
- Keep classes, components, and functions focused.
- Prefer existing dependencies and standard APIs.
- Do not perform unrelated cleanup.
- Add comments only for non-obvious decisions.
- Remove temporary output, dead code, and unused imports.

## Java

- Target Java 21 and package code under `com.vdt.log_monitoring`.
- Put business code under `modules`, module-local transport adapters under
  `modules.<module>.api`, and technical building blocks under `shared`.
- Implement `identity`, `logs`, `alerting`, and `realtime` first; do not create
  empty future modules.
- Add `domain` and `integrationevents` only when real behavior requires them.
- Never import another module's entity, repository, persistence model, or
  infrastructure class.
- Keep controllers thin inside the owning module's `api` package.
- Use `PascalCase` for types, `camelCase` for methods and variables, and
  `UPPER_SNAKE_CASE` for constants.
- Use constructor injection for required dependencies.
- Use request and response DTOs at HTTP boundaries.
- Validate external input with Bean Validation.
- Do not return persistence or domain entities from controllers.
- Keep DDD domain types framework-free.
- Keep integration events immutable and named in the past tense.
- Avoid empty interfaces, pass-through services, speculative ports, and
  mechanical layer-to-layer mappings.
- Follow Maven's existing compiler and Spring Boot configuration.

## TypeScript And React

- Use TypeScript function components and `PascalCase` component files.
- Use `camelCase` functions and variables.
- Use `@/` for imports rooted at `src`.
- Keep app wiring in `src/app`, feature code in `src/features`, and reusable
  code in `src/shared`.
- Use TanStack Query for server state and the shared Axios API client.
- Preserve lazy route modules by exporting `Component`.
- Respect strict unused-variable and unused-parameter checks.
- Do not edit `src/api/generated/api-types.ts` manually.

## Dependencies

New dependencies require a task-specific technical reason and user approval
when they change the approved dependency strategy.

## Verification

- Java: `cd apps/backend && ./mvnw test`
- Frontend lint: `cd apps/frontend && npm run lint`
- Frontend type-check/build: `cd apps/frontend && npm run build`
