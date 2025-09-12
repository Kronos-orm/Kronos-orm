# Kronos-orm Release & Snapshot Publishing via GitHub Actions

This repository includes an automated publishing workflow that runs after pull requests are merged into the `main` branch.

There are two flows:
- Snapshot publish: if the current project version ends with `-SNAPSHOT`, publish to Maven Central Snapshots.
- Release publish: if the current project version does NOT contain `-SNAPSHOT`, publish a release to Maven Central, then immediately bump the project version to the next `-SNAPSHOT`.

## Summary
- Version is managed in these places and kept in sync by the workflow:
  - build-logic/src/main/kotlin/publishing.gradle.kts → `project.version = "..."`
  - kronos-gradle-plugin/src/main/kotlin/com/kotlinorm/compiler/plugin/KronosGradlePlugin.kt → `version = "..."`
- Publishing tasks used (already provided by build-logic):
  - Snapshots: `publishAllToCentralSnapshots`
  - Releases: `publishAllToMavenCentral`

## Triggers
The workflow `.github/workflows/publish.yml` triggers on PRs that are closed and merged into `main`.
- The workflow detects the current project version from `publishing.gradle.kts`.
  - If it ends with `-SNAPSHOT`, it publishes a snapshot (no version bump).
  - If it does not contain `-SNAPSHOT`, it publishes a release and then bumps to the next `-SNAPSHOT`.

## Secrets configuration (required)
Set the following repository (or organization) secrets:
- `MAVEN_CENTRAL_USERNAME`: Sonatype username
- `MAVEN_CENTRAL_PASSWORD`: Sonatype password
- `SIGNING_KEY`: ASCII-armored GPG private key (use `gpg --export-secret-keys --armor <KEYID>`)
- `SIGNING_PASSWORD`: passphrase for the private key

Notes:
- Snapshots do not require signing in this project configuration and will only use the Sonatype credentials.
- Releases require signing; the workflow uses in-memory signing properties with the secrets above.

## Keystore / Signing guidance
The repository should not store actual keystore contents. Use GitHub Secrets instead:
- Store the GPG private key as `SIGNING_KEY` (ASCII-armored text, not a file) and its passphrase as `SIGNING_PASSWORD`.
- The workflow maps these to Gradle properties via environment variables recognized by the Vanniktech Maven Publish plugin and Gradle signing plugin:
  - `ORG_GRADLE_PROJECT_signingInMemoryKey`
  - `ORG_GRADLE_PROJECT_signingInMemoryKeyPassword`

If you must provide other keystore-like files for additional targets (e.g., Aliyun, custom repositories):
- Base64-encode the file locally and save the base64 string as a secret (e.g., `ALIYUN_KEYSTORE_B64`).
- In a workflow step, decode it to a temporary file path (e.g., `echo "$ALIYUN_KEYSTORE_B64" | base64 -d > keystore/tmp.gpg`).
- Configure Gradle to point to this runtime-generated file path via `ORG_GRADLE_PROJECT_` properties.

## Version bumping logic
We include a helper script at `.github/scripts/bump-version.sh` to update version consistently across all required locations.

Supported commands:
- `next-snapshot`: bumps patch and sets `-SNAPSHOT` (e.g., 0.0.6-SNAPSHOT → 0.0.7-SNAPSHOT)
- `release-from-current`: drops `-SNAPSHOT` if present
- `next-release`: bumps patch to next release (no `-SNAPSHOT`)
- `set <version>`: sets an explicit version (e.g., `set 0.0.7` or `set 0.0.8-SNAPSHOT`)

The workflow uses these commands to enforce consistent versioning in:
- publishing.gradle.kts
- KronosGradlePlugin.kt

## Usage examples
- Snapshot publish: merge any PR into `main` with a normal title. The workflow will bump to the next `-SNAPSHOT` and publish to Central Snapshots.
- Release publish: merge a PR into `main` with a title containing `[version]`:
  - Automatic release bump: `Fix: ready for release [version]`
  - Explicit version: `Release Kronos [version 0.0.7]`

## Local testing hints
- To dry-run version bump locally, execute:
  - `bash .github/scripts/bump-version.sh next-snapshot`
  - `bash .github/scripts/bump-version.sh release-from-current`
- Publish tasks:
  - Snapshots: `./gradlew publishAllToCentralSnapshots`
  - Releases: `./gradlew publishAllToMavenCentral`

Ensure you do NOT commit any real credentials or keystore files.
