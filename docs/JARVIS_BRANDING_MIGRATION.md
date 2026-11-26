# JARVIS branding and updater migration

This repository is maintained as JARVIS. The Orbit name is retained only where required for legal attribution, Android/package compatibility, serialized-data compatibility, or an explicitly documented migration boundary. Do not use a blind global rename.

## Migration rules

1. User-visible product identity and new release presentation should say JARVIS.
2. Keep Android application IDs `com.orbit.assistant` and `com.orbit.assistant.local`, the Java namespace, legacy preference/database keys, IPC actions, backup format identifiers, and schema-1 update manifest compatible unless a separately tested migration explicitly changes them.
3. Keep MPL-2.0 license text, copyright notices, and required upstream attribution. Branding changes do not remove license obligations.
4. The GitHub updater now reads release metadata, manifests, and APKs from `sakib-s-projects/JARVIS-Mobile-Secure`. Do not weaken its package, version, checksum, certificate, HTTPS-host, or redirect validation to make an incompatible release install.
5. A successful CI artifact is not a published release and does not prove live update discovery or installation. Live verification requires a deliberately published JARVIS Release containing the exact APK, checksum, schema-1 manifest, and optional Local component assets expected by the updater.
6. Keep existing Relay secret names accepted during migration. JARVIS-prefixed names may be preferred, but ORBIT-prefixed variables remain fallback-compatible until a verified deployment migration is complete.

## Phase A: safe identity and compatibility

- [x] App display label and text-selection action use JARVIS.
- [x] Relay title, assistant system identity, and health service name use JARVIS.
- [x] Existing ORBIT Relay environment variables remain accepted as fallbacks.
- [x] Regression tests cover JARVIS Relay token, legacy ORBIT token, and JARVIS health metadata.
- [x] CI confirms Relay authentication tests, Relay Docker image build, JVM unit tests, debug APK build, and artifact upload.

## Phase B: product documentation and release identity

- [x] README documents the JARVIS project and migration boundaries.
- [x] Gradle root project name and APK workflow display name use JARVIS.
- [x] Local companion display strings use JARVIS.
- [x] Quick Settings tile visible labels use JARVIS.
- [x] GitHub updater and release-notes endpoints point to the JARVIS repository.
- [x] New JARVIS signing certificate is pinned consistently in the updater, Local component, release workflow, verification workflow, and tests.
- [x] Release verification workflow builds and verifies both signed APKs without publishing.
- [ ] Publish a deliberate JARVIS Release and verify its real assets, manifest, checksums, Stable/Beta discovery, and download flow.
- [ ] Complete the safe fresh-install migration with a real backup exported from an older-signed installation and restored into a new-signed installation.
- [ ] Review remaining user-visible strings, updater messages, screenshots, sample content, issue templates, and badges.
- [x] Preserve explicit upstream-origin and license notices where required.

## Phase C: internal identifiers (only when safe)

- [x] Review Gradle root project name and build script naming at the project level.
- [ ] Rename Java classes/packages/resources only with a full reference search and test coverage.
- [x] Keep Android application IDs, backup schema identifiers, legacy storage keys, and IPC compatibility stable.
- [x] Keep the historical schema-1 manifest name `orbit-update.json` and APK asset name `Orbit-Assistant-v<version>.apk` for compatibility.

## Signing identity change: important installation boundary

The historical signing certificate fingerprint was:

`7D:AD:61:93:85:DF:F1:1E:C7:31:AA:55:5F:2B:44:8A:94:3C:73:91:81:3D:1A:94:DF:1C:B4:23:2E:CD:41:E3`

The historical private signing key could not be recovered. The new JARVIS release certificate is pinned as:

`84:D3:41:4B:60:EA:65:74:E0:A0:E2:72:18:84:F4:D8:F8:9D:39:09:AA:72:64:3F:CE:F0:2C:14:93:A0:68:8F`

Android will not install an APK signed by the new certificate as an in-place update over an installation signed by the old certificate. Do not weaken signature verification, and do not tell users to uninstall before they have exported and verified their data backup.

### Safe manual migration path

The app already has a local file-based backup and restore flow under **Settings → Backup & Restore**. The backup format remains schema version 1.

1. While the old-signed app is still installed, open **Settings → Backup & Restore → Export Orbit backup**.
2. Save the `.orbitbackup` file somewhere outside the app's private storage (for example, a user-controlled Downloads folder or private cloud-drive folder). Confirm the file exists and is not empty. The backup is JSON and contains private app data; keep it private and do not share it.
3. Do not uninstall the old app until the backup file has been checked and safely stored. Uninstalling can erase app-private data.
4. Only after a compatible JARVIS Release has actually been published, install its APK as a fresh installation. The updater cannot bridge the old-to-new signing identity.
5. Open **Settings → Backup & Restore → Restore Orbit backup**, select the saved file, review the restore preview, and explicitly confirm.
6. Verify important chats, Memory, routines, reminders, site tasks, saved places, and Vault content. Re-grant Android permissions, reselect default-assistant status if desired, and re-enter account/provider credentials: credentials and Android permissions/default-assistant status are deliberately not included in portable backups.
7. Keep the backup until the restored data has been checked. Do not assume cloud sync or an automatic migration happened.

This is a documented manual migration route, not a completed end-to-end test. The backup/restore implementation is present, but H-1 has not yet verified a real old-signed-device export followed by a new-signed fresh install and restore.

## Current H-1 status

- H-1 pull request: [PR #2](https://github.com/sakib-s-projects/JARVIS-Mobile-Secure/pull/2), Draft and unmerged.
- Latest H-1 updater/code commit: `b3de10d10ea0bc5abada096aa1d0c1935cf5b80d`; this migration-guide update is committed separately on the same H-1 branch.
- Signed Release Verification #7 passed: [workflow run](https://github.com/sakib-s-projects/JARVIS-Mobile-Secure/actions/runs/45871453135).
- Build JARVIS APK #226 passed: [workflow run](https://github.com/sakib-s-projects/JARVIS-Mobile-Secure/actions/runs/45871453133).
- The repository currently has no published GitHub Releases. Therefore live release discovery/download verification remains blocked until a release is intentionally published.
- No release/tag has been published for H-1, and the PR has not been merged. CI success is not production-release approval.
