# Testing, CI/CD, Version Management & Documentation

## Table of Contents
1. [Testing Patterns](#testing-patterns)
2. [CI/CD Workflows](#cicd-workflows)
3. [Coverage](#coverage)
4. [Version Management](#version-management)
5. [Publishing](#publishing)
6. [Documentation](#documentation)
7. [Build System](#build-system)

---

## Testing Patterns

### Where Tests Go
- **Unit tests**: each module's `src/test/kotlin/`. Tests that don't need real databases.
- **Integration tests**: `kronos-testing/src/test/kotlin/`. Tests that connect to real DB instances.
- **Official compiler plugin tests**: `kronos-compiler-plugin/testData/` with runners in `kronos-compiler-plugin/src/test/kotlin/com/kotlinorm/compiler/`. Uses Kotlin's official compiler test infrastructure.

### Writing Unit Tests (kronos-core)

`kronos-core/module.yaml` applies `templates/kronos-runtime.module-template.yaml`, which enables the source-built Kronos compiler plugin for application and test code. JVM test dependencies are declared in `test-dependencies@jvm`; shared and platform tests live in `test/` and `test@jvm/`.

Example pattern:
```kotlin
class SelectTest {
    @Test
    fun testSelectWithCondition() {
        val user = User(age = 18)
        val clause = user.select().where { it.age > 18 }
        val task = clause.build()
        // Assert on generated SQL and parameters
        assertEquals("SELECT ... WHERE age > :age", task.sql)
    }
}
```

### Writing Official Compiler Plugin Tests

Kronos uses Kotlin's official compiler test infrastructure for compiler-plugin behavior. These tests compile `kronos-compiler-plugin/testData` sources through the real FIR/IR/codegen pipeline with `KronosCompilerPluginRegistrar` registered.

Read the Kronos official compiler testData style section in:

```text
.agents/skills/kronos-dev-kcp/SKILL.md
```

Use official compiler tests for:

- KPojo generated declarations and generated bodies.
- DSL lambda transformations: condition, select, set, sort, reference, type-parameter injection.
- FIR/frontend generated declarations or diagnostics.
- IR verifier regressions.
- Runtime behavior that depends on compiler-generated code.

Do not use official compiler tests for:

- Pure utility functions.
- Broad smoke tests that only prove compilation.
- Database integration requiring external services.

#### Official Test Layout

```text
kronos-compiler-plugin/
  testData/
    box/
      pluginLoading/
      kpojoGeneratedBodies/
      kpojoFieldMetadata/
      condition/
      select/
      set/
      sort/
      reference/
      dslIntegration/
      typeParameterFixer/
      kpojoFactory/
      regression/
    diagnostics/
  src/test/kotlin/com/kotlinorm/compiler/
    AbstractKronosJvmBoxTest.kt
    *BoxTest.kt
```

Each `testData/box/<area>/<case>.kt` file should have a matching thin runner method:

```kotlin
class SelectBoxTest : AbstractKronosJvmBoxSuite("select") {
    @Test
    fun collectionLiteralFields() = box("collectionLiteralFields")
}
```

#### Official Box Test Requirements

Every testData `.kt` file must:

- Start with the Kronos Apache 2.0 copyright header.
- Add a short top-level comment that states the compiler-plugin contract being tested.
- Test one primary contract per file unless it is explicitly an integration scenario.
- Expose `fun box(): String`.
- Return exactly `"OK"` on success.
- Return `"Fail: <specific reason>"` on failure, including observed values where useful.
- Assert generated behavior, not merely that the source compiles.

Preferred assertion style for multiple checks:

```kotlin
fun box(): String {
    val failures = listOfNotNull(
        expect(fields.size == 2) { "field count was ${fields.size}" },
        expect(fields[0].name == "id") { "first field was ${fields[0].name}" },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
```

When a compiler test depends on global naming strategies, default data source, serializers, or other `Kronos` singleton state, set only the required properties directly and restore/reset state if it can leak into later cases:

```kotlin
Kronos.fieldNamingStrategy = lineHumpNamingStrategy
Kronos.tableNamingStrategy = lineHumpNamingStrategy
```

Collection literal tests using `[]` rely on the official test configuration enabling `+CollectionLiterals`; do not assume Gradle `compileTestKotlin` arguments apply to testData compilation.

#### Positive, Negative, And Golden Tests

- Use `testData/box` for generated runtime behavior. The test should fail if the relevant plugin transformer or generator is disabled.
- Use `testData/diagnostics` for source that should fail compilation. Do not encode expected compiler failures as box tests.
- Add `.fir.txt` or `.fir.ir.txt` golden dumps only for selected structural contracts such as FIR-generated declarations, call return type refinement, complex IR body generation, or invalid-IR regressions. Do not snapshot every box test.

#### Removed kctfork Tests

The compiler plugin no longer uses kotlin-compile-testing / kctfork tests. Do not reintroduce `KotlinSourceDynamicCompiler`, `IrTestFramework`, or `libs.kct`.

When a removed behavior needs coverage:

- Use official `testData/box` or `testData/diagnostics` if it depends on compiler-plugin generated code or DSL transformation.
- Use ordinary unit tests if it is a pure utility or small deterministic helper.
- Prefer precise behavior names and assertions over broad smoke tests.

#### Running Official Compiler Tests

```bash
# All official compiler box suites
scripts/run build test -m kronos-compiler-plugin -p jvm --include-classes='*BoxTest'

# One suite, or multiple suites using repeated options
scripts/run build test -m kronos-compiler-plugin -p jvm --include-classes='*ConditionBoxTest'
scripts/run build test -m kronos-compiler-plugin -p jvm --include-classes='*KPojoGeneratedBodiesBoxTest' --include-classes='*KronosCommandLineProcessorTest'
```

Each `--include-classes` value is a class-name glob. Repeat the option for alternatives.

### Writing Integration Tests (kronos-testing)

Tests connect to real databases or in-process JDBC databases. CI starts MySQL, PostgreSQL, SQL Server, Oracle, and DM8 through Compose; H2 and SQLite run inside the test JVM.

`kronos-testing/module.yaml` declares ORM modules, JDBC drivers and connection pools in `test-dependencies`, and applies the shared runtime compiler-plugin template.

Test setup pattern:
```kotlin
class MysqlTest {
    companion object {
        val wrapper by lazy {
            BasicDataSource().apply {
                driverClassName = "com.mysql.cj.jdbc.Driver"
                url = "jdbc:mysql://localhost:3306/kronos_testing"
                username = System.getenv("MYSQL_USERNAME") ?: "kronos"
                password = System.getenv("MYSQL_PASSWORD") ?: ""
            }.let { KronosJdbcWrapper(it) }
        }

        @BeforeAll @JvmStatic
        fun setup() {
            Kronos.dataSource = { wrapper }
            Kronos.fieldNamingStrategy = lineHumpNamingStrategy
            Kronos.tableNamingStrategy = lineHumpNamingStrategy
            wrapper.table.syncTable(User())  // ensure table exists
        }
    }

    @Test
    fun testInsertAndSelect() {
        val user = User(name = "test", age = 18)
        user.insert().execute()
        val result = User(name = "test").select().by { it.name }.queryOne()
        assertEquals("test", result.name)
    }
}
```

### Environment Variables for Integration Tests

| Variable | Default in CI | Database |
|----------|--------------|----------|
| `MYSQL_USERNAME` | `kronos` | MySQL 8.0 |
| `MYSQL_PASSWORD` | (empty) | MySQL 8.0 |
| `POSTGRES_USERNAME` | `postgres` | PostgreSQL 17 |
| `POSTGRES_PASSWORD` | (empty) | PostgreSQL 17 |
| SQL Server | SA / `YourStrong!Passw0rd` | SQL Server 2022 |
| `ORACLE_JDBC_URL` | `jdbc:oracle:thin:@localhost:1521/FREEPDB1` | Oracle Free |
| `ORACLE_USERNAME` | `kronos` | Oracle Free |
| `ORACLE_PASSWORD` | `KronosPassw0rd1` | Oracle Free |
| `DM_JDBC_URL` | `jdbc:dm://localhost:5237` | DM8 |
| `DM_USERNAME` | `SYSDBA` | DM8 |
| `DM_PASSWORD` | `SYSDBA` | DM8 |
| `H2_JDBC_URL` | `jdbc:h2:mem:kronos_testing;DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE` | H2 (in-process) |
| `H2_USERNAME` | `sa` | H2 (in-process) |
| `H2_PASSWORD` | (empty) | H2 (in-process) |
| `SQLITE_URL` | Temporary file URL | SQLite (in-process) |

### Running Tests Locally

```bash
# Start the Docker databases, run integration tests and stop the services
scripts/run ci integration

# A single JVM module
scripts/run build test -m kronos-core -p jvm
scripts/run build test -m kronos-compiler-plugin -p jvm
scripts/run build test -m kronos-codegen -p jvm

# Integration tests against databases already running
scripts/run build test -m kronos-testing -p jvm
```

These commands use JDK 21 on macOS, Linux and Windows; Windows runs them with `scripts\run.cmd`.
`scripts/common.main.kts` loads integration defaults and optional `kronos-testing/env.local.properties`.
Existing environment variables take precedence. `scripts/run ci env` writes the same database settings
to `GITHUB_ENV` for subsequent GitHub Actions steps.

---

## CI/CD Workflows

`.github/workflows/kronos-kmp-testing.yml` orchestrates the complete PR and merge-group DAG.
It keeps the protected names `kronos-core-unittests`, `kronos-compiler-plugin-unittests`,
`kronos-codegen-unittests`, `Run integration tests`, `Check Code Quality`, `kronos-core-coverage`,
`Build documentation site`, and `KMP gate`. Codacy remains a separately required GitHub App check.

The JVM candidate feeds all five OS/JDK compatibility entries, KEFS, server examples and the Android
APK producer. JDK 21 module receipts require the matrix's actual complete JUnit results, avoiding a
second identical unit-suite execution. Four instrumented JVM coverage suites and the actual KEFS editor
contracts produce execution data in parallel. The `kronos-core-coverage` job merges their verified artifacts,
generates the compiler report without rerunning tests, and compares the published baseline. Docs, detekt
and all database integration tests run in parallel.

Full project metadata is built before six Native producers: iOS, macOS/Intel Apple, tvOS, watchOS,
Linux and Windows. Each group publishes all three runtime modules and executes its shared Native
suites. Linux ARM executables run on an ARM host. Each SQLite consumer needs only its platform group;
iOS also builds and runs the application UI test. Android builds and lints both APKs once, then API 26
and 35 install and run those exact APKs. The final gate verifies every job result and eleven consumer
receipts against the assembled publication and APK manifests.

Reusable workflows are `kronos-kmp-publication.yml`, `kronos-native-sqlite-example.yml`,
`kronos-android-example.yml`, `kronos-examples.yml`, and `kronos-ide-support.yml`. `publish.yml`
uses the same complete DAG before publishing the exact assembled repository. `greetings.yml` and
`stale.yml` manage repository issues and pull requests.

SDK/dependency caches are configured in `.github/actions/setup-toolchain`; source-built candidate
repositories and reports travel through artifacts. The scripts launcher fingerprints shared main.kts
sources and JAVA_HOME to invalidate compiled scripts when imports or the script JDK change.

### Integration Test CI Setup
The `high-level-tests` job loads the integration defaults, starts `kronos-testing/docker-compose.integration.yml`, prepares the SQL Server database, and runs the `kronos-testing` JVM suite.

The regular integration gate starts `dm8` directly alongside the other integration databases using `toyangdon/dm:8-arm64`. That image is ARM64-only, so the job registers QEMU before Compose. It uses the image's public `SYSDBA` / `SYSDBA` defaults on a dedicated host port; no developer-local password, repository secret, job-specific DM8 variables, Compose profile, or Gradle exclusion is involved. Local installations override the tracked test defaults through ignored local environment settings.

H2 is supplied by `com.h2database:h2` and uses `IntegrationDatabaseEnvironments.h2` with an in-memory URL. `H2IntegrationTest` applies the shared integration suites, while `H2GeneratedKeyIntegrationTest` verifies identity inserts through JDBC generated keys. The H2 test environment is initialized inside the JVM test process; Compose services cover the external databases.

---

## Coverage

### Coverage Commands

```bash
scripts/run quality coverage --module kronos-compiler-plugin
scripts/run quality coverage --module kronos-core
scripts/run quality coverage
scripts/run quality coverage --baseline-dir .coverage-baseline
scripts/run quality compare --baseline-dir .coverage-baseline
```

The quality tool instruments JVM tests with Kover. Reports are written to `<module>/build/reports/kover/report.xml`, `html/index.html` and `summary.json`. Without `--module`, it covers core, syntax, compiler-plugin and codegen. When the selected modules include compiler-plugin, the same command runs the real KEFS editor contracts once using the already built production JARs, then merges CLI and IDE coverage before enforcing minimums. A selection without compiler-plugin runs only the selected JVM suites. Add `--baseline-dir` to compare the complete report in that invocation; comparison writes `coverage/comparison.json` and `coverage/comparison.md` and fails on any coverage regression.

CI keeps the expensive producers parallel: `quality coverage --defer-compiler-coverage --input-bundle build/coverage/cli` produces all four JVM reports without comparing the incomplete compiler report, while `ide test --coverage-dir build/coverage/ide` produces the actual editor execution data. A lightweight dependent job runs `quality merge-coverage --cli-inputs build/coverage/cli --ide-inputs build/coverage/ide`, then the existing baseline comparison. The merge verifies source/run provenance, identical compiler class bytes, agent and report hashes, and successful complete test receipts. Its independent compiler `ide/report.xml` and `ide/summary.json` prove covered lines in the FIR IDE host bridge before the combined report is accepted. Baseline thresholds and the no-regression policy apply to the merged result.

## Version Management

### Version Sources

`scripts/version.main.kts` synchronizes the publishing version in `templates/common.module-template.yaml`, the compiler bootstrap dependency in `libs.versions.toml`, and the packaged Gradle compiler version resource.

```bash
scripts/run version set 0.4.0
scripts/run version release-from-current
scripts/run version next-snapshot
```

### Release Version Upgrade Checklist

When preparing a stable release, do not stop after changing the publishing version. Update every user-facing version surface in the same change:

- Run `scripts/run version set <stable-version>` to synchronize Toolchain publishing, bootstrap and integration versions.
- Update `kronos-docs/src/app/docs/macros/common.njk`: `kronosVersion()` becomes `<stable-version>`, and snapshot macros become the next patch snapshot, for example `0.4.1-SNAPSHOT` / `0.4.1--SNAPSHOT`.
- Update `README.MD` and `README-zh_CN.MD`: Maven Central snapshot badge, Kotlin compatibility table, Gradle Kotlin/Groovy snippets, Maven dependency snippets, and plugin dependency snippets.
- Update docs release notes in both languages: add a new top section for `<stable-version>` and update `8.resources/release-notes/ng-doc.page.ts` `@status:primary`.
- Update non-macro docs surfaces: `kronos-docs/src/app/routes/home/home.component.ts`, blog snippets under `kronos-docs/src/assets/blogs/*`, `kronos-gradle-plugin/README.md`, and `kronos-maven-plugin/README.md`.
- Update skill docs that contain copyable user dependencies or version guidance: `.agents/skills/kronos-orm-guide/**`, `.agents/skills/kronos-dev-docs/SKILL.md`, and this dev guide.
- Keep historical release notes and compatibility rows for old versions unchanged unless their wording is wrong.
- Scan for stale release strings before finishing:

```bash
rg -n "0\\.3\\.1-SNAPSHOT|0\\.3\\.1--SNAPSHOT" templates plugins kronos-gradle-plugin kronos-maven-plugin kronos-docs README.MD README-zh_CN.MD .agents/skills -g '!kronos-docs/node_modules' -g '!kronos-docs/dist' -g '!**/build/**'
```

### Dependency Versions

All dependency versions live in the root `libs.versions.toml`. The Kotlin compiler version is `2.4.21`; the shared module template configures Kotlin language/API `2.4` and the JVM release target. Keep the catalog's `kronos-compiler-plugin` dependency aligned with the shared publishing version.

---

## Publishing

### Stage And Verify Publications

On macOS with Xcode, build the complete multiplatform repository into a new staging directory:

```bash
scripts/run release stage --repository build/release/repository
scripts/run release verify --repository build/release/repository
```

For a JVM publication used by Linux/Android integration consumers:

```bash
scripts/run release jvm-stage --repository build/release/jvm-repository
```

A Gradle reference application can consume the staged artifacts through its repository property:

```bash
./gradlew -PkronosRepository=/absolute/path/to/build/release/repository :shared:macosX64Test
```

This is the reference application's Gradle command; the Kronos repository artifacts are built by Kotlin Toolchain. Source SNAPSHOT artifacts use the same staging entry points.

### Publish Snapshots

Use the Maven CLI for timestamped snapshot metadata and attachments:

```bash
scripts/run release publish-snapshot --repository build/release/repository
```

The command uses `MAVEN_CENTRAL_USERNAME` and `MAVEN_CENTRAL_PASSWORD` and publishes the staged SNAPSHOT artifacts to Central's snapshot repository. Native KLIBs, Gradle module metadata, tooling metadata, sources and documentation are deployed with each component.

### Sign And Publish A Release

```bash
scripts/run version release-from-current
scripts/run release stage --repository build/release/repository
scripts/run release verify --repository build/release/repository
scripts/run release bundle --repository build/release/repository --output build/release/central-bundle.zip --sign
scripts/run release publish --output build/release/central-bundle.zip
```

`bundle --sign` signs the staged artifacts and prepares the Central bundle; `publish` submits that existing formal-release bundle. Publication credentials and signing configuration are supplied to the release process. The version must not end in `-SNAPSHOT` for this formal publication command.

KEFS compatibility keeps the ordinary compiler coordinate and stages a second version,
`<IDE Kotlin compiler version>-<Kronos version>`, with the same CLI JAR bytes. Its POM retains
publication metadata and dependency versions from the ordinary artifact. The editor gate verifies
all required types, completion, syntax and diagnostic contracts plus the actual loaded class bytes.

## Documentation

### Unified Deployment (`kronos-docs/deploy-docs.sh`)
Use JDK 21 for the Kotlin quality/build scripts invoked by deployment. A single script under `kronos-docs/` builds and merges both Dokka API docs and the kronos-docs website into `kronos-docs/dist/site/`, ready for Cloudflare Pages deployment.

```bash
cd kronos-docs
./deploy-docs.sh              # full build (Dokka + Angular)
```

Output structure:
```
kronos-docs/dist/site/
├── index.html        (kronos-docs Angular site)
├── api/
│   ├── kronos-core/
│   ├── kronos-compiler-plugin/
│   └── kronos-jdbc-wrapper/
└── _redirects         (Cloudflare Pages SPA routing)
```

Deploy: `npx wrangler pages deploy kronos-docs/dist/site --project-name=kotlinorm`

### Dokka API Docs

```bash
scripts/run quality dokka
scripts/run quality dokka --module kronos-core
```

The quality tool generates API documentation under `docs/<module>/`. Each module's README is attached to its common or main source set; verification requires the introduction, section headings and code examples in the rendered module page and checks local links throughout the API site. The required documentation CI check and deployment script merge these pages with the documentation website under `/api/`.

### User-Facing Documentation (kronos-docs)
- Angular 21 / ng-doc application in `kronos-docs/`
- Build: `pnpm install && pnpm build`
- API doc links point to `/api/<module>` (served from the same site)

---

## Build System

### Project And Module Configuration

`project.yaml` lists the runtime, compiler, Gradle/Maven integrations, integration tests and build tools. Each `module.yaml` declares its product, targets, source layout and dependencies. `templates/common.module-template.yaml` supplies shared Kotlin/JVM/publishing settings; `kronos-runtime.module-template.yaml` enables the source-built Kronos compiler.

Use `scripts/run build` for repository commands so the compiler bootstrap is refreshed first. The wrapper's local Maven repository is `build/repository/bootstrap`. Toolchain extensions under `plugins/kronos-native` generate metadata and provider resources; Android, Spring Boot, Ktor and Vert.x examples verify Gradle consumption of staged publications; Solon and Spring Boot Maven examples verify Maven integration.

### Compiler And JVM Settings

The shared module template selects Kotlin 2.4.21, language/API 2.4, collection literals, context-sensitive resolution and expect/actual classes. Published JVM bytecode targets Java 8; repository builds use JDK 21, and JVM tests target Java 17. The IDEA integration module uses the SDK's runtime requirements and its own isolated build.
