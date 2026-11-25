# JARVIS FastAPI Relay — Render staging template

This file describes an **optional staging-only** deployment of the FastAPI relay. It is not a Production deployment or a promise of permanent free hosting.

## Important free-plan limits

Render's Free web services are for testing/hobby use, not production. They spin down after 15 minutes without inbound traffic, may take about a minute to wake, have ephemeral local storage, and can restart. The service must remain stateless. The relay's in-memory rate limiter resets on restart and is process-local; this template deliberately keeps one instance. Do not use this free service for Production or assume its rate limit is global. See https://render.com/docs/free.

## What the Blueprint creates

- One Docker web service from `server/Dockerfile`, using the existing non-root container and one Uvicorn worker.
- Singapore region, `/health` health check, and the Free plan.
- Automatic deploys disabled so pushes to the development branch cannot silently redeploy the service.
- No database, disk, custom domain, paid provider, or billing upgrade.
- The Cloudflare destination is the existing Staging Adapter only.

## Secrets required in the Render dashboard

The Blueprint intentionally prompts for two values and stores neither in Git:

1. `JARVIS_RELAY_TOKEN`: a separate random token used by the Android app to authenticate to this FastAPI Relay. Configure the app's existing Backend URL and Backend Token settings with the deployed HTTPS URL and this same token. Never reuse the Cloudflare token.
2. `JARVIS_CLOUDFLARE_RELAY_TOKEN`: must exactly match the existing `JARVIS_RELAY_SHARED_SECRET` configured on the Cloudflare Staging Adapter. Do not rotate or replace the Cloudflare secret as part of this setup. If the existing value is not available to the account owner, stop and plan an explicitly approved coordinated rotation; do not guess.

Do not set `OPENAI_API_KEY`; this staging path uses the existing Cloudflare Staging Adapter and its approved provider fallback. Do not copy any secret into GitHub, chat, source files, or build logs.

## Safe setup sequence

1. Connect the Render account to ChatGPT (or open the Render Dashboard yourself).
2. Create a Blueprint from this repository and select the `jarvis-branding` branch. Review the service details before creating it.
3. Confirm the service is named `jarvis-relay-staging`, plan is Free, region is Singapore, and automatic deploys are disabled.
4. Enter the two secret values directly in Render's protected environment-variable form. If the Cloudflare secret is not already known, cancel before creating/deploying; do not change it.
5. Deploy only the staging service. Record its assigned `https://<service>.onrender.com` URL privately.
6. Test `GET /health`, missing/invalid `JARVIS_RELAY_TOKEN` rejection, and valid Android-to-FastAPI-to-Cloudflare text-only request. Do not include screenshots in this first live test.
7. Keep the service staging-only. Production deployment, paid plans, billing, and Cloudflare Production changes require separate approval.

## Known constraints

- A Render Free service sleeps after 15 minutes idle; the first request can be slow or time out. This is unsuitable for dependable always-on assistant use.
- The relay currently uses an in-memory, process-local rate limiter. One instance is required; restarts reset its counters.
- No persistent user data should be stored on this service.
- A successful Docker build or GitHub Actions test is not proof of a live Render deployment.
- This template does not deploy anything by itself. Creating the Blueprint and entering secrets requires an account-owner action.
