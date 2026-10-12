# Local Chatbot

A simple general-purpose conversational chatbot, backed by Azure OpenAI,
running entirely on your own machine. Built with Spring Boot + Spring AI.

## Prerequisites

- Java 17+
- Maven 3.9+ (or use the bundled `./mvnw` / `mvnw.cmd` wrapper — no separate
  Maven install needed)
- An Azure OpenAI resource with a deployed chat model

## Setup

1. **Set your Azure OpenAI details as environment variables** (never put
   them in a file that gets committed):

   ```bash
   export OPENAI_API_ENDPOINT=https://your-resource.openai.azure.com/
   export OPENAI_API_KEY=your-azure-openai-key
   export OPENAI_CHAT_DEPLOYMENT_NAME=your-deployment-name
   ```

   To make this persistent across terminal sessions, add that line to your
   shell profile (`~/.zshrc`, `~/.bashrc`, etc.) instead of a project file.

2. **Run the app:**

   ```bash
   ./mvnw spring-boot:run
   ```

3. **Open the UI:**

   Go to [http://localhost:8080](http://localhost:8080) in your browser.

The server binds to `127.0.0.1` only (see `application.yml`) — it is not
reachable from other devices on your network.

## How it works

- `ChatController` exposes `POST /api/chat`, which forwards your message to
  Azure OpenAI via Spring AI's `ChatClient` and returns the reply.
- The static `index.html` page is a minimal UI that calls that endpoint.
- No conversation history is persisted anywhere — closing the tab clears it.

## Sapiens Security Controls Summary

This build is local-only, single-user, with no login and no sensitive data
(per the elicitation answers given before the build), so the applicable
control set is narrower than a networked multi-user app. Controls applied:

**General secure development**
- *Avoid hard-coding sensitive information*: the Azure OpenAI endpoint and
  key are read only from the `OPENAI_API_ENDPOINT` / `OPENAI_API_KEY`
  environment variables (`application.yml`, `ChatbotApplication`), never
  written to a file, never committed.
- *Secrets management*: the key stays server-side; it is never sent to the
  browser or exposed in any client-visible response.
- *Limit third-party libraries*: the only external dependency added is the
  official, actively maintained `spring-ai-azure-openai-spring-boot-starter`.
- *Fail fast on missing config*: the app refuses to start with a clear
  message if `OPENAI_API_ENDPOINT`, `OPENAI_API_KEY`, or
  `OPENAI_CHAT_DEPLOYMENT_NAME` isn't set, rather than failing confusingly
  later.

**Input data validation**
- `ChatRequest` enforces non-blank input and a 4000-character cap via
  Bean Validation (`@NotBlank`, `@Size`), rejected before reaching the LLM
  call.
- The frontend also enforces the same length cap client-side as a first
  line of defense (server-side check is the authoritative one).
- All rendered content in the browser uses `textContent`, never
  `innerHTML`, so nothing the model or user types can execute as HTML/script
  (XSS defense) — see `index.html`.

**Error handling**
- `GlobalExceptionHandler` and the controller's try/catch return generic,
  user-safe error messages. Stack traces, provider error bodies, and
  internal details are never sent to the client — only logged server-side.

**Logging**
- Chat requests are logged with message *length* only, never message
  *content*, since chat text could contain anything the user types.
- Errors are logged server-side with full detail for debugging, but that
  detail never crosses the response boundary to the client.

**Deliberately not applied (and why)**
- *Authentication / session management*: skipped — this is a single-user,
  local-only tool with no login, per the stated requirements. If this ever
  moves beyond your own laptop (shared server, multiple users, deployed
  online), authentication and session controls must be added before that
  happens.
- *DDoS protection / rate limiting*: skipped — not meaningful for a
  single local user. Revisit if this is ever exposed to a network.
- *Encryption at rest*: skipped — no data is persisted; there's nothing at
  rest to encrypt.

## If you outgrow "local-only"

If you later deploy this somewhere shared or networked, revisit before
going further:
- Add authentication (this build has none).
- Add TLS (HTTPS) — currently plain HTTP, which is only acceptable because
  everything stays on `127.0.0.1`.
- Add rate limiting per user/IP.
- Move the API key into a proper secrets manager rather than a local env var.
