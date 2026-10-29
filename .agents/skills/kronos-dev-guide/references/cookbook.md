# Cookbook: Step-by-Step Development Tasks

## Table of Contents
1. [Add a New Database Dialect](#add-a-new-database-dialect)
2. [Add a New DSL Operation](#add-a-new-dsl-operation)
3. [Add Subquery Syntax](#add-subquery-syntax)
4. [Add a New SQL Function](#add-a-new-sql-function)
5. [Add a New Annotation](#add-a-new-annotation)
6. [Write a Compiler Plugin Test](#write-a-compiler-plugin-test)
7. [Write an Integration Test](#write-an-integration-test)
8. [Write a Unit Test for kronos-core](#write-a-unit-test-for-kronos-core)
9. [Add a New ORM Strategy](#add-a-new-orm-strategy)
10. [Debug Compiler Plugin IR Output](#debug-compiler-plugin-ir-output)
11. [Release a New Version](#release-a-new-version)

---

## Add a New Database Dialect

Database support has two parts: portable SQL rendering in `kronos-syntax/src` and metadata/table operations in `kronos-core/src`.

### Step 1: Define database identity

Add the database identifier and name/URL recognition to `kronos-core/src/com/kotlinorm/enums/DBType.kt`. Check how the JDBC connection wrapper resolves the database product name. Keep these declarations independent of JDBC classes.

### Step 2: Implement SQL rendering

Use `kronos-syntax/src/com/kotlinorm/syntax/render/SqlDialect.kt` for quoting, pagination and parameter capabilities. Check the dialect family and renderers for SELECT, mutations, conflict handling, locking and functions. Add renderer behavior and syntax tests in `kronos-syntax` when the existing dialect settings cannot express the database's SQL.

### Step 3: Implement metadata and table statements

Create the database package under `kronos-core/src/com/kotlinorm/database/` and subclass `DatabaseStatements`. Follow `database/h2/H2Statements.kt` or another matching database implementation. Implement:

- `databaseName`, `tableExists`, `tableColumns` and `tableIndexes` for metadata queries;
- `mapColumns` and `mapIndexes` for normalized metadata rows;
- `sameColumnDefinition` for schema comparisons;
- `createTable`, `dropTable`, `truncateTable` and `syncTable` for AST statements;
- `tableComment` and `lastInsertIdFallback` when the database needs them.

`DatabaseCreateTable` and `DatabaseSyncTable` carry the inputs. Return SQL AST nodes so the selected renderer handles quoting and parameters consistently.

### Step 4: Register both parts

`kronos-core/src/com/kotlinorm/database/SqlManager.kt` owns the mappings. Use `SqlManager.registerDatabase(dbType, dialect, definitions)` for a complete registration, or its `registerDialect` / `registerStatements` methods when replacing one part. For a built-in database, update the initial mappings as well.

### Step 5: Add database tests

Manage the JDBC driver through the version catalog and add the integration test in `kronos-testing/src/test/kotlin`. Reuse shared integration suites, then cover database-specific generated keys, type normalization, defaults, indexes, upserts, pagination and schema synchronization. `H2IntegrationTest` is an in-process reference.

Run the syntax and core suites, followed by the new database's integration class. When touching shared rendering or metadata contracts, repeat the existing database suites affected by the change.

---

## Add a New DSL Operation

Example: adding a `KTableForGroupBy` DSL.

### Step 1: Create DSL bean
File: `kronos-core/src/com/kotlinorm/beans/dsl/KTableForGroupBy.kt`
```kotlin
class KTableForGroupBy<T : KPojo> {
    // Operator overloads for the DSL
    // Follow KTableForSelect pattern for field selection
    operator fun T.plus(other: T): Int = 1
    operator fun T.unaryPlus(): Int = 1
}
```

### Step 2: Add type alias
File: `kronos-core/src/com/kotlinorm/types/KTableField.kt`
```kotlin
typealias KTableGroupByField<T, R> = KTableForGroupBy<T>.() -> R
```

### Step 3: Create compiler plugin transformer
File: `kronos-compiler-plugin/src/main/kotlin/.../transformers/GroupByTransformer.kt`
```kotlin
class GroupByTransformer(...) : KTableTransformer(...) {
    // Follow SelectTransformer pattern
    // Use FieldAnalysis to parse field expressions
}
```

### Step 4: Add constants
File: `kronos-compiler-plugin/.../utils/Constants.kt`
```kotlin
val kTableForGroupByFqName = FqName("com.kotlinorm.beans.dsl.KTableForGroupBy")
```

### Step 5: Add dispatch in KronosParserTransformer
File: `kronos-compiler-plugin/.../transformers/KronosParserTransformer.kt`
In `visitFunctionNew()`:
```kotlin
kTableForGroupBySymbol -> GroupByTransformer(...)
```

### Step 6: Wire into ORM clause
Add `groupBy {}` method to `SelectClause` (or wherever it's used) that accepts the new DSL lambda.

### Step 7: Write tests
- Compiler plugin test: verify IR transformation
- Unit test: verify SQL generation
- Integration test: verify against real DB

---

## Add Subquery Syntax

Subquery support already exists in the AST (`SubqueryExpression`, `InSubqueryExpression`, `ScalarSubquery`, `ExistsExpression`, `SubqueryTableReference`). To add new subquery DSL syntax:

### For WHERE IN (subquery)
The AST node `InSubqueryExpression(expr, query)` exists. To expose it in the DSL:

1. Add method to `KTableForCondition`:
```kotlin
fun <R : KPojo> KPojo.inSelect(subquery: SelectClause<R>): Boolean
```

2. In `ConditionAnalysis`, handle the new `inSelect` call:
   - Recognize the function name
   - Build the subquery's `SelectStatement` AST
   - Wrap in `InSubqueryExpression`
   - Return `Criteria(type=InSubquery, ...)`

3. Ensure `SqlRenderer` handles `InSubqueryExpression` rendering (already implemented in base renderers).

### For FROM (subquery)
The AST node `SubqueryTableReference(query, alias)` exists. To use it:

1. Add a method that wraps a `SelectClause` as a table reference
2. Wire into `SelectClause.from()` or join operations
3. The renderer already handles `SubqueryTableReference`

### For EXISTS (subquery)
The AST node `ExistsExpression(query)` exists:

1. Add `exists(subquery)` to `KTableForCondition`
2. Handle in `ConditionAnalysis`
3. Renderer support already exists

---

## Add a New SQL Function

### Option A: Built-in function builder
Add to an existing builder or create a new one in `functions/bundled/builders/`:

```kotlin
object MyFunctionBuilder : FunctionBuilder {
    override val supportFunctionNames = mapOf(
        DBType.Mysql to listOf("my_func"),
        DBType.Postgres to listOf("my_func")
    )
    override fun transform(field: FunctionField, ...): String {
        return "MY_FUNC(${renderArgs(field.fields)})"
    }
    override fun transformAst(function: FunctionCall, ...): String {
        return "MY_FUNC(${renderArgs(function.args)})"
    }
}
```

Register in `FunctionManager`:
```kotlin
// In FunctionManager initialization
private val functionBuilders = mutableListOf(
    ..., MyFunctionBuilder
)
```

### Option B: Extension function on FunctionHandler
Add to `functions/bundled/exts/`:
```kotlin
fun FunctionHandler.myFunc(field: Field): FunctionField {
    return FunctionField("my_func", listOf(field to null))
}
```

Usage: `User().select { f.myFunc(it.name) }.queryList()`

---

## Add a New Annotation

### Step 1: Define annotation
File: `kronos-core/src/com/kotlinorm/annotations/MyAnnotation.kt`
```kotlin
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)  // must be BINARY for compiler plugin
annotation class MyAnnotation(val value: String = "")
```

### Step 2: Add constant in compiler plugin
File: `kronos-compiler-plugin/.../utils/Constants.kt`
```kotlin
val myAnnotationFqName = FqName("com.kotlinorm.annotations.MyAnnotation")
val myAnnotationClassId = ClassId.topLevel(myAnnotationFqName)
```

### Step 3: Handle in KronosClassBodyGenerator
Read the annotation in the relevant generator method (e.g., `createKronosColumns()` or a new strategy method).

### Step 4: Wire into ORM logic
Use the annotation data in clause classes, rendering, or strategy application.

---

## Write a Compiler Plugin Test

Use official compiler testData for compiler-plugin behavior. Do not add kotlin-compile-testing / kctfork tests.

Step 1: add a focused `box` test under `kronos-compiler-plugin/testData/box/<area>/<case>.kt`.

```kotlin
/**
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Verifies that equality conditions are transformed into EQUAL criteria.

import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.enums.ConditionType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select

data class User(
    val id: Int? = null,
    val name: String? = null,
) : KPojo

fun box(): String {
    val clause = User().select().where { it.name == "test" }
    val criteria = clause.where as? Criteria
        ?: return "Fail: where was ${clause.where}"

    if (criteria.type != ConditionType.EQUAL) return "Fail: type was ${criteria.type}"
    if (criteria.field.name != "name") return "Fail: field was ${criteria.field.name}"
    if (criteria.value != "test") return "Fail: value was ${criteria.value}"
    return "OK"
}
```

Step 2: add a thin runner method in `kronos-compiler-plugin/src/test/kotlin/com/kotlinorm/compiler/<Area>BoxTest.kt`.

```kotlin
class ConditionBoxTest : AbstractKronosJvmBoxSuite("condition") {
    @Test
    fun equalityCondition() = box("equalityCondition")
}
```

Use `testData/diagnostics` for invalid source that should fail compilation. Use ordinary unit tests for pure helpers that do not require generated declarations or DSL IR rewriting.

Run:

```bash
scripts/run build test -m kronos-compiler-plugin -p jvm --include-classes='*ConditionBoxTest'
```

---

## Write an Integration Test

File: `kronos-testing/src/test/kotlin/com/kotlinorm/testing/MyDbTest.kt`

```kotlin
class MyFeatureTest {
    companion object {
        val mysqlWrapper by lazy {
            BasicDataSource().apply {
                driverClassName = "com.mysql.cj.jdbc.Driver"
                url = "jdbc:mysql://localhost:3306/kronos_testing"
                username = System.getenv("MYSQL_USERNAME") ?: "kronos"
                password = System.getenv("MYSQL_PASSWORD") ?: ""
            }.let { KronosBasicWrapper(it) }
        }

        @BeforeAll @JvmStatic
        fun setup() {
            Kronos.dataSource = { mysqlWrapper }
            Kronos.fieldNamingStrategy = lineHumpNamingStrategy
            Kronos.tableNamingStrategy = lineHumpNamingStrategy
            mysqlWrapper.table.syncTable(TestEntity())
        }

        @AfterAll @JvmStatic
        fun cleanup() {
            mysqlWrapper.table.dropTable(TestEntity())
        }
    }

    @Test
    fun testMyFeature() {
        val entity = TestEntity(name = "test")
        entity.insert().execute()
        val result = TestEntity(name = "test").select().by { it.name }.queryOne()
        assertEquals("test", result.name)
    }
}
```

Run: `scripts/run build test -m kronos-testing -p jvm --include-classes='*MyFeatureTest'`

---

## Write a Unit Test for kronos-core

File: `kronos-core/test@jvm/com/kotlinorm/MyTest.kt`

```kotlin
class MyTest {
    @Test
    fun testSqlGeneration() {
        // kronos-core tests have the compiler plugin active via
        // the shared runtime module template
        val user = User(name = "test", age = 18)
        val clause = user.select().where { it.age > 18 }
        val task = clause.build()
        assertTrue(task.sql.contains("WHERE"))
        assertTrue(task.paramMap.containsKey("age"))
    }
}
```

Run: `scripts/run build test -m kronos-core -p jvm`

---

## Add a New ORM Strategy

Example: adding a "tenant ID" strategy.

1. Add annotation: `@TenantId` in `annotations/`
2. Add strategy field to `Kronos.kt`: `var tenantIdStrategy = KronosCommonStrategy(false, Field("tenant_id"))`
3. Add `kronosTenantId()` to `KPojo` interface
4. Generate body in `KronosClassBodyGenerator.createKronosSpecialField()`
5. Apply in clause classes: auto-add tenant_id to INSERT values, auto-add to WHERE in SELECT/UPDATE/DELETE
6. Add compiler plugin constant and symbol resolution

---

## Debug Compiler Plugin IR Output

### Enable IR Dump
In your `build.gradle.kts`:
```kotlin
kronos {
    dumpIr = true
    dumpIrPath = "build/tmp/kronosDebug"
    dumpIrMode = "kotlinLike"  // human-readable, or "common" for raw IR
    dumpIrFiles = "User.kt"   // filter specific files, empty = all
}
```

### Read the Output
After compilation, check `build/tmp/kronosDebug/` for transformed IR files. Compare with the original source to verify transformations.

### Enable Debug Logging
```kotlin
kronos {
    debug = true  // prints transformation steps to console
}
```

---

## Release a New Version

1. Set the release version with `scripts/run version set 0.4.0`.
2. Complete the release checklist in `references/testing-and-ci.md`, including bilingual release notes and dependency examples.
3. Run the affected Toolchain tests and publication/editor gates.
4. Stage and verify the complete Maven repository on macOS with Xcode.
5. Create the signed Central bundle and publish it.

```bash
scripts/run release stage --repository build/release/repository
scripts/run release verify --repository build/release/repository
scripts/run release bundle --repository build/release/repository --output build/release/central-bundle.zip --sign
scripts/run release publish --output build/release/central-bundle.zip
```

For local JVM consumers use `scripts/run release jvm-stage --repository build/release/jvm-repository`. Reference applications select that absolute repository path with `-PkronosRepository=...`.

After completing the release, `scripts/run version next-snapshot` prepares the next development version. The release workflow owns repository tags and GitHub Release automation.
