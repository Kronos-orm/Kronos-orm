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

kronos-core tests use the compiler plugin at test time via:
```kotlin
// kronos-core/build.gradle.kts
dependencies {
    kotlinCompilerPluginClasspathTest(project(":kronos-compiler-plugin"))
    testImplementation(libs.kotlin.test)
    testImplementation(libs.mockk)
}
```

This means test code can use KPojo data classes with full compiler plugin augmentation.

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

Kronos is migrating compiler-plugin behavior to Kotlin's official compiler test infrastructure. These tests compile `kronos-compiler-plugin/testData` sources through the real FIR/IR/codegen pipeline with `KronosCompilerPluginRegistrar` registered.

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
# All official compiler box tests
./gradlew :kronos-compiler-plugin:test --tests "com.kotlinorm.compiler.*BoxTest" --no-daemon --console=plain

# One official suite
./gradlew :kronos-compiler-plugin:test --tests com.kotlinorm.compiler.ConditionBoxTest --no-daemon --console=plain

# One test method
./gradlew :kronos-compiler-plugin:test --tests com.kotlinorm.compiler.TypeParameterFixerBoxTest.queryReturnTypes --no-daemon --console=plain
```

### Writing Integration Tests (kronos-testing)

Tests connect to real databases. CI spins up MySQL, PostgreSQL, SQL Server automatically.

```kotlin
// kronos-testing/build.gradle.kts
dependencies {
    implementation(project(":kronos-core"))
    implementation(project(":kronos-jdbc-wrapper"))
    kotlinCompilerPluginClasspath(project(":kronos-compiler-plugin"))
    // DB drivers
    implementation(libs.driver.jdbc.mysql)
    implementation(libs.driver.jdbc.postgres)
    implementation(libs.driver.jdbc.sqlite)
    implementation(libs.driver.jdbc.mssql)
    implementation(libs.driver.jdbc.oracle)
    // Connection pool
    implementation(libs.dbcp2)
}
```

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

### Running Tests Locally

```bash
# All tests (checks DB connectivity, runs all modules)
./test.sh

# Unit tests only
./gradlew test

# Single module
./gradlew :kronos-core:test
./gradlew :kronos-compiler-plugin:test
./gradlew :kronos-codegen:test

# Integration tests (requires running DBs + env vars)
source envsetup.sh  # sets DB env vars
./gradlew :kronos-testing:test --info --stacktrace
```

`test.sh` runs: DB connectivity check → kronos-testing → kronos-core → kronos-codegen → kronos-compiler-plugin, with output logged to `*-output.log` files.

Windows equivalents:

```bat
rem All tests (checks DB connectivity, runs all modules)
test.bat

rem Environment defaults for local DB credentials
call envsetup.bat
```

`test.bat` mirrors `test.sh` and writes the same `*-output.log` files at the repo root.

---

## CI/CD Workflows

All workflows in `.github/workflows/`:

| Workflow | Trigger | What It Does |
|----------|---------|-------------|
| `kronos-core-testing.yml` | push/PR to `main` | `./gradlew :kronos-core:test` (JDK 21) |
| `kronos-compiler-plugin-testing.yml` | push/PR to `main` | `./gradlew :kronos-compiler-plugin:test` (JDK 21) |
| `kronos-codegen-testing.yml` | push/PR to `main` | `./gradlew :kronos-codegen:test` (JDK 21) |
| `kronos-testing.yml` | push/PR to `main` | Integration tests with real DBs (MySQL 8.0, PostgreSQL 17, SQL Server 2022 via `ankane/setup-*` actions) |
| `kronos-examples.yml` | push/PR to `main` | Publishes current Kronos artifacts to Maven Local, checks out external examples, rewires their coordinates to the current version, runs backend smoke tests, and builds/lints/tests the Android example on an API 35 emulator |
| `kronos-docs-testing.yml` | docs/workflow push or PR to `main` | Installs locked pnpm dependencies and builds the Angular/ng-doc documentation site |
| `detekt.yml` | push to main/master/releases/*, all PRs | Static analysis via `alaegin/Detekt-Action@v1.23.8` |
| `coverage.yml` | push/merge_group to `main` | Kover coverage reports + badge generation for core, compiler-plugin, codegen |
| `publish.yml` | push to `main` | Snapshot publishing to Central Snapshots, or formal release publishing to Maven Central, JetBrains Marketplace, and GitHub Releases |
| `greetings.yml` | new issues/PRs | Bilingual welcome messages |
| `stale.yml` | daily 15:40 UTC | Marks stale issues (60d) and PRs, closes after grace period |

### Integration Test CI Setup
`kronos-testing.yml` spins up real databases:
```yaml
- uses: ankane/setup-mysql@v1
  with: { mysql-version: "8.0", database: kronos_testing }
