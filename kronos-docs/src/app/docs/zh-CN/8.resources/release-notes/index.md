{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

`Kronos` 严格遵循 Semantic Versioning 2.0.0 语义化版本规范。

- **当前版本:** `{{ $.kronosVersion() }}`

## 📝 更新日志

### 0.2.1

- ✨ 支持在投影 `[]` 中使用 `it - [it.id, it.age]`，包括 `[it - [it.id, it.age], it.id.alias("sourceId")]` 这类混合投影列表。
- 🔧 将发布流程中的 next SNAPSHOT PR 步骤改为幂等；重复运行时已有版本 PR 会直接跳过，不再因为同名分支失败。
- 📚 将 README、文档宏、用户示例和 AI skill 引用同步到 `0.2.1`。

### 0.2.0

> **Warning**
> `0.2.0` 包含源码和二进制不兼容的公开 API 变更。升级依赖后需要重新编译项目，并按下面的迁移说明更新调用点和自定义扩展。

#### 破坏性 API 变更

查询终结方法统一使用集合转换和单行读取语义：

| `0.1.x` | `0.2.0` |
| --- | --- |
| `query()` | `toMapList()` / `toList<Map<String, Any?>>()` |
| `queryList<T>()` | `toList<T>()` |
| `queryMap()` | `toMap()` / `first<Map<String, Any?>>()` |
| `queryMapOrNull()` | `toMapOrNull()` / `firstOrNull<Map<String, Any?>>()` / `first<Map<String, Any?>?>()` |
| `queryOne<T>()` | `first<T>()` |
| `queryOneOrNull<T>()` | `firstOrNull<T>()` / `first<T?>()` |
| `PagedClause.query()` | `PagedClause.toMapList()` |
| `PagedClause.queryList()` | `PagedClause.toList()` |

`toMapList()`、`toMap()` 和 `toMapOrNull()` 是对应泛型写法的便捷方法；一般情况下，`firstOrNull<T>()` 也等价于 `first<T?>()`。Map 查询结果类型由 `Map<String, Any>` 改为 `Map<String, Any?>`，被选中的 SQL `NULL` 会保留在结果中。`QueryType` 同步精简为 `ToMapList`、`ToList`、`ToMap` 和 `First`；使用查询事件回调的项目需要更新枚举分支。原生 SQL 扩展改由 `com.kotlinorm.database.SqlExecutor` 提供，原 `SqlHandler` 对象已删除。

内置 JDBC wrapper 已统一为 `KronosJdbcWrapper`。从更早版本升级、仍在使用旧 `KronosBasicWrapper` 的项目需要替换类名和包名：

```kotlin
// 旧 API
import com.kotlinorm.KronosBasicWrapper
val wrapper = KronosBasicWrapper(dataSource)

// 0.2.0
import com.kotlinorm.wrappers.KronosJdbcWrapper
val wrapper = KronosJdbcWrapper(dataSource)
```

自定义 `KronosDataSourceWrapper` 实现需要移除 `forList`、`forMap`、`forObject`，改为实现以下两个查询入口；结果映射目标从 `task.targetType` 读取：

```kotlin
override fun toList(task: KAtomicQueryTask): List<Any?>
override fun first(task: KAtomicQueryTask): Any?
```

查询任务和类型转换链路改用完整 Kotlin `KType`：

- `KAtomicQueryTask` 新增必需的 `targetType: KType`；直接构造 `KronosAtomicQueryTask` 时需要传入 `typeOf<T>()`。
- `KronosSerializeProcessor` 改为 `serialize(obj, kType)` 和 `deserialize(serializedStr, kType)`，不再提供无类型参数的序列化方法或接收 `KClass` 的反序列化方法。
- `ValueTransformer`、`TransformerManager.getValueTransformed`、`getTypeSafeValue`、`getSafeValue` 和 `TransformerSafeValue` 改为接收 `KType`；字符串类型名和 `superTypes` 参数已删除，运行时值类型参数统一命名为 `sourceValueClass`。
- `Field` 构造参数中的 `cascadeIsCollectionOrArray`、`kClass` 和 `superTypes` 合并为 `kType`；`kClass`、`elementKType` 和 `cascadeIsCollectionOrArray` 现在由声明类型延迟计算。

#### 功能与修复

- ✨ 在字段、查询任务、序列化处理器和值转换器链路中保留完整 Kotlin `KType`，支持 `List<List<String>>` 等嵌套泛型集合 ([#232](https://github.com/Kronos-orm/Kronos-orm/pull/232))
- 🐛 修复 Kotlinx Serialization 对泛型集合和 data class 字段的反序列化，并在 Map 与标量查询结果中保留被选中的 `null` 值 ([#232](https://github.com/Kronos-orm/Kronos-orm/pull/232))
- 🐛 修复 `select { it }`、`select { [it] }`、KPojo 排除投影，以及使用 `[]` 混合完整行与 alias 时的生成类型 ([#232](https://github.com/Kronos-orm/Kronos-orm/pull/232))
- 🐛 修复 SQLite UNION 渲染、SQLite 表结构同步、字符串默认值及扩展测试发现的其他 ORM 边缘问题 ([#232](https://github.com/Kronos-orm/Kronos-orm/pull/232))
- 💪 扩充投影、序列化、空值结果、表结构同步和跨数据库回归测试，并将 README、用户文档、AI skill、Gradle 插件和 IDEA 插件同步到 `0.2.0` ([#232](https://github.com/Kronos-orm/Kronos-orm/pull/232))

### 0.1.2

- 🐛 修复 `syncTable()` 表结构差异比较：数据库 metadata 回读的普通主键与 KPojo 中的 `@PrimaryKey(custom = true)` 自定义主键会按同一种数据库主键模式处理 ([#229](https://github.com/Kronos-orm/Kronos-orm/pull/229))
- 🐛 修复 SQLite 在 `ADD COLUMN` 后重复执行表结构同步的问题；SQLite 只能把新增列追加到表尾，因此同步时会忽略仅由列顺序造成的差异，避免生成不支持的 `ALTER COLUMN` ([#229](https://github.com/Kronos-orm/Kronos-orm/pull/229))
- 💪 新增跨数据库集成回归测试：先创建 V1 表，再用新增 `age` 字段的 V2 执行 `syncTable()`，校验主键 metadata，并再次同步验证幂等；覆盖 MySQL、PostgreSQL、SQLite、SQL Server 和 Oracle ([#229](https://github.com/Kronos-orm/Kronos-orm/pull/229))
- 💪 简化 AI skill 安装文档，并统一覆盖 Claude、Codex、Cursor 和通用 agent 的 `main` 分支安装路径 ([#228](https://github.com/Kronos-orm/Kronos-orm/pull/228))
- 🔧 修复新版 JDK 下的文档部署，并在 docs 部署 workflow 中补充 Java 环境初始化 ([#226](https://github.com/Kronos-orm/Kronos-orm/pull/226), [#227](https://github.com/Kronos-orm/Kronos-orm/pull/227))

### 0.1.1

- ✨ 补全投影和子查询 DSL 能力，包括生成结果行类、标量子查询、谓词子查询、INSERT SELECT 和窗口函数 alias 的使用说明 ([#222](https://github.com/Kronos-orm/Kronos-orm/pull/222))
- ✨ 新增 IDEA 插件用户文档，说明生成的 KPojo 形态、投影 receiver、编辑器诊断，以及从 GitHub Release 附件安装插件 zip ([#222](https://github.com/Kronos-orm/Kronos-orm/pull/222))
- 💪 重新整理用户文档结构，覆盖起步、映射、查询、修改、数据库、配置、高级主题和资源页；刷新表映射、内置函数、投影、方言行为和 codegen 工作流示例 ([#222](https://github.com/Kronos-orm/Kronos-orm/pull/222))
- 💪 改进正式发版自动化：发布 Maven 构件，构建 JVM jar 和 IDEA 插件 zip，附加到 GitHub Release，并自动生成 release notes ([#222](https://github.com/Kronos-orm/Kronos-orm/pull/222))
- 🐛 修复编译器插件对 `SelectFrom10` 到 `SelectFrom16` 查询函数的类型参数处理，并补充 query return type 和投影行为的官方编译器测试 ([#222](https://github.com/Kronos-orm/Kronos-orm/pull/222))
- 🔧 所有 Gradle wrapper 改用官方分发地址，避免镜像下载超时导致 CI 失败 ([#222](https://github.com/Kronos-orm/Kronos-orm/pull/222))

### 0.1.0

- ✨ 发布 `0.1.0`，完成核心模块、构建配置和 README 版本引用更新 ([#204](https://github.com/Kronos-orm/Kronos-orm/pull/204), [#207](https://github.com/Kronos-orm/Kronos-orm/pull/207), [#208](https://github.com/Kronos-orm/Kronos-orm/pull/208))
- ✨ 新增 K2 编译器插件实现，替换旧版编译器插件实现，并将 K2 插件作为唯一主插件发布 ([#204](https://github.com/Kronos-orm/Kronos-orm/pull/204))
- ✨ 新增类型安全 SQL AST 与多数据库方言渲染器，覆盖 select、insert、update、delete、upsert、DDL、union 和函数渲染 ([#204](https://github.com/Kronos-orm/Kronos-orm/pull/204))
- ✨ 新增事务 DSL 能力，支持隔离级别、超时、保存点以及嵌套事务复用连接；JDBC wrapper 增加 ThreadLocal 事务连接共享 ([#204](https://github.com/Kronos-orm/Kronos-orm/pull/204))
- 💪 改进级联查询、跨模块测试基础设施、代码生成测试、核心 ORM/方言函数测试和覆盖率 CI ([#204](https://github.com/Kronos-orm/Kronos-orm/pull/204))
- 💪 重构文档站点首页、文档页主题切换、侧栏折叠、复制 Markdown、页脚、AI 文档章节和模块 README ([#204](https://github.com/Kronos-orm/Kronos-orm/pull/204))
- 🐛 修复 upsert `criteriaParams` 注入、`.set {}` 赋值、逻辑删除存在性检查、PostgreSQL `ANY(ARRAY[...])` 类型不匹配以及 PostgreSQL 并发索引建表事务问题 ([#204](https://github.com/Kronos-orm/Kronos-orm/pull/204))
- 🔧 更新 `__tableName` 属性相关文档和插件配置，并调整 detekt 抑制项 ([#206](https://github.com/Kronos-orm/Kronos-orm/pull/206))

### 0.0.7

- 🔧 升级 Gradle 至 9.4.1，Kotlin 至 2.3.0 ([#201](https://github.com/Kronos-orm/Kronos-orm/pull/201))
- 🐛 修复 PostgreSQL 自增主键对 BIGINT 列使用 BIGSERIAL ([#201](https://github.com/Kronos-orm/Kronos-orm/pull/201))
- 💪 重构表名和表注释从函数改为属性（`__tableName`、`__tableComment`）([#198](https://github.com/Kronos-orm/Kronos-orm/pull/198))
- 🔧 修复 bump-version.sh 在 Linux CI 环境下的 sed 语法问题 ([#197](https://github.com/Kronos-orm/Kronos-orm/pull/197))

### 0.0.6

- ✨ 更新 Kotlin 版本至 `2.2.21`，添加对 `kotlin.time.Instant` 的支持 ([#191](https://github.com/Kronos-orm/Kronos-orm/pull/191))
- ✨ 为 select 子句添加 patch 功能 ([#192](https://github.com/Kronos-orm/Kronos-orm/pull/192))
- ✨ 添加 `takeIf` 条件支持，用于条件查询处理 ([#178](https://github.com/Kronos-orm/Kronos-orm/pull/178))
- 💪 增强 upsert 逻辑，支持空值处理和逻辑删除策略 ([#188](https://github.com/Kronos-orm/Kronos-orm/pull/188))
- 🐛 修复 join with on 子句时 criteria 丢失的问题 ([#194](https://github.com/Kronos-orm/Kronos-orm/pull/194))
- 🐛 修复 `java.sql.Date` 转换逻辑，并为 `LocalDate` 类型解析添加测试用例 ([#185](https://github.com/Kronos-orm/Kronos-orm/pull/185))
- 🐛 修复 jdbc wrapper `getObject(position)` 空指针异常 ([#184](https://github.com/Kronos-orm/Kronos-orm/pull/184))
- 🐛 处理列定义中的空默认值 ([#175](https://github.com/Kronos-orm/Kronos-orm/pull/175))

### 0.0.5

- ✨ 为 DeleteClauseInfo 和 InsertClauseInfo 添加 kClass 属性 ([#152](https://github.com/Kronos-orm/Kronos-orm/pull/152))
- 💪 优化查询参数处理，修复无效参数映射 ([#150](https://github.com/Kronos-orm/Kronos-orm/pull/150))

### 0.0.4

- ✨ 添加默认布尔值处理方法 `getDefaultBoolean` ([#147](https://github.com/Kronos-orm/Kronos-orm/pull/147))
- 💪 修改缓存实现，使用现有实例而不是每次创建新实例 ([#145](https://github.com/Kronos-orm/Kronos-orm/pull/145))
- 🐛 确保 PostgreSQL SQL 语句中的表名被正确引用 ([#142](https://github.com/Kronos-orm/Kronos-orm/pull/142))

### 0.0.3

- ✨ 添加新的 CodeGen 模块用于代码生成 ([#130](https://github.com/Kronos-orm/Kronos-orm/pull/130))
- ✨ 添加 DataGuardPlugin，防止对表进行删除、更新和清空操作
- ✨ 添加 LastInsertIdPlugin，支持获取最后插入的 ID
- ✨ 添加任务事件钩子支持，包括 QueryEvent 和 ActionEvent ([#123](https://github.com/Kronos-orm/Kronos-orm/pull/123))
- ✨ 添加缓存和标识配置 ([#122](https://github.com/Kronos-orm/Kronos-orm/pull/122))
- ✨ 添加命名 SQL 解析缓存 ([#116](https://github.com/Kronos-orm/Kronos-orm/pull/116))
- 💪 重构 jdbc wrapper ([#117](https://github.com/Kronos-orm/Kronos-orm/pull/117))
- 💪 重构 LastInsertIdPlugin 启用逻辑，使用插件注册/注销方式 ([#125](https://github.com/Kronos-orm/Kronos-orm/pull/125))
- 💪 更新日志 DSL ([#137](https://github.com/Kronos-orm/Kronos-orm/pull/137))
- 🐛 修复 InsertClause 中的空值处理逻辑以适配 SpringData
- 🐛 修复覆盖率计算中的字段索引 ([#133](https://github.com/Kronos-orm/Kronos-orm/pull/133))
- 🔧 移除 KronosKClassMapperTransformer ([#131](https://github.com/Kronos-orm/Kronos-orm/pull/131))

### 0.0.2

- 🐛 修复 `@CreateTime`、`@Update` 表级注解参数 `enable` 为 `false` 时不生效的问题，
  修复全局表创建时间、更新时间、逻辑删除字段设置，使其仅在 KPojo 包含该字段时生效。([#92](https://github.com/Kronos-orm/Kronos-orm/pull/92))
- 💪 优化实例化性能，取消转换时使用 `map` 作为中间变量。([#94](https://github.com/Kronos-orm/Kronos-orm/pull/92))
- 💪 添加默认数据源的事务创建函数
  `fun transact(wrapper: KronosDataSourceWrapper? = null, block: () -> Any?)`。([#94](https://github.com/Kronos-orm/Kronos-orm/pull/95))
- 💪 为 insert 语句添加缓存支持，性能提升 20%，详细性能数据请查看 kronos-benchmark。([#100](https://github.com/Kronos-orm/Kronos-orm/pull/100))
- ✨ 为 `Field` 添加 `scale` 属性，用于指定小数类型的精度，并重构从 kotlin 类型到 Column 类型的默认映射，将 `java.math.BigDecimal` 更改为 `decimal`（原为 `NUMERIC`），将 `kotlin.ByteArray` 更改为 `BLOB`（原为 `BINARY`）。([#106](https://github.com/Kronos-orm/Kronos-orm/pull/106))

### 0.0.1

- Kronos 的第一个版本发布，提供了 ORM 的所有功能。
- 提供 `Kronos-core`、`Kronos-compiler-plugin`、`Kronos-jdbc-wrapper`、`Kronos-logging`、`Kronos-gradle-plugin`、
  `Kronos-maven-plugin` 等官方库，可直接使用。
- 最低支持的 Kotlin 版本为 `2.2.0`。

> **Note**
> 当前版本为开发阶段，不保证向后兼容性。
>
> 若您有意愿，可以使用最新版本来测试。

```groovy name="gradle(groovy)" icon="gradle" group="dependency"
// settings.gradle
pluginManagement {
    repositories {
        mavenCentral()
        maven {
            url "https://central.sonatype.com/repository/maven-snapshots/"
        }
    }
}

// build.gradle
repositories {
    mavenCentral()
    maven {
        url "https://central.sonatype.com/repository/maven-snapshots/"
    }
}

plugins {
    id 'com.kotlinorm.kronos-gradle-plugin' version '{{ $.kronosSnapshotVersion() }}'
}

dependencies {
    implementation "com.kotlinorm:kronos-core:{{ $.kronosSnapshotVersion() }}"
    implementation "com.kotlinorm:kronos-jdbc-wrapper:{{ $.kronosSnapshotVersion() }}"
}
```

```groovy name="gradle(kts)" icon="gradlekts" group="dependency"
// settings.gradle.kts
pluginManagement {
    repositories {
        mavenCentral()
        maven {
            name = "Maven Central Snapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        }
    }
}

// build.gradle.kts
repositories {
    mavenCentral()
    maven {
        name = "Maven Central Snapshots"
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
    }
}

plugins {
    id("com.kotlinorm.kronos-gradle-plugin") version "{{ $.kronosSnapshotVersion() }}"
}

dependencies {
    implementation("com.kotlinorm:kronos-core:{{ $.kronosSnapshotVersion() }}")
    implementation("com.kotlinorm:kronos-jdbc-wrapper:{{ $.kronosSnapshotVersion() }}")
}
```

```xml name="maven" icon="maven" group="dependency"
<project>
    <repositories>
        <repository>
            <id>maven-center-snapshots</id>
            <url>https://central.sonatype.com/repository/maven-snapshots/</url>
            <releases>
                <enabled>false</enabled>
            </releases>
            <snapshots>
                <enabled>true</enabled>
            </snapshots>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>com.kotlinorm</groupId>
            <artifactId>kronos-core</artifactId>
            <version>{{ $.kronosSnapshotVersion() }}</version>
        </dependency>
        <dependency>
            <groupId>com.kotlinorm</groupId>
            <artifactId>kronos-jdbc-wrapper</artifactId>
            <version>{{ $.kronosSnapshotVersion() }}</version>
        </dependency>
    </dependencies>

    <pluginRepositories>
        <pluginRepository>
            <id>central-portal-snapshots</id>
            <url>https://central.sonatype.com/repository/maven-snapshots/</url>
            <releases>
                <enabled>false</enabled>
            </releases>
            <snapshots>
                <enabled>true</enabled>
            </snapshots>
        </pluginRepository>
    </pluginRepositories>

    <plugins>
        <plugin>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-maven-plugin</artifactId>
            <extensions>true</extensions>
            <configuration>
                <compilerPlugins>
                    <plugin>all-open</plugin>
                    <plugin>kronos-maven-plugin</plugin>
                </compilerPlugins>
            </configuration>
            <dependencies>
                <dependency>
                    <groupId>org.jetbrains.kotlin</groupId>
                    <artifactId>kotlin-maven-allopen</artifactId>
                    <version>${kotlin.version}</version>
                </dependency>
                <dependency>
                    <groupId>com.kotlinorm</groupId>
                    <artifactId>kronos-maven-plugin</artifactId>
                    <version>${kronos.version}</version>
                </dependency>
            </dependencies>
        </plugin>
    </plugins>
</project>
```
