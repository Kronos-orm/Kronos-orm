# AST, SQL Rendering & Database Dialects

## Table of Contents
1. [SqlNode Type Hierarchy](#sqlnode-type-hierarchy)
2. [Statement Nodes](#statement-nodes)
3. [Expression Nodes](#expression-nodes)
4. [TableReference Nodes](#tablereference-nodes)
5. [Supporting AST Types](#supporting-ast-types)
6. [SQL Rendering Pipeline](#sql-rendering-pipeline)
7. [DatabasesSupport Interface](#databasessupport-interface)
8. [Dialect Implementations](#dialect-implementations)
9. [RegisteredDBTypeManager](#registereddbypemanager)
10. [SqlManager Facade](#sqlmanager-facade)
11. [Functions System](#functions-system)
12. [Named Parameter Parsing](#named-parameter-parsing)

---

## SqlNode Type Hierarchy

```
SqlNode (sealed interface)
├── Expression (sealed interface)
│   ├── ColumnReference(name, table?, alias?)
│   ├── Literal (sealed)
│   │   ├── StringLiteral(value)
│   │   ├── NumberLiteral(value)
│   │   ├── BooleanLiteral(value)
│   │   ├── NullLiteral
│   │   ├── DateLiteral(value)
│   │   ├── TimeLiteral(value)
│   │   └── TimestampLiteral(value)
│   ├── Parameter (sealed)
│   │   ├── NamedParameter(name)
│   │   └── PositionalParameter(index)
│   ├── BinaryExpression(left, operator, right)
│   ├── UnaryExpression(operator, operand)
│   ├── FunctionCall(name, args, distinct?, alias?)
│   ├── CaseExpression (sealed)
│   │   ├── SimpleCaseExpression(operand, branches, elseResult?)
│   │   └── SearchedCaseExpression(branches, elseResult?)
│   ├── SubqueryExpression (sealed)
│   │   ├── ExistsExpression(query)
│   │   ├── ScalarSubquery(query)
│   │   └── QuantifiedComparison(operator, quantifier, query)
│   └── SpecialExpression (sealed)
│       ├── BetweenExpression(expr, low, high)
│       ├── InExpression(expr, values)
│       ├── InSubqueryExpression(expr, query)
│       ├── IsNullExpression(expr)
│       ├── LikeExpression(expr, pattern)
│       └── RawSqlExpression(sql)
├── Statement (sealed interface)
│   ├── SelectStatement(columns, from, where, groupBy, having, orderBy, limit, distinct, lock, joins)
│   ├── InsertStatement(table, columns, values, conflictResolver?)
│   ├── UpdateStatement(table, assignments, where)
│   ├── DeleteStatement(table, where)
│   ├── UnionStatement(queries, unionAll, orderBy?, limit?)
│   └── DdlStatement (sealed)
│       ├── CreateTable(table, columns, indexes, ifNotExists)
│       ├── DropTable(table, ifExists)
│       └── TruncateTable(table)
└── TableReference (sealed interface)
    ├── SimpleTableReference(name, alias?)
    ├── JoinTableReference(left, right, joinType, condition)
    ├── SubqueryTableReference(query, alias)
    └── UnionTableReference(union, alias)
```

---

## Statement Nodes

### SelectStatement
```kotlin
data class SelectStatement(
    val columns: List<SelectItem>,     // column expressions + aliases
    val from: TableReference?,
    val where: Expression? = null,
    val groupBy: List<Expression>? = null,
    val having: Expression? = null,
    val orderBy: List<OrderByItem>? = null,
    val limit: LimitClause? = null,
    val distinct: Boolean = false,
    val lock: PessimisticLock? = null,
    val joins: List<Join>? = null
) : Statement
```

### InsertStatement
```kotlin
data class InsertStatement(
    val table: TableName,
    val columns: List<ColumnReference>,
    val values: List<Expression>,
    val conflictResolver: ConflictResolver? = null  // for upsert
) : Statement
```

### UpdateStatement
```kotlin
data class UpdateStatement(
    val table: TableName,
    val assignments: List<Assignment>,  // column = expression pairs
    val where: Expression? = null
) : Statement
```

### DeleteStatement
```kotlin
data class DeleteStatement(
    val table: TableName,
    val where: Expression? = null
) : Statement
```

### UnionStatement
```kotlin
data class UnionStatement(
    val queries: List<SelectStatement>,
    val unionAll: Boolean = false,
    val orderBy: List<OrderByItem>? = null,
    val limit: LimitClause? = null
) : Statement
```

### DdlStatement
```kotlin
sealed interface DdlStatement : Statement {
    data class CreateTable(table, columns: List<ColumnDef>, indexes: List<IndexDef>, ifNotExists: Boolean)
    data class DropTable(table, ifExists: Boolean)
    data class TruncateTable(table)
}
```

---

## Expression Nodes

### ColumnReference
```kotlin
data class ColumnReference(
    val name: String,
    val table: String? = null,   // table prefix for disambiguation
    val alias: String? = null
) : Expression
```

### BinaryExpression
```kotlin
data class BinaryExpression(
    val left: Expression,
    val operator: BinaryOp,   // AND, OR, EQ, NEQ, GT, LT, GTE, LTE, PLUS, MINUS, TIMES, DIV, MOD
    val right: Expression
) : Expression
```

### FunctionCall
```kotlin
data class FunctionCall(
    val name: String,
    val args: List<Expression>,
    val distinct: Boolean = false,
    val alias: String? = null
) : Expression
```

---

## TableReference Nodes

### JoinTableReference
```kotlin
data class JoinTableReference(
    val left: TableReference,
    val right: TableReference,
    val joinType: JoinType,    // INNER, LEFT, RIGHT, FULL, CROSS
    val condition: Expression
) : TableReference
```

---

## Supporting AST Types

| Type | Fields | Used In |
|------|--------|---------|
| `SelectItem` | `expression: Expression, alias: String?` | SelectStatement.columns |
| `OrderByItem` | `expression: Expression, direction: SortDirection` | SelectStatement.orderBy |
| `LimitClause` | `limit: Int, offset: Int` | SelectStatement.limit |
| `Assignment` | `column: ColumnReference, value: Expression` | UpdateStatement.assignments |
| `Join` | `type: JoinType, table: TableReference, condition: Expression` | SelectStatement.joins |
| `PessimisticLock` | `type: LockType` | SelectStatement.lock |
| `ConflictResolver` | `tableName, onFields, toUpdateFields, toInsertFields` | InsertStatement |
| `ColumnDef` | `name, type, nullable, default, primaryKey, autoIncrement` | CreateTable |
| `IndexDef` | `name, columns, unique, method` | CreateTable |
| `TableName` | `name: String, alias: String?` | All statements |

---

## SQL Rendering Pipeline

```
ORM Clause (e.g., SelectClause)
  → toStatement() builds AST (SelectStatement)
  → SqlManager.getSelectSql(clauseInfo)
    → Resolves DBType from wrapper URL
    → Gets DatabasesSupport from RegisteredDBTypeManager
    → Calls databasesSupport.getSelectSql(clauseInfo)
      → Builds SelectStatement AST
      → Creates SqlRenderer for the dialect
      → SqlRenderer.renderSelect(statement) → SQL string
      → Collects named parameters during rendering
  → Returns KAtomicQueryTask(sql, paramMap)
  → NamedParameterUtils.parseSqlStatement(sql)
    → Converts ":paramName" → "?" positional
    → Returns ParsedSql with parameter order
  → KronosDataSourceWrapper.forList/forMap/forObject(task)
```

---

## DatabasesSupport Interface

Each dialect implements `DatabasesSupport` (an object). Key methods:

```kotlin
interface DatabasesSupport {
    val quotes: Pair<String, String>           // identifier quoting chars, e.g., ("`", "`")
    fun getDBNameFromUrl(url: String): String  // extract DB name from JDBC URL
    fun getColumnType(field: Field): String    // Field → DDL type string (e.g., "VARCHAR(255)")
    fun getKColumnType(type: String, length: Int): KColumnType  // DB type string → KColumnType enum
    fun getSelectSql(info: SelectClauseInfo): KAtomicQueryTask
    fun getJoinSql(info: JoinClauseInfo): KAtomicQueryTask
    fun getInsertSql(tableName, fields, paramMap): KAtomicActionTask
    fun getDeleteSql(tableName, where, paramMap): KAtomicActionTask
    fun getUpdateSql(tableName, toUpdateFields, where, paramMap): KAtomicActionTask
    fun getOnConflictSql(resolver: ConflictResolver): KAtomicActionTask  // upsert
    fun getCreateTableSql(tableName, columns, indexes): List<String>
    fun getDropTableSql(tableName): String
    fun getSyncTableSql(tableName, columns, indexes, existingColumns, existingIndexes): List<String>
    fun showColumnsFrom(tableName): String     // SQL to query column metadata
    fun showIndexesFrom(tableName): String     // SQL to query index metadata
    fun getTableColumns(wrapper, tableName): List<Field>
    fun getTableIndexes(wrapper, tableName): List<KTableIndex>
}
```

---

## Dialect Implementations

### MySQL (`database/mysql/MysqlSupport`)
- Quotes: `` ` `` backticks
- Pagination: `LIMIT n OFFSET m`
- Locking: `FOR UPDATE` / `FOR SHARE`
- Upsert: `ON DUPLICATE KEY UPDATE col1 = VALUES(col1), ...`
- DDL types: `INT`, `BIGINT`, `VARCHAR(n)`, `TEXT`, `DATETIME`, `TINYINT(1)` for Boolean
- Index methods: BTREE, HASH. Index types: UNIQUE, FULLTEXT, SPATIAL

### PostgreSQL (`database/postgres/PostgresqlSupport`)
- Quotes: `"` double quotes
- Pagination: `LIMIT n OFFSET m`
- Locking: `FOR UPDATE` / `FOR SHARE`
- Upsert: `ON CONFLICT (col) DO UPDATE SET col1 = EXCLUDED.col1, ...`
- DDL types: `INTEGER`, `BIGINT`, `VARCHAR(n)`, `TEXT`, `TIMESTAMP`, `BOOLEAN`
- Has custom function builder (`PostgresFunctionBuilder`) for Postgres-specific functions

### SQLite (`database/sqlite/SqliteSupport`)
- Quotes: `"` double quotes
- Pagination: `LIMIT n OFFSET m`
- Locking: Not supported (throws)
- Upsert: `INSERT OR REPLACE INTO ... ON CONFLICT(...) DO UPDATE SET ...`
- DDL types: `INTEGER`, `TEXT`, `REAL`, `BLOB` (SQLite's type affinity)

### SQL Server (`database/mssql/MssqlSupport`)
- Quotes: `[` `]` brackets
- Pagination: `OFFSET n ROWS FETCH NEXT m ROWS ONLY`
- Locking: `WITH (ROWLOCK, UPDLOCK)`
- Upsert: `IF EXISTS (SELECT ...) UPDATE ... ELSE INSERT ...`
- DDL types: `INT`, `BIGINT`, `NVARCHAR(n)`, `NTEXT`, `DATETIME2`, `BIT`

### Oracle (`database/oracle/OracleSupport`)
- Quotes: `"` double quotes
- Pagination: `OFFSET n ROWS FETCH NEXT m ROWS ONLY` (12c+)
- Locking: `FOR UPDATE NOWAIT`
- Upsert: PL/SQL `BEGIN ... EXCEPTION WHEN DUP_VAL_ON_INDEX THEN ... END`
- DDL types: `NUMBER`, `VARCHAR2(n)`, `CLOB`, `TIMESTAMP`, `NUMBER(1)` for Boolean

---

## RegisteredDBTypeManager

```kotlin
object RegisteredDBTypeManager {
    private val registeredDBTypes = mutableMapOf<DBType, DatabasesSupport>(
        DBType.Mysql to MysqlSupport,
        DBType.Postgres to PostgresqlSupport,
        DBType.SQLite to SqliteSupport,
        DBType.Mssql to MssqlSupport,
        DBType.Oracle to OracleSupport
    )
    fun getDBSupport(dbType: DBType): DatabasesSupport
    fun registerDBType(dbType: DBType, support: DatabasesSupport)
}
```

`DBType` is resolved from the JDBC URL pattern (e.g., `jdbc:mysql://` → `DBType.Mysql`).

To add a new dialect: implement `DatabasesSupport`, add a `DBType` enum value, call `registerDBType()`.

---

## SqlManager Facade

`SqlManager` is the central routing point. It resolves `DBType` from the wrapper's URL, gets the `DatabasesSupport`, and delegates:

```kotlin
object SqlManager {
    fun getSelectSql(info) = getDBSupport(info.dbType).getSelectSql(info)
    fun getInsertSql(...) = getDBSupport(dbType).getInsertSql(...)
    fun getUpdateSql(...) = getDBSupport(dbType).getUpdateSql(...)
    fun getDeleteSql(...) = getDBSupport(dbType).getDeleteSql(...)
    fun getOnConflictSql(resolver) = getDBSupport(dbType).getOnConflictSql(resolver)
    fun quoted(field, wrapper) = // wraps field name in dialect quotes
    fun getDBNameFrom(wrapper) = // extracts DB name from URL
    // ... more delegation methods
}
```

---

## Functions System

### FunctionManager
Central registry at `functions/FunctionManager.kt`:
```kotlin
object FunctionManager {
    private val functionBuilders = mutableListOf(
        MathFunctionBuilder, StringFunctionBuilder,
        PolymerizationFunctionBuilder, PostgresFunctionBuilder
    )
    fun registerFunctionBuilder(builder: FunctionBuilder)
    fun getBuiltFunctionField(field, dataSource, showTable, showAlias): String
    fun getBuiltFunctionFieldAst(function, context, renderExpression): String?
}
```

### Built-in Function Builders

| Builder | Functions | Dialect Differences |
|---------|-----------|-------------------|
| `MathFunctionBuilder` | abs, ceil, floor, exp, greatest, least, ln, log, mod, pi, rand, round, sign, sqrt, trunc, add/sub/mul/div | CEILING (MSSQL), RANDOM (SQLite/Postgres), TRUNCATE (MySQL) |
| `StringFunctionBuilder` | length, upper, lower, substr, replace, left, right, repeat, reverse, trim, ltrim, rtrim, concat, join | LEN (MSSQL), GROUP_CONCAT (MySQL) vs STRING_AGG (Postgres/MSSQL) |
| `PolymerizationFunctionBuilder` | count, sum, avg, min, max | Standard across dialects |
| `PostgresFunctionBuilder` | Postgres-specific functions | Only active for Postgres DBType |

### FunctionHandler
`FunctionHandler` is a marker object used as the receiver (`f`) for DSL function calls:
```kotlin
// User code:
User().select { f.count(it.id) + f.max(it.age) }.queryList()
```
The compiler plugin transforms `f.count(it.id)` into `FunctionField("count", [(Field("id"), null)])`.

### Adding Custom Functions
```kotlin
FunctionManager.registerFunctionBuilder(object : FunctionBuilder {
    override val supportFunctionNames = mapOf(DBType.Mysql to listOf("my_func"))
    override fun transform(field: FunctionField, ...): String = "MY_FUNC(${args})"
    override fun transformAst(function: FunctionCall, ...): String = "MY_FUNC(${args})"
})
```

---

## Named Parameter Parsing

`beans/parser/NamedParameterUtils.kt` converts named parameters to positional:

```
Input:  "SELECT * FROM user WHERE name = :name AND age > :age"
Output: ParsedSql("SELECT * FROM user WHERE name = ? AND age > ?", ["name", "age"])
```

Key method: `parseSqlStatement(sql: String): ParsedSql`
- Scans for `:paramName` patterns
- Handles string literals (skips quoted content)
- Handles escape sequences
- Returns `ParsedSql` with positional SQL and ordered parameter names
- Used by `SqlHandler` to bridge named params → JDBC `?` placeholders

---

## Key Enums

### ConditionType
Used in `Criteria.type` to identify the condition kind:
```kotlin
enum class ConditionType(val value: String) {
    LIKE("like"), EQUAL("="), IN("in"), ISNULL("is null"), SQL(""),
    GT(">"), GE(">="), LT("<"), LE("<="),
    BETWEEN("between"), REGEXP("regexp"),
    AND("and"), OR("or"), ROOT("")
}
```
Note: `AND`, `OR`, `ROOT` are structural (grouping children). `SQL` is for raw SQL injection. To add a new condition type (e.g., `IN_SUBQUERY`), add an enum value here and handle it in the SQL rendering pipeline.

### NoValueStrategyType
Controls behavior when a condition value is null:
```kotlin
enum class NoValueStrategyType {
    Ignore,     // skip the condition entirely
    False,      // condition always evaluates to FALSE
    True,       // condition always evaluates to TRUE
    JudgeNull,  // convert to IS NULL check
    Auto        // default: use global Kronos.noValueStrategy
}
```

### KColumnType
Maps Kotlin types to SQL column types:
```
BIT, TINYINT, SMALLINT, INT, BIGINT, FLOAT, DOUBLE, DECIMAL,
CHAR, VARCHAR, TEXT, CLOB, BLOB, BINARY,
DATE, TIME, DATETIME, TIMESTAMP,
UUID, SERIALIZABLE, UNDEFINED
```

### DBType
Database dialect identifier:
```kotlin
enum class DBType { Mysql, Postgres, SQLite, Mssql, Oracle }
```
Add new values here when adding a new dialect.

### PrimaryKeyType
```kotlin
enum class PrimaryKeyType { NOT, IDENTITY, UUID, SNOWFLAKE }
```

### JoinType
```kotlin
enum class JoinType { INNER, LEFT, RIGHT, FULL, CROSS }
```
