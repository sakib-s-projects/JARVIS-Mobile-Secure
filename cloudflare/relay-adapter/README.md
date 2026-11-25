# JARVIS Cloudflare Relay Adapter (staging)

This is a separate, staging-only adapter. It does not modify either existing JARVIS AI Worker.

## Configuration

- `JARVIS_RELAY_SHARED_SECRET`: Worker secret, minimum 32 random characters. It must be unique and must never equal the JARVIS admin, session, or provider-encryption secret.
- `JARVIS_STAGING_CHAT`: Cloudflare Service Binding to the existing `jarvis-ai-connection-staging` Worker. The adapter calls its `/api/chat` route privately through the binding; it does not make a public `workers.dev` subrequest.
- `AI`: Workers AI binding used only for screenshot analysis.
- `JARVIS_VISION_ENABLED`: explicit opt-in, `false` by default. Enable only after confirming the account is on Cloudflare Workers Free.

## Screenshot flow

Android sends one JPEG screenshot as raw Base64 in `screenshotBase64`. When vision is enabled, the adapter validates the Base64 and 4 MiB encoded-image cap, calls Cloudflare Workers AI `@cf/moondream/moondream3.1-9B-A2B`, then passes only the bounded visual description to the existing text-only staging chat Worker. The description is explicitly labelled untrusted; visible text inside an image is never treated as instructions. The image is not forwarded to the text-only Worker and is not stored by this adapter.

If vision is disabled, missing, or unavailable, screenshot requests fail explicitly. They are never silently downgraded to text-only requests.

## Cost and privacy guard

Cloudflare documents a 10,000-Neuron daily free allocation for Workers AI on the Workers Free plan. On the Free plan, requests stop when the allocation is exhausted. A Paid plan may bill above the free allocation, so do not upgrade or enable paid billing. Keep `JARVIS_VISION_ENABLED=false` until the account's plan is confirmed. Screenshots are sent to Cloudflare for inference.

## Limits and rollout

- Adapter request body: 5 MiB maximum.
- Screenshot Base64: 4 MiB maximum.
- Vision output passed to text model: 6,000 characters maximum.
- Existing staging chat prompt remains capped at 12,000 characters.
- Screenshot support is staging-only and opt-in. Do not use this adapter in Production until live staging contract/security tests pass.
- The model response is prompt-mediated JSON, not provider-enforced schema output. Parsing and action filters are defense-in-depth, not a guarantee against model mistakes.

## Deployment status

Staging-only deployment. The adapter uses a zero-cost Service Binding to the existing staging chat Worker. No Production Worker, secret value, D1 resource, or billing setting is changed.
