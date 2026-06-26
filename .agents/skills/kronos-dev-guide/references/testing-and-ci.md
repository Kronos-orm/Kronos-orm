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
- **Compiler plugin tests**: `kronos-compiler-plugin/src/test/kotlin/`. Uses kotlin-compile-testing.

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

### Writing Compiler Plugin Tests

Uses `kotlin-compile-testing` (kct/kctfork) framework.

**IrTestFramework** (`utils/IrTestFramework.kt`):
```kotlin
object IrTestFramework {
    fun compile(source: String): CompilationResult
    // Compiles Kotlin source with the Kronos plugin active
    // Returns compilation result for IR-level assertions
}
```

**KotlinSourceDynamicCompiler** (`utils/KotlinSourceDynamicCompiler.kt`):
```kotlin
object KotlinSourceDynamicCompiler {
    fun compile(source: String): ClassLoader
    // Compiles and returns a ClassLoader for end-to-end testing
    // Load classes, invoke methods, verify runtime behavior
}
```

Example pattern:
```kotlin
class KTableParserForConditionTransformerTest {
    @Test
    fun testEqualityCondition() {
        val result = IrTestFramework.compile("""
            data class User(val name: String? = null) : KPojo
            fun test() {
                User().select().where { it.name == "test" }
            }
        """)
        // Assert compilation succeeds, inspect IR output
    }
}
```

Test files in compiler plugin:
- `core/`: ErrorReporterTest, FieldAnalysisTest, KTableTransformerTest, SymbolsTest
- `plugin/`: KronosPluginTest
- `transformers/`: Tests for each transformer (Condition, Reference, Select, Set, Sort, TypeParameterFixer)
- `utils/`: AnnotationUtilsTest, TypeUtilsTest

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
            }.let { KronosBasicWrapper(it) }
        }

        @BeforeAll @JvmStatic
        fun setup() {
            Kronos.init {
                dataSource = { wrapper }
                fieldNamingStrategy = lineHumpNamingStrategy
                tableNamingStrategy = lineHumpNamingStrategy
            }
            wrapper.table.syncTable(User())  // ensure table exists
        }
    }

    @Test
    fun testInsertAndSelect() {
        val user = User(name = "test", age = 18)
        user.insert().execute()
        val result = User(name = "test").select().queryOne()
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

---

## CI/CD Workflows

All workflows in `.github/workflows/`:

| Workflow | Trigger | What It Does |
|----------|---------|-------------|
| `kronos-core-testing.yml` | push/PR to `main` | `./gradlew :kronos-core:test` (JDK 21) |
| `kronos-compiler-plugin-testing.yml` | push/PR to `main` | `./gradlew :kronos-compiler-plugin:test` (JDK 21) |
| `kronos-codegen-testing.yml` | push/PR to `main` | `./gradlew :kronos-codegen:test` (JDK 21) |
| `kronos-testing.yml` | push/PR to `main` | Integration tests with real DBs (MySQL 8.0, PostgreSQL 17, SQL Server 2022 via `ankane/setup-*` actions) |
| `detekt.yml` | push to main/master/releases/*, all PRs | Static analysis via `alaegin/Detekt-Action@v1.23.8` |
| `coverage.yml` | push/merge_group to `main` | Kover coverage reports + badge generation for core, compiler-plugin, codegen |
| `publish.yml` | push to `release/0.1.0` OR PR merged to `main` | Snapshot or release publishing to Maven Central |
| `sync-skills.yml` | push to `release/0.1.0` (paths: `.claude/skills/kronos-orm-guide/**`) | Syncs skill files to `release/llm` branch |
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
1. `build-logic/src/main/kotlin/publishing.gradle.kts` → `project.version = "0.1.0-SNAPSHOT"`
2. `kronos-gradle-plugin/src/main/kotlin/.../KronosGradlePlugin.kt` → `version = "0.1.0-SNAPSHOT"`

### Bump Script
`.github/scripts/bump-version.sh`:
```bash
bump-version.sh set 1.2.3            # set explicit version
bump-version.sh next-snapshot         # 0.1.0 → 0.1.1-SNAPSHOT
bump-version.sh release-from-current  # 0.1.1-SNAPSHOT → 0.1.1
bump-version.sh next-release          # 0.1.0 → 0.1.1
```

Parses `MAJOR.MINOR.PATCH`, increments PATCH, uses `sed` to update both files. Handles macOS vs Linux `sed` differences.

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
- Trigger: PR merged to `main` (version must NOT end with `-SNAPSHOT`)
- Flow:
  1. `bump-version.sh release-from-current` strips `-SNAPSHOT`
  2. Commit + tag `v$VERSION`
  3. `./gradlew publishAllToMavenCentral` with GPG signing
  4. `bump-version.sh next-snapshot` bumps to next SNAPSHOT
  5. Commit + push
- Secrets: `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `SIGNING_KEY` (ASCII-armored GPG), `SIGNING_PASSWORD`

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

### Unified Deployment (`deploy-docs.sh`)
A single script at the repo root builds and merges both Dokka API docs and the kronos-docs website into `dist/site/`, ready for Cloudflare Pages deployment.

```bash
./deploy-docs.sh              # full build (Dokka + Angular)
./deploy-docs.sh --skip-dokka # skip Dokka, only rebuild Angular (faster iteration)
```

Output structure:
```
dist/site/
├── index.html        (kronos-docs Angular site)
├── api/
│   ├── kronos-core/
│   ├── kronos-compiler-plugin/
│   └── kronos-jdbc-wrapper/
└── _redirects         (Cloudflare Pages SPA routing)
```

Deploy: `npx wrangler pages deploy dist/site --project-name=kotlinorm`

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
`settings.gradle.kts` includes 8 modules:
```kotlin
include("kronos-core", "kronos-compiler-plugin", "kronos-compiler-plugin-legacy",
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
