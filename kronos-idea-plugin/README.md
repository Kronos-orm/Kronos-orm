# kronos-idea-plugin

IntelliJ IDEA plugin for Kronos ORM.

This module merges the standalone Kronos-Orm IDEA plugin with the monorepo IDE
support module:

- `Kronos-ORM` tool window with the Kronos icon.
- Project settings page at `Kronos ORM Setting`.
- Code generation script templates under `templates/`.
- K2 analysis support for Kronos compiler-plugin features, including generated
  projection classes, projection context members, and FIR diagnostics.

It is intentionally separate from `kronos-gradle-plugin`: the Gradle plugin makes
command-line compilation work, while this IDE plugin makes editor analysis and
IDE tooling work.

## Resources

- Plugin logo: `src/main/resources/META-INF/pluginIcon.svg`
- Tool window icon: `src/main/resources/icons/kronos-circle.svg`
- Full icon asset: `src/main/resources/icons/kronos.svg`
- Code generation templates: `templates/all`, `templates/pojo`, `templates/service`

## Build

Use a local IDEA installation when developing against the same IDE build:

```powershell
.\gradlew.bat :kronos-idea-plugin:buildPlugin -Pkronos.idea.localPath="C:/Program Files/JetBrains/IntelliJ IDEA 2026.2"
```

or set `IDEA_HOME`. Without a local path, Gradle downloads the IDEA dependency
selected by `-Pkronos.idea.version=...`, defaulting to `2026.2`.

On macOS:

```bash
./gradlew :kronos-idea-plugin:buildPlugin \
  -Pkronos.idea.localPath="$HOME/Applications/IntelliJ IDEA.app/Contents"
```

The packaged plugin is written to:

```text
kronos-idea-plugin/build/distributions/kronos-idea-plugin-<version>.zip
```

After signing, the release artifact is
`kronos-idea-plugin-<version>-signed.zip` in the same directory.

## Marketplace release

The `publish.yml` workflow publishes the IDEA plugin for every formal Kronos
release. Snapshot builds do not upload the plugin. The plugin version is the
current root project version; it is not maintained separately.

Configure these GitHub Actions repository secrets before merging a formal
release version to `main`:

| Secret | Value |
|---|---|
| `JETBRAINS_MARKETPLACE_TOKEN` | A permanent token created in the JetBrains Marketplace profile |
| `IDEA_CERTIFICATE_CHAIN` | The full PEM certificate chain used to sign the plugin |
| `IDEA_PRIVATE_KEY` | The full PEM private key matching the certificate |
| `IDEA_PRIVATE_KEY_PASSWORD` | The private-key password |

Keep the signing material in secure maintainer storage and never commit it. The
release job fails when any required secret is missing. It runs `publishPlugin`,
which builds and signs the plugin and uploads it to the public `default`
Marketplace channel. The same signed zip is then attached to the GitHub Release.

For a local signing check, provide `CERTIFICATE_CHAIN_FILE`, `PRIVATE_KEY_FILE`,
and `PRIVATE_KEY_PASSWORD`, then run `signPlugin` and
`verifyPluginSignature`. Do not run `publishPlugin` for local verification.

## Release artifacts

Formal release builds attach the signed IDEA plugin zip and JVM jar artifacts to
the GitHub Release.

The release IDEA plugin version uses the Kronos release version:

```text
<release-version>
```

The GitHub Release notes are generated from merged changes. The attached
artifacts include the IDEA plugin zip, JVM jars, and checksum files.
