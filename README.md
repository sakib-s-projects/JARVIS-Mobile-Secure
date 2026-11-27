<div align="center">

# JARVIS

**A privacy-conscious Android AI assistant, built around safe device actions and user control.**

Bangla-first personal assistant project with a modular Android app, optional local companion, and a separately secured AI relay.

![Android 10+](https://img.shields.io/badge/Android-10%2B-3DDC84?style=flat-square)
![License](https://img.shields.io/badge/License-MPL--2.0-blue?style=flat-square)
![Status](https://img.shields.io/badge/Project-Under%20Development-orange?style=flat-square)

</div>

## Project identity

This repository is the JARVIS project source. The Android application display name is **JARVIS Assistant**. The project is being migrated from an upstream Orbit Assistant codebase, so some internal identifiers and compatibility strings still contain the former name.

The migration is intentionally staged. User-facing identity is being changed to JARVIS. Android package IDs, persistent storage keys, backup formats, IPC contracts, and updater manifest/asset formats remain compatible where required. **The Android signing identity has changed** because the historical private key could not be recovered; APKs signed with the new JARVIS certificate cannot update installations signed with the historical certificate. See the release and updater status below before changing installations.

## Project priorities

- Bangla-first interaction, with English support.
- User-controlled text and voice interaction. No always-listening microphone requirement.
- Safe Android actions with confirmation for consequential changes.
- Reminders and task management for everyday and work-site use.
- Privacy-conscious provider connections and optional local capabilities.
- Secure backup/restore and a separately authenticated AI relay.
- No financial transactions performed by the assistant.

## Repository layout

| Path | Purpose |
| --- | --- |
| `app/` | Main Android application |
| `local/` | Optional local AI companion component |
| `server/` | FastAPI-based AI relay and its security tests |
| `ipc/` | Shared Android IPC contracts |
| `.github/workflows/` | Build, test, and security verification workflows |
| `docs/JARVIS_BRANDING_MIGRATION.md` | Branding migration rules and compatibility plan |

## Build and test

The project uses the Android Gradle build system and Java 17. The CI workflow runs the relay authentication tests, builds the relay container image, runs JVM tests, and builds a debug APK.

For local Android build prerequisites and commands, see [Building from source](docs/BUILDING.md).

## Relay configuration

The FastAPI relay reads its provider key only on the server. Never place provider API keys in the Android app or commit secrets to this repository.

Current environment variables:
- `OPENAI_API_KEY`: provider API key.
- `JARVIS_RELAY_TOKEN`: preferred relay access token, at least 32 characters.
- `ORBIT_RELAY_TOKEN`: legacy-compatible fallback during migration.
- `JARVIS_RELAY_TOKEN_PREVIOUS` and `JARVIS_RELAY_TOKEN_PREVIOUS_EXPIRES_AT`: optional bounded token-rotation values; legacy ORBIT-prefixed equivalents remain accepted.
- `JARVIS_SAFETY_ID`: optional provider safety identifier.

The relay is not considered production-ready merely because CI passes. Live HTTPS, authentication, rate limiting, deployment topology, and provider configuration must be verified against the actual deployment before production status is claimed.

## Release and updater status

The in-app updater and release-notes feed now resolve releases from this JARVIS repository. The update manifest remains `orbit-update.json` (schema 1), and the historical APK asset filename `Orbit-Assistant-v<version>.apk` is retained for manifest/asset compatibility. **Signing identity changed:** the historical private signing key could not be recovered, so this repository now uses a new JARVIS signing certificate. Builds signed with the previous certificate cannot be updated in-place by this new certificate; users on an older differently-signed build will need a separately planned fresh-install/migration path. Do not promise an in-app upgrade from the old signing identity. The repository must publish a release signed with the new pinned JARVIS identity before updates are live. No production release is implied by a CI artifact.

Do not install unofficial APKs or treat a successful CI artifact as a signed production release. A debug APK is for development/testing only.

## Upstream origin and license

This project is derived from Orbit Assistant, originally published at [lpnovi/Orbit-Assistant](https://github.com/lpnovi/Orbit-Assistant). The upstream project is licensed under the Mozilla Public License 2.0. This repository retains the applicable license text and required copyright/attribution notices. Branding changes do not remove upstream license obligations.

See [LICENSE](LICENSE) and the source-file notices for the applicable terms.
