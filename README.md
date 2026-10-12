# Aegis — Enterprise AI Knowledge & Support Platform

A learning-to-production project: grows from a plain Spring Boot 3 / Java 17 REST API
into a full production-grade AI application (RAG, agents, tool calling, MCP) built on
Clean / Hexagonal Architecture.

## Tech (added milestone by milestone)
Java 21 · Spring Boot 3 · Maven · PostgreSQL · JPA/Hibernate · Spring Security + JWT ·
Redis · Kafka · Docker · Kubernetes · LangChain4j · pgvector · RAG · MCP · AI Agents.

## Architecture
Classic layered Spring, packaged **by feature** (not by layer). Each feature package
holds its own controller / service / repository / entity / dto, so everything about
documents lives in one place:

```
com/sapiens/aegis/
  AegisApplication.java
  config/     AppProperties                      — type-safe aegis.* config
  common/     CorrelationIdFilter                — per-request trace id
              ApiExceptionHandler, ApiError      — one error shape for the whole API
              HealthController                   — /firstRestApi/ping
  document/   Document                           — entity (gains @Entity at M5)
              DocumentController                 — web layer (HTTP + DTO mapping)
              DocumentService                    — business logic
              DocumentRepository                 — persistence (Map now, JPA at M5)
              DocumentNotFoundException
              dto/                               — request/response records
```

The rule that matters: **Controller → Service → Repository**, one direction only.
The controller never touches storage, the repository never knows about HTTP, and
DTOs stop the wire format from leaking into the entity.

## Run locally
Prerequisites: JDK 17+ (project targets Java 17).

> This repo ships the **Maven Wrapper** (`mvnw`). Always use `.\mvnw` instead of a
> system `mvn` — it downloads and uses a modern, pinned Maven (3.9.9) into a local
> cache, so an old system Maven does not matter and builds are reproducible.

```powershell
.\mvnw spring-boot:run
```

Then (dev profile → port 8081):
- http://localhost:8081/firstRestApi/ping
- http://localhost:8081/actuator/health

## Build & test
```powershell
.\mvnw clean package                          # builds target/aegis-0.0.1-SNAPSHOT.jar
.\mvnw test                                    # run tests
java -jar target\aegis-0.0.1-SNAPSHOT.jar      # run the built jar
```

## API (M4)
| Method | Path | Success | Notes |
|---|---|---|---|
| POST | `/api/v1/documents` | 201 + `Location` | validates title/content |
| GET | `/api/v1/documents` | 200 | all documents, newest first |
| GET | `/api/v1/documents/{id}` | 200 | 404 if unknown, 400 if id isn't a UUID |
| PUT | `/api/v1/documents/{id}` | 200 | full replace, bumps `updatedAt` |
| DELETE | `/api/v1/documents/{id}` | 204 | 404 if unknown |

Every error returns the same JSON shape (`timestamp`, `status`, `error`, `message`,
`correlationId`, plus `fieldErrors` on validation failures).

## Milestone log
- **M1** — Project scaffold: Maven + Spring Boot 3 + Java 17, package layout, health endpoint.
- **M2** — Config profiles (`dev`/`prod`), type-safe `AppProperties`, correlation-id filter, structured logging.
- **M3** — First feature slice: `Document` entity, service, repository, `POST`/`GET /api/v1/documents`.
- **M4** — Restructured to package-by-feature layered code; full CRUD (`GET` list, `PUT`, `DELETE`),
  Bean Validation on request DTOs, correlation id + field errors in error bodies, MockMvc web-layer tests (30 tests).
