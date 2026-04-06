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

Example: adding DB2 support.

### Step 1: Add DBType enum value
File: `kronos-core/src/main/kotlin/com/kotlinorm/enums/DBType.kt`
```kotlin
enum class DBType {
    Mysql, Postgres, SQLite, Mssql, Oracle,
    Db2  // add new value
}
```

### Step 2: Create dialect package
Create: `kronos-core/src/main/kotlin/com/kotlinorm/database/db2/`

### Step 3: Implement DatabasesSupport
Create: `database/db2/Db2Support.kt`
```kotlin
object Db2Support : DatabasesSupport {
    override val quotes = Pair("\"", "\"")

    override fun getDBNameFromUrl(url: String): String {
        // Parse DB name from jdbc:db2://host:port/dbname
    }

    override fun getColumnType(field: Field): String {
        // Map Field → DB2 DDL type string
        // e.g., KColumnType.INT → "INTEGER", VARCHAR → "VARCHAR(${field.length})"
    }

    override fun getKColumnType(type: String, length: Int): KColumnType {
        // Map DB2 type string → KColumnType enum
    }

    override fun getSelectSql(info: SelectClauseInfo): KAtomicQueryTask {
        // Build SELECT with DB2-specific pagination, locking
        // DB2 pagination: FETCH FIRST n ROWS ONLY / OFFSET n ROWS
    }

    override fun getInsertSql(tableName, fields, paramMap): KAtomicActionTask { ... }
    override fun getDeleteSql(tableName, where, paramMap): KAtomicActionTask { ... }
    override fun getUpdateSql(tableName, toUpdateFields, where, paramMap): KAtomicActionTask { ... }

    override fun getOnConflictSql(resolver: ConflictResolver): KAtomicActionTask {
        // DB2 upsert: MERGE INTO ... USING ... WHEN MATCHED THEN UPDATE ... WHEN NOT MATCHED THEN INSERT
    }

    override fun getCreateTableSql(tableName, columns, indexes): List<String> { ... }
    override fun getSyncTableSql(...): List<String> { ... }
    override fun showColumnsFrom(tableName): String { ... }
    override fun showIndexesFrom(tableName): String { ... }
    override fun getTableColumns(wrapper, tableName): List<Field> { ... }
    override fun getTableIndexes(wrapper, tableName): List<KTableIndex> { ... }
}
```

Use existing dialects as reference — `MysqlSupport` is the most complete.

### Step 4: Register the dialect
File: `kronos-core/src/main/kotlin/com/kotlinorm/database/RegisteredDBTypeManager.kt`
```kotlin
private val registeredDBTypes = mutableMapOf(
    // ... existing entries ...
    DBType.Db2 to Db2Support
)
```

### Step 5: Add URL pattern matching
Ensure `DBType` can be resolved from `jdbc:db2://` URLs. Check the URL parsing logic in `SqlManager` or `DBType`.

### Step 6: Add function builder (if needed)
If DB2 has dialect-specific SQL functions, create a `Db2FunctionBuilder` and register it:
```kotlin
FunctionManager.registerFunctionBuilder(Db2FunctionBuilder)
```

### Step 7: Add driver dependency to kronos-testing
File: `kronos-testing/build.gradle.kts`:
```kotlin
implementation("com.ibm.db2:jcc:11.5.9.0")
```

### Step 8: Write integration tests
Create test class in `kronos-testing/src/test/kotlin/` following the existing pattern (see "Write an Integration Test" below).

---

## Add a New DSL Operation

Example: adding a `KTableForGroupBy` DSL.

### Step 1: Create DSL bean
File: `kronos-core/src/main/kotlin/com/kotlinorm/beans/dsl/KTableForGroupBy.kt`
```kotlin
class KTableForGroupBy<T : KPojo> {
    // Operator overloads for the DSL
    // Follow KTableForSelect pattern for field selection
    operator fun T.plus(other: T): Int = 1
    operator fun T.unaryPlus(): Int = 1
}
```

### Step 2: Add type alias
File: `kronos-core/src/main/kotlin/com/kotlinorm/types/KTableField.kt`
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
File: `kronos-core/src/main/kotlin/com/kotlinorm/annotations/MyAnnotation.kt`
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

File: `kronos-compiler-plugin/src/test/kotlin/com/kotlinorm/compiler/transformers/MyTest.kt`

```kotlin
class MyTransformerTest {
    @Test
    fun testMyTransformation() {
        // Option 1: IR-level test
        val result = IrTestFramework.compile("""
            import com.kotlinorm.interfaces.KPojo
            import com.kotlinorm.annotations.*

            data class User(
                @PrimaryKey(identity = true) val id: Int? = null,
                val name: String? = null
            ) : KPojo

            fun test() {
                User().select().where { it.name == "test" }
            }
        """)
        assertTrue(result.exitCode == KotlinCompilation.ExitCode.OK)

        // Option 2: Runtime behavior test
        val classLoader = KotlinSourceDynamicCompiler.compile("""
            // ... same source ...
            fun test(): Any {
                return User(name = "hello").toDataMap()
            }
        """)
        val testClass = classLoader.loadClass("TestKt")
        val result = testClass.getMethod("test").invoke(null) as Map<*, *>
        assertEquals("hello", result["name"])
    }
}
```

Run: `./gradlew :kronos-compiler-plugin:test`

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
            Kronos.init {
                dataSource = { mysqlWrapper }
                fieldNamingStrategy = lineHumpNamingStrategy
                tableNamingStrategy = lineHumpNamingStrategy
            }
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
        val result = TestEntity(name = "test").select().queryOne()
        assertEquals("test", result.name)
    }
}
```

Run: `source envsetup.sh && ./gradlew :kronos-testing:test --tests "*.MyFeatureTest"`

---

## Write a Unit Test for kronos-core

File: `kronos-core/src/test/kotlin/com/kotlinorm/MyTest.kt`

```kotlin
class MyTest {
    @Test
    fun testSqlGeneration() {
        // kronos-core tests have the compiler plugin active via
        // kotlinCompilerPluginClasspathTest dependency
        val user = User(name = "test", age = 18)
        val clause = user.select().where { it.age > 18 }
        val task = clause.build()
        assertTrue(task.sql.contains("WHERE"))
        assertTrue(task.paramMap.containsKey("age"))
    }
}
```

Run: `./gradlew :kronos-core:test`

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

### Snapshot (automatic)
Push to `release/0.1.0` → CI runs `publishAllToCentralSnapshots`.

### Release
1. Update version: `bash .github/scripts/bump-version.sh set 0.1.0` (remove -SNAPSHOT)
2. Commit: `git commit -am "chore(version): prepare release 0.1.0"`
3. Create PR to `main`
4. Merge PR → CI automatically:
   - Tags `v0.1.0`
   - Publishes to Maven Central (signed)
   - Bumps to `0.1.1-SNAPSHOT`
   - Commits and pushes

### Manual Release
```bash
bash .github/scripts/bump-version.sh release-from-current
git add -A && git commit -m "chore(version): release $(cat VERSION)"
git tag "v$(cat VERSION)"
git push origin main --tags
./gradlew publishAllToMavenCentral
bash .github/scripts/bump-version.sh next-snapshot
git add -A && git commit -m "chore(version): start next snapshot"
git push origin main
```