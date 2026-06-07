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
- Make the business capability the first package boundary, for example
  `ingestion`, `logsearch`, or `alerting`.
- Keep simple capabilities compact; use feature-local classes or small
  subpackages without forcing DDD layers.
- For complex domain capabilities, use `api`, `application`, `domain`, and
  `infrastructure` where those boundaries add value.
- Do not create top-level technical buckets that group unrelated capabilities
  by class role.
- Use `PascalCase` for types, `camelCase` for methods and variables, and
  `UPPER_SNAKE_CASE` for constants.
- Use constructor injection for required dependencies.
- Use request and response DTOs at HTTP boundaries.
- Validate external input with Bean Validation.
- Keep HTTP classes inside their owning capability and do not return
  persistence entities from them.
- In DDD-oriented capabilities, keep domain types framework-free.
- Avoid empty interfaces, pass-through services, and mechanical layer-to-layer
  mappings.
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