- uses: ankane/setup-postgres@v1
  with: { postgres-version: 17, database: kronos_testing }
- uses: ankane/setup-sqlserver@v1
  with: { sqlserver-version: 2022, accept-eula: true, sa-password: "YourStrong!Passw0rd" }
```

---

## Coverage

### Kover Configuration
Coverage is measured by Kover. Configured per-module in `build.gradle.kts`:

**kronos-compiler-plugin** (enforced):
```kotlin
kover {
    reports {
        verify {
            rule { minBound(80) }  // 80% minimum line coverage
        }
    }
}
```

**kronos-core** and **kronos-codegen**: Kover enabled for reporting but no minimum enforcement.

### Coverage CI (`coverage.yml`)
1. Runs `./gradlew :module:koverLog` and `:module:koverHtmlReport` for core, compiler-plugin, codegen
2. Extracts line coverage percentage from `build/kover/coverage.txt`
3. Generates SVG badges via `emibcn/badge-action@v2.0.3`
4. Deploys HTML reports + badges to `coverage` branch via `JamesIves/github-pages-deploy-action@v4`
5. Reports accessible at `https://coverage.kotlinorm.com/<module-name>`

### Running Coverage Locally
```bash
./gradlew :kronos-compiler-plugin:koverVerify   # check 80% minimum
./gradlew :kronos-core:koverHtmlReport           # generate HTML report
# Report at: build/reports/kover/html/index.html
```

---

## Version Management

### Where Version Lives
Two files (must stay in sync):
1. `build-logic/src/main/kotlin/publishing.gradle.kts` → `project.version = "0.3.0"`
2. `kronos-gradle-plugin/src/main/kotlin/.../KronosGradlePlugin.kt` → `version = "0.3.0"`

### Bump Script
`.github/scripts/bump-version.sh`:
```bash
bump-version.sh set 1.2.3            # set explicit version
bump-version.sh next-snapshot         # 0.3.0 → 0.3.1-SNAPSHOT
bump-version.sh release-from-current  # 0.3.0-SNAPSHOT → 0.3.0
bump-version.sh next-release          # 0.3.0 → 0.3.1
```

Parses `MAJOR.MINOR.PATCH`, increments PATCH, uses `sed` to update both files. Handles macOS vs Linux `sed` differences.

### Release Version Upgrade Checklist

When preparing a stable release, do not stop after changing the two code version fields. Update every user-facing version surface in the same change:

- Run `bash .github/scripts/bump-version.sh set <stable-version>` to update `publishing.gradle.kts` and `KronosGradlePlugin.kt`.
- Update `kronos-docs/src/app/docs/macros/common.njk`: `kronosVersion()` becomes `<stable-version>`, and snapshot macros become the next patch snapshot, for example `0.3.1-SNAPSHOT` / `0.3.1--SNAPSHOT`.
- Update `README.MD` and `README-zh_CN.MD`: Maven Central snapshot badge, Kotlin compatibility table, Gradle Kotlin/Groovy snippets, Maven dependency snippets, and plugin dependency snippets.
- Update docs release notes in both languages: add a new top section for `<stable-version>` and update `8.resources/release-notes/ng-doc.page.ts` `@status:primary`.
- Update non-macro docs surfaces: `kronos-docs/src/app/routes/home/home.component.ts`, blog snippets under `kronos-docs/src/assets/blogs/*`, `kronos-gradle-plugin/README.md`, and `kronos-maven-plugin/README.md`.
- Update skill docs that contain copyable user dependencies or version guidance: `.agents/skills/kronos-orm-guide/**`, `.agents/skills/kronos-dev-docs/SKILL.md`, and this dev guide.
- Keep historical release notes and compatibility rows for old versions unchanged unless their wording is wrong.
- Scan for stale release strings before finishing:

```bash
rg -n "0\\.2\\.5-SNAPSHOT|0\\.2\\.5--SNAPSHOT" build-logic kronos-gradle-plugin kronos-maven-plugin kronos-docs README.MD README-zh_CN.MD .agents/skills -g '!kronos-docs/node_modules' -g '!kronos-docs/dist' -g '!**/build/**'
```

### Dependency Versions
All dependency versions in `gradle/libs.versions.toml`:
```toml
[versions]
kotlin = "2.3.0"
kover = "0.9.1"
dokka = "2.0.0"
# ... drivers, test libs, etc.

[libraries]
kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin" }
driver-jdbc-mysql = { module = "com.mysql:mysql-connector-j", version = "9.2.0" }
# ...

[plugins]
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kover = { id = "org.jetbrains.kotlinx.kover", version.ref = "kover" }
# ...
```

---

## Publishing

### Snapshot Publishing
- Trigger: push to `release/0.1.0` branch
- Task: `./gradlew publishAllToCentralSnapshots`
- Target: Maven Central Snapshots (`https://central.sonatype.com/repository/maven-snapshots/`)
- No GPG signing required
- Secrets: `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`

