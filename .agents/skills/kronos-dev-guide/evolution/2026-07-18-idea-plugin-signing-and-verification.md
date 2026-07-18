# IDEA plugin signing and verification must use explicit inputs

## Symptoms

- `signPlugin` succeeds, but IntelliJ Platform Gradle Plugin 2.17.0 fails `verifyPluginSignature` with the PEM certificate body reported as an invalid argument.
- `verifyPluginStructure` rejects a plugin that depends on `org.jetbrains.kotlin` without a `supportsKotlinPluginMode` extension.
- `verifyPlugin` against a local IDEA installation can wait indefinitely while downloading a transitive plugin dependency.

## Causes

- The 2.17.0 `VerifyPluginSignatureTask` content-input branch writes a temporary certificate and also passes the decoded certificate body as an extra CLI argument.
- Gradle 9.6 detects that `verifyPluginSignature` consumes the signed zip without the IntelliJ Platform plugin declaring a task dependency on `signPlugin`.
- IDEA 2024.2.1 and later require Kotlin-dependent plugins to declare whether they support K1, K2, or both.
- Plugin Verifier resolves dependencies from Marketplace unless offline mode is enabled, even when the required direct dependencies are bundled with the local IDE.

## Solution

- Keep CI signing inputs as `CERTIFICATE_CHAIN`, `PRIVATE_KEY`, and `PRIVATE_KEY_PASSWORD`, but also support `CERTIFICATE_CHAIN_FILE` and `PRIVATE_KEY_FILE` for secure local validation. Use the file inputs with `verifyPluginSignature` on 2.17.0.
- Make `verifyPluginSignature` explicitly depend on `signPlugin` so Gradle validates and orders the signed-archive producer correctly.
- For a FIR/Analysis API plugin, declare `<supportsKotlinPluginMode supportsK1="false" supportsK2="true" />` under the `org.jetbrains.kotlin` extension namespace.
- If online verification stalls in dependency download, rerun the same Plugin Verifier version with `-offline` against the local target IDE and confirm that all required dependencies resolve locally.
- For every formal repository release, let `publishPlugin` build, sign, and upload the root project version to the public Marketplace `default` channel. Require all Marketplace and signing secrets, and attach only the current version's `*-signed.zip` to the GitHub Release. Snapshot builds must not upload.

## Prevention

Before uploading an IDEA plugin, run `signPlugin`, `verifyPluginStructure`, `verifyPluginSignature`, and Plugin Verifier against the exact target build. Inspect the packaged `plugin.xml` and manifest to confirm the plugin version, Kotlin mode, platform version, and platform build. Keep Marketplace publication in the formal release job, fail on missing secrets or upload errors, and never collect unsigned or stale distribution zips.
