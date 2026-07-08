{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 设置 Kotlin 项目基线

Kronos 编译期支持由 Kotlin 构建加载。项目使用 JDK 8+、Kotlin 2.4.0+、Maven 3.9+，或 Kotlin 2.4.0 支持的 Gradle 版本。

```kotlin group="Baseline 1" name="gradle(kts)" icon="gradlekts"
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.4.0"
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}
```

```xml group="Baseline 1" name="maven" icon="maven"
<properties>
    <kotlin.version>2.4.0</kotlin.version>
    <maven.compiler.release>8</maven.compiler.release>
</properties>
```

先让基础 Kotlin 构建通过，再添加 Kronos 数据源和 ORM 代码。

```text group="Baseline 2" name="output"
> Task :compileKotlin
BUILD SUCCESSFUL
```

## 启用{{ $.title("kronos-gradle-plugin") }}

在 JVM Kotlin 项目中添加 Kronos Gradle 插件和 `kronos-core`。Gradle 插件会把 Kronos 编译期支持接入 `compileKotlin`。

```kotlin group="Gradle 1" name="build.gradle.kts" icon="gradlekts"
plugins {
    kotlin("jvm") version "2.4.0"
    id("com.kotlinorm.kronos-gradle-plugin") version "{{ $.kronosVersion() }}"
}

dependencies {
    implementation("com.kotlinorm:kronos-core:{{ $.kronosVersion() }}")
}
```

运行 Kotlin 编译任务，并检查插件加载输出。

```bash group="Gradle 2" name="shell" icon="terminal"
./gradlew compileKotlin
```

```text group="Gradle 2" name="output"
Loaded Gradle plugin com.kotlinorm.compiler.plugin.KronosGradlePlugin version {{ $.kronosVersion() }}
Loaded Compiler plugin com.kotlinorm.kronos-compiler-plugin version {{ $.kronosVersion() }}
BUILD SUCCESSFUL
```

## 启用{{ $.title("kronos-maven-plugin") }}

在 Maven 中，将 `kronos-core` 作为应用依赖，并在 `kotlin-maven-plugin` 中注册 `kronos-maven-plugin`。

```xml group="Maven 1" name="pom.xml" icon="maven"
<project>
    <properties>
        <kotlin.version>2.4.0</kotlin.version>
        <kronos.version>{{ $.kronosVersion() }}</kronos.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>com.kotlinorm</groupId>
            <artifactId>kronos-core</artifactId>
            <version>${kronos.version}</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.jetbrains.kotlin</groupId>
                <artifactId>kotlin-maven-plugin</artifactId>
                <version>${kotlin.version}</version>
                <extensions>true</extensions>
                <configuration>
                    <compilerPlugins>
                        <plugin>kronos-maven-plugin</plugin>
                    </compilerPlugins>
                </configuration>
                <dependencies>
                    <dependency>
                        <groupId>com.kotlinorm</groupId>
                        <artifactId>kronos-maven-plugin</artifactId>
                        <version>${kronos.version}</version>
                    </dependency>
                </dependencies>
            </plugin>
        </plugins>
    </build>
</project>
```

使用 Maven 编译，并检查 Kronos 插件加载输出。

```bash group="Maven 2" name="shell" icon="terminal"
mvn compile
```

```text group="Maven 2" name="output"
Loaded Maven plugin com.kotlinorm.compiler.plugin.KronosMavenPlugin
[INFO] BUILD SUCCESS
```

## 编译每个 Kronos source set

每个声明 `KPojo` 类或使用 Kronos DSL 的 JVM source set 都需要启用 Kronos 构建插件。只消费已编译实体的模块可以正常依赖上游模块；但只要本模块声明实体或编写查询 DSL，就需要在本模块编译时加载 Kronos 插件。

```kotlin group="Source set 1" name="module layout" icon="kotlin"
// :domain 声明 KPojo，需要应用 Kronos Gradle 或 Maven 插件。
@Table("tb_user")
data class User(
    @PrimaryKey
    var id: Int? = null,
    var name: String? = null
) : KPojo

// :service 基于 User 编写 DSL，也需要经过 Kronos 编译期支持。
val names = User()
    .select { [it.id, it.name] }
    .where { it.name like "A%" }
    .queryList()
```

最快的检查方式是编译拥有这些源码的模块。

```bash group="Source set 2" name="shell" icon="terminal"
./gradlew :service:compileKotlin
```

```text group="Source set 2" name="output"
[Kronos] Kronos compiler plugin K2 initialized
BUILD SUCCESSFUL
```

## 检查生成的{{ $.code("KPojo") }}成员

构建插件生效后，`KPojo` 可以在运行时检查中暴露生成的元数据和 Map 转换方法。

```kotlin group="KPojo Check 1" name="User.kt" icon="kotlin"
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo

@Table("tb_user")
data class User(
    @PrimaryKey
    var id: Int? = null,
    var name: String? = null
) : KPojo

fun main() {
    val user = User(7, "Ada")

    println(user.__tableName)
    println(user.toDataMap())
    println(user.kronosColumns().map { it.name })
}
```

输出应包含表元数据和 Kronos 收集到的属性值。

```text group="KPojo Check 2" name="output"
tb_user
{id=7, name=Ada}
[id, name]
```

> **Note**
> 如果出现 `__tableName must be overridden by the compiler plugin`，表示该 source set 没有经过 Kronos 编译期支持处理。

## 检查 DSL SQL 输出

使用一个小的 `select` 查询确认 Kotlin 属性访问和条件表达式已转换为 Kronos SQL。

```kotlin group="DSL Check 1" name="kotlin" icon="kotlin"
import com.kotlinorm.orm.select.select

val (sql, params) = User()
    .select { [it.id, it.name] }
    .where { it.id == 7 }
    .build()

println(sql)
println(params)
```

使用 MySQL 数据源时，检查输出会包含选择字段和命名参数。

```text group="DSL Check 2" name="output"
SELECT `id`, `name` FROM `tb_user` WHERE `tb_user`.`id` = :id
{id=7}
```

条件操作符和 query-by-example 行为请参考 {{ $.keyword("query/conditions", ["Where、Having 和 On 条件"]) }}。

## 使用 {{ $.title("kronos-syntax") }} 的 DSL 规则

Kronos 的查询、变更和 DDL API 都会在执行前生成可检查的 SQL 任务。需要先查看 SQL 和参数时，调用 `build()`，不需要连接数据库。

```kotlin group="Syntax 1" name="projection and condition" icon="kotlin"
val task = User()
    .select {
        [
            it.id,
            it.name.alias("username"),
            f.length(it.name).alias("nameLength")
        ]
    }
    .where { (it.id in listOf(1, 2, 3)) && it.name like "A%" }
    .build()

println(task.sql)
println(task.paramMap)
```

```text group="Syntax 1" name="output"
SELECT `id`, `name` AS `username`, LENGTH(`name`) AS `nameLength`
FROM `tb_user`
WHERE `tb_user`.`id` IN (:idList) AND `tb_user`.`name` LIKE :name
{idList=[1, 2, 3], name=A%}
```

同一套规则适用于 `where`、`having` 和 `on`：Kotlin 比较会变成 SQL 谓词，`&&` 和 `||` 保留布尔组合，`in` 可以接集合或可查询子查询，非直接字段投影需要 `.alias("name")`。

```kotlin group="Syntax 2" name="derived projection" icon="kotlin"
val nameLengths = User()
    .select { [it.id, f.length(it.name).alias("nameLength")] }

val rows = nameLengths
    .select { [it.id, it.nameLength] }
    .where { it.nameLength > 8 }
    .queryList()
```

投影、子查询和生成结果形态示例见 {{ $.keyword("query/projection", ["投影"]) }} 和 {{ $.keyword("query/subqueries", ["子查询"]) }}。

## 读取 Kronos 诊断

Kronos 诊断会出现在 IDE 和构建输出中。`KRONOS_` 代码用于定位 DSL 规则，诊断消息会说明修复方式。

```kotlin group="Diagnostics 1" name="invalid select" icon="kotlin"
import com.kotlinorm.orm.select.select

User()
    .select { [it.id, f.length(it.name)] }
```

这个选择项是函数表达式，因此诊断会要求显式设置别名。

```text group="Diagnostics 2" name="output"
User.kt:6:23: error: Non-field select item must declare .alias("name")
KRONOS_SELECT_ITEM_REQUIRES_ALIAS
```

添加别名后重新编译。

```kotlin group="Diagnostics 3" name="fixed select" icon="kotlin"
User()
    .select { [it.id, f.length(it.name).alias("nameLength")] }
```

Kronos 发出的编译插件错误也会以 `[Kronos]` 开头，并可能带有 `Fix:` 行。

```text group="Diagnostics 4" name="kronos output"
User.kt:12:13: error: [Kronos] Unsupported field
  Fix: Use a property reference
```

## 修复常见诊断

通过诊断代码定位需要调整的 DSL 形态。

| 诊断 | 检查点 | 修复方式 |
|------|--------|----------|
| `__tableName must be overridden by the compiler plugin` | 当前 source set 没有经过 Kronos 编译期支持 | 为该模块启用 Gradle 或 Maven 插件后重新编译 |
| `KRONOS_SELECT_ITEM_REQUIRES_ALIAS` | `select { ... }` 中包含函数、聚合、标量子查询、窗口函数或原生 SQL | 添加 `.alias("resultName")` |
| `KRONOS_DUPLICATE_PROJECTION_FIELD` | 两个投影项生成了同名结果属性 | 删除重复字段，或给其中一个投影项设置不同 alias |
| `KRONOS_SELECTED_FIELD_CONFLICTS_WITH_SOURCE` | selected alias 与输入字段同名 | 使用不和源 `KPojo` 字段冲突的 alias |
| `KRONOS_SCALAR_SUBQUERY_REQUIRES_LIMIT` | 标量子查询可能返回多行 | 作为值使用前添加 `.limit(1)` |
| `KRONOS_SCALAR_SUBQUERY_REQUIRES_SINGLE_COLUMN` | 标量子查询选择了多列 | 只选择一个字段，或拆成多个表达式 |
| `KRONOS_PREDICATE_SUBQUERY_COLUMN_COUNT_MISMATCH` | `field in query`、`ANY`、`SOME`、`ALL` 或 row-value `IN` 左右列数不一致 | 让左侧表达式和右侧查询返回相同列数 |
| `KRONOS_ROW_VALUE_TUPLE_REQUIRES_MULTIPLE_FIELDS` | 使用了 `[it.id] in query` 这样的单字段 tuple | 改写为 `it.id in query` |
| `KRONOS_INSERT_SELECT_VALUE_COUNT_MISMATCH` | `insert<Target> { ... }` 值数量与目标字段数量不一致 | 选择与目标可插入列数量一致的值 |
| `KRONOS_INSERT_SELECT_VALUE_TYPE_MISMATCH` | insert-select 值类型和目标字段类型不匹配 | 调整顺序，或把标量表达式 cast 成预期 Kotlin 类型 |

构建配置从 {{ $.keyword("getting-started/quick-start", ["Quick Start"]) }} 开始。数据库执行和 SQL 日志请参考 {{ $.keyword("database/connect-to-db", ["连接到数据库"]) }} 和 {{ $.keyword("configuration/logging", ["日志"]) }}。IDE 检查行为请参考 {{ $.keyword("resources/idea-plugin", ["IntelliJ IDEA 插件"]) }}。