### Release Publishing
- Trigger: push to `main` (version must NOT end with `-SNAPSHOT`)
- Flow:
  1. `bump-version.sh release-from-current` strips `-SNAPSHOT`
  2. Commits and pushes the release version
  3. Builds JVM artifacts
  4. Runs `:kronos-idea-plugin:publishPlugin`, which builds, signs, and uploads the current release version to the public JetBrains Marketplace `default` channel
  5. Runs `publishAllToMavenCentral` with GPG signing
  6. Creates or updates the GitHub Release with JVM artifacts, the signed IDEA plugin zip, and checksums
  7. Opens a PR that bumps the project to the next SNAPSHOT version
- Maven Central secrets: `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `SIGNING_KEY` (ASCII-armored GPG), `SIGNING_PASSWORD`
- JetBrains Marketplace secrets: `JETBRAINS_MARKETPLACE_TOKEN`, `IDEA_CERTIFICATE_CHAIN`, `IDEA_PRIVATE_KEY`, `IDEA_PRIVATE_KEY_PASSWORD`

The IDEA plugin version comes from `rootProject.version`; do not hard-code a
release number in the plugin build. Snapshot pushes never upload to Marketplace.
The formal release job must fail if Marketplace credentials are absent, plugin
signing fails, upload fails, or the expected signed zip is missing. Only the
signed zip for the current release version may be attached to the GitHub Release.

Create `JETBRAINS_MARKETPLACE_TOKEN` in the JetBrains Marketplace profile. Store
the complete PEM certificate chain, matching complete PEM private key, and key
password in the other three repository secrets. Never commit or print these
values. For local verification, prefer the file-backed
`CERTIFICATE_CHAIN_FILE` and `PRIVATE_KEY_FILE` inputs and run `signPlugin`,
`verifyPluginStructure`, `verifyPluginSignature`, and Plugin Verifier without
invoking `publishPlugin`.

### Publishing Convention Plugin
`build-logic/src/main/kotlin/publishing.gradle.kts`:
- Uses `com.vanniktech.maven.publish` with `KotlinJvm(JavadocJar.Javadoc(), sourcesJar = true)`
- POM: Apache 2.0 license, 3 developers (ousc, FOYU, yf), SCM → GitHub
- Three targets: Maven Central (release), Central Portal Snapshots, Aliyun (private mirror)
- Root project registers aggregate tasks: `publishAllToMavenLocal`, `publishAllToMavenCentral`, `publishAllToAliyun`, `publishAllToCentralSnapshots`
- Signing skipped for SNAPSHOT builds

### Publishing Locally
```bash
./gradlew publishAllToMavenLocal  # publishes to ~/.m2/repository
```

---

## Documentation

### Unified Deployment (`kronos-docs/deploy-docs.sh`)
A single script under `kronos-docs/` builds and merges both Dokka API docs and the kronos-docs website into `kronos-docs/dist/site/`, ready for Cloudflare Pages deployment.

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
- Convention plugin: `build-logic/src/main/kotlin/dokka-convention.gradle.kts`
- Root task: `dokkaGenerateAll` runs `dokkaGenerate` on all subprojects
- Copies each subproject's `build/dokka/html/` into `docs/<module-name>/`
- Config: `reportUndocumented = true`, `jdkVersion = 17`, includes `README.md`

```bash
./gradlew :dokkaGenerateAll  # generates all API docs
# Output: docs/<module-name>/index.html
```

### User-Facing Documentation (kronos-docs)
- Angular 21 / ng-doc application in `kronos-docs/`
- Build: `pnpm install && pnpm build`
- API doc links point to `/api/<module>` (served from the same site)

---

## Build System

### Root Settings
`settings.gradle.kts` includes 7 modules:
```kotlin
include("kronos-core", "kronos-compiler-plugin",
        "kronos-maven-plugin", "kronos-logging", "kronos-jdbc-wrapper",
        "kronos-codegen", "kronos-testing")
includeBuild("kronos-gradle-plugin")  // separate included build
```

### Build Logic
`build-logic/` is a composite build with two convention plugins:
- `kronos.publishing` — Maven Central publishing config
- `kronos.dokka-convention` — Dokka documentation config

Applied via:
```kotlin
plugins {
    alias(libs.plugins.kronos.publishing)
    alias(libs.plugins.kronos.dokka)
}
```

### Compiler Flags
Common across modules:
```kotlin
kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
        freeCompilerArgs.add("-Xcontext-sensitive-resolution")
        freeCompilerArgs.add("-Xskip-prerelease-check")
    }
}
```

### JDK Configuration
- Runtime target: JVM 1.8 (for broad compatibility)
- Build toolchain: JDK 17/21
- CI uses JDK 21
