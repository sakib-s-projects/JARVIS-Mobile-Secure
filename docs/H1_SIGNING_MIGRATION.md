# H-1 signing and updater migration runbook

## Current JARVIS update authority

The GitHub updater and release-notes client use the public repository
`sakib-s-projects/JARVIS-Mobile-Secure`. Release APKs are checked against the
JARVIS SHA-256 signing-certificate pin in the app, Local component, and release
workflow. The verification workflow builds and verifies both APKs without
publishing a release.

## Signing-certificate transition: do not assume in-place upgrade

The legacy installed application was signed with this certificate:

`7D:AD:61:93:85:DF:F1:1E:C7:31:AA:55:5F:2B:44:8A:94:3C:73:91:81:3D:1A:94:DF:1C:B4:23:2E:CD:41:E3`

The JARVIS release workflow currently pins this certificate:

`84:D3:41:4B:60:EA:65:74:E0:A0:E2:72:18:84:F4:D8:F8:9D:39:09:AA:72:64:3F:CE:F0:2C:14:93:A0:68:8F`

Android does not permit an ordinary in-place APK update when the installed APK
and candidate APK have unrelated signing certificates, even when the package
name is unchanged. The unchanged package IDs
(`com.orbit.assistant` and `com.orbit.assistant.local`) do not bypass this
platform check.

Before publishing a JARVIS APK intended to replace an existing legacy install:

1. Determine whether the private key matching the legacy certificate is
   available and under the repository owner's control. Never expose or commit
   the keystore or passwords.
2. If it is available, evaluate Android APK Signature Scheme v3 signing-key
   rotation with a proof-of-rotation lineage from the legacy key to the JARVIS
   key. Verify the resulting APK on the minimum supported Android version and
   Android 10 before enabling rollout. Do not merely change the certificate
   pin and assume Android will accept the update.
3. If the legacy private key is unavailable, treat the new certificate as a
   new-install identity. Provide a user-visible backup/export and restore
   procedure for app data first. Only then may a user knowingly uninstall the
   legacy app and install the JARVIS APK. Never silently uninstall, clear app
   data, or claim that a direct update is supported.
4. Keep the first release unpublished until the chosen migration path has
   passed physical-device testing. A successful CI build or signature check
   does not prove Android will upgrade an already-installed APK.

## Required release checks

- Build and unit tests pass on the exact candidate commit.
- Main app and Local component package IDs are the expected values.
- Both APKs have exactly one expected signer and pass `apksigner verify`.
- The update manifest has the supported schema, package, version, asset names,
  SHA-256 checksums, and signing-certificate fingerprint.
- Stable and Beta discovery reject drafts, malformed tags, wrong-channel
  releases, duplicate assets, and non-official download URLs.
- Download/install remains user-confirmed through Android's package installer.
- Test clean install and update/migration on a physical Android 10 device before
  declaring H-1 complete.

## Compatibility identifiers intentionally retained

The package IDs, Java namespaces, manifest filename `orbit-update.json`,
schema version 1, and historical release asset naming remain unchanged for
compatibility. Their presence is not an active connection to the former
upstream repository. Do not rename them as part of this signing migration.
