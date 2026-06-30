---
name: kronos-dev-guide
description: >
  Kronos-ORM internal developer guide for contributors and maintainers.
  Use this skill whenever working on the Kronos-ORM repository internals:
  adding features, fixing bugs, adding database dialect support, writing tests,
  understanding the compiler plugin or AST architecture, modifying DSL operations,
  working with transactions, or navigating the CI/CD pipeline.
  Also trigger when someone asks about module structure, how the compiler plugin
  transforms KPojo classes, how SQL is generated from the AST, how to add
  a new database backend, how to write tests, how coverage works, how to publish
  releases, or how any specific module (codegen, logging, jdbc-wrapper, gradle/maven
  plugin) works internally.
---

# Kronos-ORM Developer Guide

Kotlin compiler-plugin-powered ORM. Zero reflection, AST-based SQL generation, multi-database.

## Evolution Memory Protocol

For every error, failure, or unexpected behavior, consult `Evolution.index.md` first before attempting a fix. The index is the highest-priority troubleshooting memory and keeps the context cost low. Do not read all of `Evolution.md` by default, and do not open it at all unless the index or a targeted search points to a relevant entry.

Priority order for error handling:

1. Read `Evolution.index.md` and look for a matching symptom, module, command, or error message.
2. If the index matches, read only the corresponding `Evolution.md` entry with targeted search/context, for example `Select-String -Path .agents/skills/kronos-dev-guide/Evolution.md -Pattern "entry title" -Context 0,20`.
3. Reuse any documented solution or prevention rule that applies.
4. If no matching index record exists, continue to the relevant reference file below or search code/tests normally without opening the full evolution log.
5. After a successful build or verified fix, append only reusable, confirmed pitfalls to `Evolution.md` using the required format, then add one concise row to `Evolution.index.md`.

Use this protocol to preserve discovered project pitfalls and avoid repeating the same mistakes.

## Architecture at a Glance

```
User DSL code (select/insert/update/delete/upsert/join/union)
  ↓ compile time
Compiler Plugin (kronos-compiler-plugin)
  → KPojo class augmentation (toDataMap, fromMapData, kronosColumns, get/set, strategies)
  → DSL lambda transformation (KTableForCondition/Select/Set/Sort/Reference → Criteria/Field IR)
  ↓ runtime
ORM Clause classes (SelectClause, InsertClause, etc.)
  → Build AST nodes (SelectStatement, InsertStatement, etc.)
  → SqlManager routes to dialect-specific SqlRenderer
  → SqlRenderer renders AST → SQL string + named parameters
  → NamedParameterUtils converts :name → ? positional params
  → KronosDataSourceWrapper executes via JDBC
```

## Module Map

| Module | Role | Key Entry Points |
|--------|------|-----------------|
| `kronos-core` | Contracts, AST, SQL rendering, ORM ops, strategies, DSL beans | `Kronos.kt`, `ast/`, `database/`, `orm/`, `beans/dsl/` |
| `kronos-compiler-plugin` | K2 IR transformations | `KronosParserTransformer`, `KronosIrClassTransformer` |
| `kronos-gradle-plugin` | Gradle build integration | `KronosGradlePlugin.kt` |
| `kronos-maven-plugin` | Maven build integration | `KronosMavenPlugin.kt` |
| `kronos-logging` | Pluggable logging (SLF4J/JUL/Commons/Android) | `KronosLoggerApp.kt` |
| `kronos-jdbc-wrapper` | Default JDBC DataSource wrapper | `KronosBasicWrapper.kt` |
| `kronos-codegen` | DB schema → KPojo Kotlin files | `ConfigReader.kt`, `TemplateConfig.kt` |
| `kronos-testing` | Integration tests (real DBs) | MySQL/Postgres/SQLite/Oracle/MSSQL tests |
| `kronos-docs` | Documentation website (Angular/ng-doc) | `pnpm install && ng build` |
| `build-logic` | Convention plugins (publishing, dokka) | `publishing.gradle.kts`, `dokka-convention.gradle.kts` |

## Testing Frameworks At A Glance

Kronos currently uses two active kinds of tests. Pick the narrowest one that proves the behavior:

| Test kind | Location | Use for | Do not use for |
|-----------|----------|---------|----------------|
| Ordinary unit tests | `*/src/test/kotlin` | Pure functions, AST rendering, utility classes, core runtime behavior that does not need a fresh compiler pipeline | Compiler-plugin generated declarations or IR rewrites |
| Official compiler tests | `kronos-compiler-plugin/testData` plus runners in `src/test/kotlin/com/kotlinorm/compiler` | KPojo generated bodies, DSL lambda transformations, FIR diagnostics, IR verifier regressions, compiler-plugin behavior through Kotlin's real FIR/IR pipeline | Broad smoke tests, pure utilities, external database integration |

Historical note: kctfork / kotlin-compile-testing tests and helpers such as `KotlinSourceDynamicCompiler` and `IrTestFramework` were removed from `kronos-compiler-plugin`. Do not add new tests using kctfork; migrate compiler-plugin behavior into official `testData` tests instead. Keep ordinary utility tests when they do not require compiler execution.

When adding or migrating compiler-plugin tests:

1. Read `.agents/skills/kronos-dev-kcp/SKILL.md`, especially the Kronos official compiler testData style section.
2. If the behavior depends on generated KPojo members or DSL IR rewrites, add a `testData/box/.../*.kt` file and a thin official runner method.
3. If invalid source should fail, use `testData/diagnostics`, not a box test.
4. If a removed kctfork behavior is rediscovered as uncovered, add a precise official `testData` file that asserts the same compiler-plugin contract and important values.
5. Keep pure utility coverage as ordinary unit tests; do not force utility behavior through compiler testData.

