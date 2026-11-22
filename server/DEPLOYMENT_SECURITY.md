# Relay Production Deployment Security Checklist

This checklist applies to the FastAPI relay in this directory. CI tests the relay code and builds the Android APK, but CI does not verify a live production deployment.

## Required deployment controls

- Put the relay behind HTTPS. Do not expose the relay over plaintext HTTP to clients.
- Keep `OPENAI_API_KEY` and `ORBIT_RELAY_TOKEN` in the hosting platform's secret manager or environment configuration. Never embed either secret in the Android APK, repository, logs, or screenshots.
- Set `ORBIT_RELAY_TOKEN` to a random secret at least 32 characters long. The relay intentionally fails closed when it is absent, blank, or weak.
- Only configure `ORBIT_RELAY_TOKEN_PREVIOUS` and `ORBIT_RELAY_TOKEN_PREVIOUS_EXPIRES_AT` for a planned rotation, and remove the previous token after the window closes.
- Keep the container running as the non-root `relay` user (the Dockerfile sets this).
- Use a platform or reverse proxy that enforces TLS and reasonable connection/request timeouts. Avoid logging Authorization headers, request bodies, prompt contents, or secrets.
- Restrict management access to the hosting account and protect environment/secrets changes with MFA where available.

## Rate-limiter scope and scaling

`RelayRateLimiter` is in-memory and process-local. Its fixed-window counters and lock protect concurrent requests handled by one relay process, but counters are not shared across multiple processes, replicas, or hosts and reset when the process restarts.

- For a single relay process, the configured limit is enforced per process.
- Do not scale to multiple workers or replicas and assume the same token-wide limit applies globally.
- Before multi-worker or multi-replica deployment, replace or front this limiter with a shared atomic store (for example, Redis) and test its failure behavior. A shared-store outage must not silently disable the intended abuse controls.
- The repository Dockerfile explicitly starts Uvicorn with `--workers 1`; keep that image command intact. Confirm the hosting platform does not start additional replicas or wrap it in a multi-worker process. The in-process limiter is not a global limit across replicas.

## Live verification before declaring production ready

1. Confirm the deployed revision matches the reviewed commit.
2. Confirm HTTPS and certificate validity from a client.
3. Confirm `/health` responds as expected.
4. Confirm missing and invalid relay tokens are rejected.
5. Confirm the rate limit is enforced on the actual deployment.
6. Confirm secrets and sensitive request data are not present in application/proxy logs.
7. Record the hosting provider, worker/process count, and test date in a private deployment record.

CI status is not a substitute for these live checks. Do not report production configuration as verified until these checks are performed against the actual deployment. Record the deployed image/revision and hosting worker/replica settings; a successful Docker build proves the image can be built, not that the live platform uses it.
