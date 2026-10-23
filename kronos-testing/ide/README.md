# IDE compiler tests

This Kotlin Toolchain module runs 26 real IDEA and Kotlin External FIR Support (KEFS) editor contracts against the complete compiler-plugin JAR. It verifies projection types, JOIN, aliases, subqueries, windows, collection literals, completion, diagnostics, hover documentation, and opening generated property declarations such as `row.uid` and `it.rn`.

Run from the repository root:

```sh
scripts/run ide test
```

The SDK and KEFS versions come from `libs.versions.toml`. Set `--ide-path /path/to/idea` or `IDEA_HOME` to use an installed SDK; on macOS, use the application's `Contents` directory. Linux CI runs `xvfb-run -a scripts/run ide test`. Use `--skip-build` to reuse built artifacts; `--cli-jar`, `--core-jar`, and `--syntax-jar` select their paths.

`scripts/ide.main.kts` prepares the IDEA SDK, KEFS and sandbox, compiles this module with the official Toolchain, then runs JUnit ConsoleLauncher using the SDK’s original file layout. Compilation resolves unchanged SDK JARs through the private `build/ide/repository` Maven repository. The runner reads the compiled output, resolved dependencies and selected JDK from the pinned Toolchain compilation record. The module runs separately from the database and CLI compiler tests so that the IDE's embedded compiler owns its classpath. It does not depend on the compiler test framework.

The compiler is copied byte for byte to the Maven coordinate KEFS resolves:

```text
com.kotlinorm:kronos-compiler-plugin:<IDE Kotlin compiler version>-<Kronos version>
```

The script reads the IDE compiler version from the SDK's `META-INF/compiler.version`. Every editor contract checks the loaded FIR classes against the CLI artifact. `scripts/run ide verify` requires all 26 contracts, rejects skips or failures, and checks the artifact's class-byte evidence. SDK, compatibility, shared-settings and verification evidence is written under `build/ide`.

For repository development, `scripts/run build bootstrap` stages the compiler automatically. The standalone command is:

```sh
scripts/run ide bootstrap \
  --repository build/repository/bootstrap \
  --library-version 0.4.0-SNAPSHOT \
  --kotlin-version 2.4.21 \
  --ide-import-repository "$HOME/.m2/repository"
```

Bootstrap preserves the ordinary compiler publication's POM metadata. `KRONOS_IDE_IMPORT_REPOSITORY` selects the Maven Local import destination; KEFS artifacts remain in the private bootstrap repository.

`testSharedProjectSettingsRoundTrip` checks the real KEFS serializer against `.idea/kotlin-plugins.xml`, including Exact matching and the project path macro. `.github/workflows/kronos-ide-support.yml` builds the compiler/runtime, runs these editor contracts, and uploads the evidence.
