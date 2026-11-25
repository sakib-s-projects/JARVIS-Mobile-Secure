# JARVIS Cloudflare Worker Integration Review

Date: 2026-11-25
Branch reviewed: `jarvis-branding`
Scope: read-only inspection of the existing FastAPI Relay and the two Cloudflare Workers.

## Decision

Do not point the Production FastAPI Relay at either existing Worker yet. The staging Worker is the closest provider backend, but its public chat contract is not compatible with the Android Relay contract. The production Worker is an older implementation and is not a drop-in replacement either.

No Worker code, Worker secret, D1 data, production deployment, or billing setting was changed during this review.

## Cloudflare inventory observed

- `jarvis-ai-connection`: existing production-named Worker, last modified 2026-11-09.
- `jarvis-ai-connection-staging`: staging Worker, last modified 2026-11-21.
- Staging Worker has an `ASSETS` binding, a D1 `DB` binding, and secret bindings for Gemini, Groq, Google OAuth, JARVIS admin, provider-key encryption, and JARVIS sessions.
- The staging Worker reports a staging identity in its health/status route. It must not be treated as the Production Relay endpoint.
- Worker secret values were not read or exposed.

## Contract mismatch

### Android -> FastAPI Relay

The Android `RelayProvider` posts to `POST /assistant` and expects a JSON object with `text` and `actions`. Its payload can contain:

- prompt, model, reasoning, and up to 10 history entries;
- memory, task, screen, notification, time, locale, and capability context;
- an optional base64 screenshot (the Android client can send a multi-megabyte payload).

The FastAPI Relay validates that request, authenticates `Authorization: Bearer ...`, applies its relay rate limiter, asks the model for strict JSON, validates the response with Pydantic, and filters actions against the announced Android capabilities.

### Existing Cloudflare staging chat route

The staging Worker exposes `POST /api/chat` and `POST /api/chat/stream`. Its current validator expects a different body:

- `message` (required, maximum 12,000 characters);
- optional `lang`, `persona`, `responseStyle`, and `history` entries shaped as `{role,text}`.

The Worker returns a provider-oriented `{reply, model, provider, usedFallback}` response, not the Relay's `{text,actions}` response. The chat route is IP-rate-limited, but it does not require the FastAPI Relay bearer token. Its request body limit is 1 MiB. Its Groq fallback maps text-only message parts; the current route does not implement the Relay's screenshot contract or OpenAI Responses web-search tool contract.

The staging Worker chat implementation currently attempts Gemini (including its configured fallback model) and then Groq. The existence of provider-management endpoints or stored provider credentials does not mean every configured provider is part of the chat fallback chain.

### Existing Cloudflare production Worker

The production-named Worker is an older implementation with a different API shape and provider code. It should not be used as a substitute for the staging Worker without a separate contract and security review.

## FastAPI Relay blockers for a no-paid-provider deployment

- `server/app.py` initializes the OpenAI SDK client at import time using `OPENAI_API_KEY`.
- `POST /assistant` returns HTTP 500 when `OPENAI_API_KEY` is absent.
- The request path currently invokes the OpenAI Responses API directly, including its web-search tool.
- Therefore, merely setting a Cloudflare Worker URL does not remove the OpenAI dependency or preserve existing request/response behavior.

## Safe integration design

Before routing Relay traffic through Cloudflare, add a dedicated, versioned endpoint to a reviewed Worker release, rather than repurposing the existing public chat route. The new endpoint should:

1. Require a dedicated high-entropy relay-to-worker secret, compared in constant time. Do not reuse `JARVIS_ADMIN_TOKEN`, OAuth/session secrets, or provider API keys.
2. Accept the Relay's supported fields with explicit body and per-field limits. It must explicitly document any fields that are unsupported; it must not silently discard screenshots, history, or safety context.
3. Return a strict, versioned JSON response containing `text` and `actions`, with schema validation before returning it to Android.
4. Preserve the Relay's action allowlist/capability filtering and confirmation rules. Treat screen, memory, notification, and task context as untrusted input.
5. Apply bounded upstream timeouts, bounded retries, and a shared/accounted rate limit. Do not assume process-local in-memory limits are global.
6. Use the existing approved Gemini/Groq fallback policy only. Do not add paid providers, activate billing, or silently route to OpenAI.
7. Have contract tests for valid/invalid authentication, malformed payloads, oversized bodies, provider outage/fallback, malformed model JSON, action filtering, and screenshot handling.
8. Be deployed to staging first and tested without exposing secrets. Production rollout requires a separate explicit release step and live verification.

The FastAPI Relay should then gain a provider adapter selected by configuration. The OpenAI SDK dependency and `OPENAI_API_KEY` must become optional only after the Cloudflare adapter and its tests are complete. The default must fail closed with a clear configuration error if no approved provider is configured.

## Deployment status

The GitHub CI Docker build proves that the container image can be built. It does not prove that a Production FastAPI Relay host exists. The existing Cloudflare Workers are separate services, and the staging Worker is not a deployed FastAPI container.

No Production Relay URL or hosting provider was established in this review. Live Production Relay verification remains blocked until a no-cost hosting target and secure secret configuration are available.