## Coverage Workflow

Use Kover from the module being evaluated. For compiler-plugin coverage:

```powershell
.\gradlew.bat :kronos-compiler-plugin:koverLog --no-daemon --console=plain
```

Generate inspectable reports when deciding what to test next:

```powershell
.\gradlew.bat :kronos-compiler-plugin:cleanTest :kronos-compiler-plugin:koverXmlReport :kronos-compiler-plugin:koverHtmlReport --no-daemon --console=plain
```

Open the HTML report at:

```text
kronos-compiler-plugin/build/reports/kover/html/index.html
```

Use the XML report for quick class-level analysis:

```text
kronos-compiler-plugin/build/reports/kover/report.xml
```

`koverLog` prints line coverage only. To get branch coverage, generate `koverXmlReport` and parse the report-level `BRANCH` counter from `report.xml`.

PowerShell:

```powershell
$text = Get-Content 'kronos-compiler-plugin/build/reports/kover/report.xml' -Raw
$counters = [regex]::Matches($text, '<counter type="([^"]+)" missed="(\d+)" covered="(\d+)"/>')
foreach ($type in @('LINE','BRANCH','INSTRUCTION','METHOD')) {
    $m = $counters | Where-Object { $_.Groups[1].Value -eq $type } | Select-Object -Last 1
    if ($m) {
        $missed = [int]$m.Groups[2].Value
        $covered = [int]$m.Groups[3].Value
        $total = $missed + $covered
        $pct = [math]::Round(100 * $covered / $total, 4)
        "$type missed=$missed covered=$covered total=$total pct=$pct%"
    }
}
```

bash/zsh:

```bash
python - <<'PY'
import re
from pathlib import Path

text = Path("kronos-compiler-plugin/build/reports/kover/report.xml").read_text()
counters = re.findall(r'<counter type="([^"]+)" missed="(\d+)" covered="(\d+)"/>', text)
for kind in ("LINE", "BRANCH", "INSTRUCTION", "METHOD"):
    matches = [(int(missed), int(covered)) for typ, missed, covered in counters if typ == kind]
    if matches:
        missed, covered = matches[-1]
        total = missed + covered
        pct = round(100 * covered / total, 4)
        print(f"{kind} missed={missed} covered={covered} total={total} pct={pct}%")
PY
```

fish:

```fish
python -c 'import re; from pathlib import Path
text = Path("kronos-compiler-plugin/build/reports/kover/report.xml").read_text()
counters = re.findall(r"<counter type=\"([^\"]+)\" missed=\"(\d+)\" covered=\"(\d+)\"/>", text)
for kind in ("LINE", "BRANCH", "INSTRUCTION", "METHOD"):
    matches = [(int(missed), int(covered)) for typ, missed, covered in counters if typ == kind]
    if matches:
        missed, covered = matches[-1]
        total = missed + covered
        pct = round(100 * covered / total, 4)
        print(f"{kind} missed={missed} covered={covered} total={total} pct={pct}%")'
```

Example output:

```text
LINE missed=492 covered=1811 total=2303 pct=78.6366%
BRANCH missed=764 covered=822 total=1586 pct=51.8285%
```

Coverage interpretation rules:

- Treat coverage as a guide to missing test surfaces, not proof that behavior is equivalent.
- Prioritize high-missed-line compiler behavior such as `ConditionAnalysis` and `FieldAnalysis` before low-risk option parsing or dump paths.
- Track branch coverage separately from line coverage. Compiler DSL analyzers can keep line coverage acceptable while branch coverage drops sharply.
- Prefer official `testData` for generated code and DSL transformer coverage.
- Prefer ordinary unit tests for pure utilities such as error messages, caches, type mapping, and string/path helpers.
- Re-run `koverLog` after restoring or deleting tests; stale `.ic` execution data can make comparisons misleading, so use `cleanTest` before serious comparisons.
- If coverage drops after removing a test framework, inspect the HTML/XML report before re-adding tests. Restore pure UTs directly; migrate compiler behavior to official testData.

## Reference Files

Read these for deep implementation details:

| File | When to Read |
|------|-------------|
| `Evolution.index.md` | Required first read before fixing any error; maps symptoms and keywords to targeted `Evolution.md` entries |
| `Evolution.md` | Historical problem database; read only the indexed matching entry or use targeted search/context |
| `references/compiler-plugin.md` | Modifying compiler plugin, adding/changing IR transforms, debugging IR output. Before compiler plugin architecture, FIR/frontend, IR/backend, diagnostics, or compiler-test work, use `.agents/skills/kronos-dev-kcp` first. |
| `references/ast-and-rendering.md` | Working with AST nodes, SQL rendering, adding/modifying database dialects, functions system |
| `references/orm-and-dsl.md` | ORM clause classes, DSL beans, plugin/hook system, task execution, transactions, value transformers |
| `references/api-design.md` | Designing or reviewing Kronos user-facing DSL syntax, API shape, subquery syntax, projection syntax, or documentation specs before implementation |
| `references/modules.md` | Working on codegen, logging, jdbc-wrapper, gradle/maven plugins |
| `references/testing-and-ci.md` | Writing tests, coverage, CI workflows, version management, publishing, documentation |
| `references/cookbook.md` | Step-by-step: add new DB dialect, add new DSL operation, add subquery syntax, write tests |
