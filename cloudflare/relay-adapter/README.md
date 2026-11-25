# JARVIS Cloudflare Relay Adapter (staging)

This is a separate, staging-only adapter. It does not modify either existing JARVIS AI Worker.

## Configuration

- `JARVIS_RELAY_SHARED_SECRET`: Worker secret, minimum 32 random characters. It must be unique and must never equal the JARVIS admin, session, or provider-encryption secret.
- `JARVIS_STAGING_CHAT_URL`: HTTPS URL for the existing staging Worker's `/api/chat` route (the non-secret URL is in `wrangler.toml`).

Deploy only as `jarvis-relay-adapter-staging`. Do not point the FastAPI Production Relay or Android app at this adapter until contract and live staging tests pass.

## Current deliberate limitation

The existing staging chat endpoint accepts a request body up to 1 MiB and is text-only. This adapter therefore rejects any screenshot with HTTP 413 instead of silently dropping it, and caps its inbound body at 900 KiB. It preserves text, history, time, locale, memory/task/screen/notification context and announced Android capabilities within that cap.

The model is prompted to return the Android Relay JSON contract. The adapter parses and validates the shape and filters actions against the exact capabilities supplied by Android. This remains a prompt-mediated model output and must pass staging contract/security tests before production use.

## Deployment status

Source only. No Worker was deployed, no secret was created, and no existing Worker or D1 resource was changed as part of authoring this adapter.
