# Aegis — Milestone Progress

A guided 50-milestone journey: Spring Boot 3 REST API → production AI platform.
Type **"Next"** to Claude to advance to the next milestone.

## Environment notes (so any session/machine is consistent)
- JDK **17** (project targets Java 17). Build & run ONLY with the wrapper: `.\mvnw`
- Dev profile → port **8081**; prod profile → port **8080**
- Stop a running app (IntelliJ red ■) before re-running, or you get "port in use".

## Status
- [x] **M1 — Project scaffold**: Maven + Spring Boot 3.3.5 + Java 17, hexagonal package layout, `/firstRestApi/ping`, actuator health, context-load test.
- [x] **M2 — Config profiles & structured logging**: `dev`/`prod` profiles, type-safe `AppProperties`, `CorrelationIdFilter` (X-Correlation-Id trace id in MDC + response header), `logback-spring.xml` (console in dev, JSON in prod).
- [x] **M3 — First feature slice**: `Document` entity, `DocumentService`, in-memory `DocumentRepository` (temp until M5), REST `POST`/`GET /api/v1/documents`, DTOs, minimal exception handler.
- [x] **M4 — CRUD, validation & web tests (Phase 0 finish)**:
  - **Restructured to layered, package-by-feature** — `document/` now holds controller + service + repository + entity + `dto/`; shared bits in `common/` and `config/`. The hexagonal `domain/`/`application/`/`infrastructure/` rings and the port interfaces are gone (simpler, and what most Spring codebases look like).
  - Completed the CRUD: `GET /api/v1/documents` (list, newest first), `PUT /{id}` (full replace, bumps `updatedAt`), `DELETE /{id}` → 204.
  - **Bean Validation** (`spring-boot-starter-validation`): `@NotBlank`/`@Size` on the request DTOs + `@Valid` in the controller, so bad input is rejected at the edge.
  - Error handling: one `ApiError` shape everywhere, now carrying the **correlation id** and per-field `fieldErrors`; bad UUID → 400 (not 500); catch-all handler never leaks a stack trace.
  - **MockMvc web-layer tests** — the layer M3 never covered. 30 tests total (13 web, 8 service, 4 repository, 4 entity, 1 context).
- [x] **M5 — PostgreSQL + JPA**:
  - `Document`/`DocumentRepository` were already JPA-mapped since M4's restructure; M5's real work was swapping the `dev` profile off H2 onto a real PostgreSQL 16 instance (run via Docker: `docker run --name aegis-postgres -e POSTGRES_PASSWORD=aegis -e POSTGRES_DB=aegis -p 5432:5432 -d postgres:16`).
  - Added the `org.postgresql:postgresql` runtime driver to `pom.xml`; H2 stays as a test-only dependency (`@DataJpaTest` auto-configures its own embedded H2, unaffected by the `dev`/`prod` datasource properties).
  - `application-dev.properties` now points at `jdbc:postgresql://localhost:5432/aegis`; `documents` table is auto-created there by `ddl-auto=update` from the `@Entity` mapping.
  - Fixed a Windows/JDK quirk: the JVM's default timezone reports as the legacy alias `Asia/Calcutta`, which current Postgres's tzdata doesn't recognize, causing every JDBC connection (app **and** tests) to fail at connect time. Fixed via a `static` initializer in `AegisApplication` pinning the JVM to UTC — a static block (not a `main()` one-liner) is required so tests, which boot the context via `SpringApplication.run()` directly and skip `main()`, are also covered.
  - `prod` profile is untouched — still file-based H2 (already marked TEMPORARY in its own comments); a real Postgres for prod is deferred to a later M6–M10 milestone.
  - 31/31 tests green.
- [x] **M6 — Pagination, sorting, filtering** *(reconstructed from code in 2026-10-04 audit)*: `GET /api/v1/documents?page&size&sort&q`, `Pageable` defaults (size 20, newest first, id tiebreak), `PageResponse` wire DTO, `findByTitleContainingIgnoreCase`.
- [x] **Flyway migrations** *(reconstructed)*: `db/migration/V1__create_documents_table.sql`; both profiles `ddl-auto=validate`.
- [x] **M11–M12 — Spring Security base + JWT issuing** *(reconstructed)*: default-deny `SecurityConfig` (stateless, CSRF off), in-memory `admin` user, `POST /api/v1/auth/login` → HS256 JWT via `JwtService`. `/api/v1/documents/**` is still `permitAll` (TEMPORARY).
- [x] **M13 — JWT validation filter**: `JwtAuthenticationFilter` (not a @Component, added in `SecurityConfig` only) reads `Authorization: Bearer`, verifies via `JwtService`, loads the user, populates the SecurityContext. `/api/v1/documents/**` now requires a token; HTTP Basic removed; custom `AuthenticationEntryPoint` returns 401 in the `ApiError` shape. `CorrelationIdFilter` moved to `HIGHEST_PRECEDENCE` so 401s carry a correlation id. Added `spring-security-test`. 48 tests green.
- [ ] **M14 … M50** (M14 next) (see full roadmap below). Exact M7–M10 labels weren't logged; confirm what they were if it matters.

Audit 2026-10-04 (before M13): `.\mvnw clean test` → 40 tests, 0 failures (1 context, 3+2 auth, 15 web, 5 repo, 10 service, 4 entity).
Housekeeping: `TestThreads.java` is a scratch file and can be deleted; `New Text Document.txt` is stray notes.

## Roadmap
- Phase 0 Foundations: M1–M4 ✅
- Phase 1 Persistence (PostgreSQL/JPA, pagination, migrations): M5–M10
- Phase 2 Security + JWT: M11–M14
- Phase 3 API quality (exceptions, OpenAPI): M15–M18
- Phase 4 Infra (Docker, Redis, Kafka, async): M19–M23
- Phase 5 AI foundations (LangChain4j, LLM abstraction, prompts, chat): M24–M28
- Phase 6 RAG (pgvector, embeddings, vector search): M29–M34
- Phase 7 Conversation (memory, streaming, citations): M35–M37
- Phase 8 Agents & tools (function calling, MCP): M38–M42
- Phase 9 Hardening (rate limiting, resilience, observability): M43–M46
- Phase 10 Testing & CI/CD (JUnit5, Mockito, Testcontainers, GitHub Actions): M47–M48
- Phase 11 Kubernetes & deployment: M49–M50

## How to resume tomorrow
1. Open a terminal in this folder (or IntelliJ terminal).
2. Run: `claude --continue`   (resumes the most recent conversation)
   or:  `claude --resume`     (pick from a list of past sessions)
3. Say: "Continue Aegis from M4" (or just "Next" to start M5).

## Where things stand (end of 2026-09-11)
- Phase 0 complete: M1–M4 done. M5 done: dev now runs against real PostgreSQL 16 in Docker (`aegis-postgres` container, `aegis` database), `.\mvnw clean test` green (31 tests).
- Code is **layered, package-by-feature**: `config/`, `common/`, `document/`.
  Not hexagonal — the port interfaces were removed in M4 on purpose.
- Storage is a real Postgres table (`documents`) now; data persists across app restarts (only lost if the `aegis-postgres` container itself is removed).
- Next up: M6.
