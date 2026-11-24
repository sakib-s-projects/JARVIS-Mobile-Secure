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
- [ ] Add/verify regression tests for both JARVIS and legacy environment variables.
- [ ] Confirm CI after the branding changes.

### Phase B: product documentation and release identity
- [ ] Rewrite README and user-facing guides for the JARVIS project.
- [ ] Replace upstream release/download links only after JARVIS releases are actually published and verified.
- [ ] Review screenshots, sample content, issue templates, badges, and workflow labels.
- [ ] Preserve explicit upstream-origin and license notices where required.

### Phase C: internal identifiers (only when safe)
- [ ] Review Gradle root project name and build script variable names.
- [ ] Rename Java classes/packages/resources only with a full reference search and test coverage.
- [ ] Keep Android application IDs, signing identity, backup keys, and IPC compatibility stable unless a dedicated migration is designed and tested.

## Current compatibility boundary

The current app package and namespace are `com.orbit.assistant`; the updater also verifies that package and a pinned signing certificate. These are not cosmetic labels. Do not change them as part of a branding-only edit.

The GitHub edition updater currently fetches release metadata and APKs from `lpnovi/Orbit-Assistant`. Changing that repository constant before a JARVIS release is signed with the same expected certificate and publishes the exact verified manifest/assets would make updates fail closed or, if verification were weakened, could compromise update integrity. Therefore the updater endpoint is intentionally unchanged pending a complete release migration.

## Verification record

Each phase must be built and tested on the `jarvis-branding` branch. A green APK CI run is evidence for that source revision only; it is not evidence that a live Relay is deployed or that a phone upgrade was tested.
