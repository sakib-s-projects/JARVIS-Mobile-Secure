# JARVIS branding migration

This repository is being maintained as JARVIS. The upstream Orbit name is retained only where it is required for legal attribution, binary compatibility, migration, or an explicitly documented upstream-origin reference.

## Migration rules

1. User-visible product identity, assistant prompts, relay service metadata, documentation, screenshots, and release presentation should say JARVIS.
2. Do not perform a blind global replacement. Existing Android installs depend on the application ID `com.orbit.assistant`, the optional local companion ID, the signing certificate, updater manifest/package checks, and persistent preference/database keys.
3. Keep the current Android application IDs and namespace until a separately tested migration proves upgrade compatibility. Changing the application ID would make Android treat JARVIS as a different app and can strand existing app data or break the optional local companion.
4. Keep legacy preference keys, backup schema identifiers, IPC actions, and serialized values readable. New JARVIS-facing names may be added only with backward-compatible migration code and tests.
5. Do not point the in-app updater at a JARVIS repository until that repository has verified Stable/Beta release assets, matching APK signing certificate, checksums, manifest schema, and tested redirect allowlist. The current updater is cryptographically pinned to the upstream release identity.
6. Keep MPL-2.0 license text, copyright notices, and required upstream attribution. Branding changes do not remove license obligations.
7. Keep existing Relay secret names accepted during migration. JARVIS-prefixed names may be preferred, but ORBIT-prefixed variables remain fallback-compatible until a verified deployment migration is complete.

## Current phases

### Phase A: safe identity and compatibility
- [x] App display label and text-selection action use JARVIS.
- [x] Relay title, assistant system identity, and health service name use JARVIS.
- [x] Existing ORBIT Relay environment variables remain accepted as fallbacks.
- [x] Regression tests cover JARVIS Relay token, legacy ORBIT token, and JARVIS health metadata.
- [x] CI confirms Relay authentication tests, Relay Docker image build, JVM unit tests, debug APK build, and artifact upload.

### Phase B: product documentation and release identity
- [x] Rewrite README for the JARVIS project and document migration boundaries.
- [x] Update Gradle root project name and APK workflow display name to JARVIS.
- [x] Update Local companion display strings to JARVIS.
- [x] Update Quick Settings tile visible labels and widget provider comment to JARVIS.
- [ ] Review remaining user-visible strings, updater messages, screenshots, sample content, issue templates, and badges.
- [ ] Replace upstream release/download links only after JARVIS releases are actually published and verified.
- [x] Preserve explicit upstream-origin and license notices where required.

### Phase C: internal identifiers (only when safe)
- [x] Review Gradle root project name and build script naming at the project level.
- [ ] Rename Java classes/packages/resources only with a full reference search and test coverage.
- [x] Keep Android application IDs, signing identity, updater trust pin, backup keys, and IPC compatibility stable pending a dedicated migration.

## Current compatibility boundary

The current app package and namespace are `com.orbit.assistant`; the updater also verifies that package and a pinned signing certificate. These are not cosmetic labels. Do not change them as part of a branding-only edit.

The GitHub edition updater currently fetches release metadata and APKs from `lpnovi/Orbit-Assistant`. Changing that repository constant before a JARVIS release is signed with the same expected certificate and publishes the exact verified manifest/assets would make updates fail closed or, if verification were weakened, could compromise update integrity. Therefore the updater endpoint is intentionally unchanged pending a complete release migration.

## Verification record

- Latest successful build: Build JARVIS APK Run #111, commit `867dc787be2944e6ea5dd0aeb0d0a4e1d9069546`.
- Run #111 completed all steps successfully: Relay authentication tests, Relay Docker image build, JVM unit tests, debug APK build, and APK artifact upload.
- Runs #108–#110 also completed successfully.
- H-3 audit workflow check entries #43–#46 show failure with no jobs returned by the GitHub Jobs API. These are not evidence of a completed audit; the workflow trigger/check configuration needs separate investigation.
- A green APK CI run is evidence for that source revision only; it is not evidence that a live Relay is deployed or that a phone upgrade was tested.
