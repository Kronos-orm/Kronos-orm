{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

`Kronos` 严格遵循 Semantic Versioning 2.0.0 语义化版本规范。

- **当前版本:** `{{ $.kronosVersion() }}`

## 📝 更新日志

### 0.3.0

- ✨ 新增面向 `KType` 的 `ValueCodec` 转换与序列化链路、生成 KPojo factory 和 enum metadata。`ValueTransformer` 与 `KronosSerializeProcessor` 统一替换为 `Kronos.registerValueCodec(...)`（[#283](https://github.com/Kronos-orm/Kronos-orm/pull/283)）。
- ✨ 新增 `KSelectable<Selected>.filter { ... }`，可通过明确的派生查询边界继续筛选已选择的结果（[#282](https://github.com/Kronos-orm/Kronos-orm/pull/282)）。
- ✨ 新增 Android/JVM SQLite 接入：使用 Gradle 插件、`kronos-core` 和 Android `SQLiteDatabase` wrapper。[Android SQLite](/documentation/zh-CN/database/android-sqlite) 指南与 [kronos-example-android](https://github.com/Kronos-orm/kronos-example-android) 参考应用覆盖 `AndroidSQLiteDataSourceWrapper`、配置、事务和日志（[#284](https://github.com/Kronos-orm/Kronos-orm/pull/284)）。
- ✨ 扩展 iterable predicate 和 SQL 字符串函数的条件 lowering（[#284](https://github.com/Kronos-orm/Kronos-orm/pull/284)）。
- 🐛 通过归一化 schema 列类型、索引定义和默认访问方法，提升 `syncTable()` 的跨方言稳定性；自动生成的 `Long` 时间字段会以 epoch milliseconds 绑定（[#270](https://github.com/Kronos-orm/Kronos-orm/pull/270)、[#276](https://github.com/Kronos-orm/Kronos-orm/pull/276)）。
- 🐛 级联插入会将 assigned、custom、UUID 和 Snowflake 主键继续传递到子节点外键（[#274](https://github.com/Kronos-orm/Kronos-orm/pull/274)）。
- 🐛 在编译期诊断条件中未注册的捕获 KPojo 字段，同时保留普通 Kotlin 捕获值，并新增 `takeUnless` 条件 gate（[#271](https://github.com/Kronos-orm/Kronos-orm/pull/271)、[#272](https://github.com/Kronos-orm/Kronos-orm/pull/272)）。
- 🐛 在 FIR 分析阶段报告不支持的泛型 KPojo 声明，避免后续 IR 生成阶段失败（[#278](https://github.com/Kronos-orm/Kronos-orm/pull/278)）。
- 🧩 为重复 projection 输出新增 source alias override opt-in，并为映射、生成和派生字段使用 `id_1` 这类稳定的 `_N` 名称（[#279](https://github.com/Kronos-orm/Kronos-orm/pull/279)）。
- 📚 补充方言相关的 `@Default` 表达式、逻辑删除默认值和 IDEA 插件发布使用说明（[#275](https://github.com/Kronos-orm/Kronos-orm/pull/275)、[#277](https://github.com/Kronos-orm/Kronos-orm/pull/277)）。
- 🔧 在 CI 中使用当前 Maven Local 构件检查外部 Ktor、Spring Boot、Solon、Vert.x 和 Android 示例（[#285](https://github.com/Kronos-orm/Kronos-orm/pull/285)）。

#### 升级说明

- 将 `ValueTransformer` 和 `KronosSerializeProcessor` 注册替换为 `Kronos.registerValueCodec(...)` 与 `ValueCodec` API。
- 当前条件 source 之外的捕获 KPojo 读取 Kotlin 属性时需要通过 `.value`。泛型 KPojo 需要改为使用具体属性类型，而不是类类型参数。
- 重复 projection 输出需要使用 `@OptIn(UnsafeProjectionOverride::class)`。自动消歧名称改为 `_N`，读取 `id@1` 这类 key 的代码需要改用 `id_1` 或显式 alias。

### 0.2.4

- 🐛 修复 identity-source select 的生成投影类型，并在 alias、生成投影、window 派生查询与嵌套投影中保留包括 `@Serialize` 在内的源字段元数据 ([#246](https://github.com/Kronos-orm/Kronos-orm/issues/246))。
- 🐛 修复普通 Kotlin `if` / `when` 条件的编译器 lowering，只生成实际选中分支的 SQL 与参数；`contains`、`startsWith`、`endsWith` 会转义 LIKE 字面量通配符 ([#249](https://github.com/Kronos-orm/Kronos-orm/issues/249))。
- 🐛 通过运行时 source identity 绑定与参数重命名，修复自连接、同表多次连接、关联子查询和嵌套查询层 ([#252](https://github.com/Kronos-orm/Kronos-orm/issues/252))。
- 🐛 修复命名参数绑定：只在显式列表位置展开集合，`ByteArray` 及其他数组作为单个 JDBC 参数绑定；为空时间和二进制值增加 JDBC 类型提示 ([#251](https://github.com/Kronos-orm/Kronos-orm/issues/251))。
- 🐛 游标分页会自动追加主键或唯一键 tie-breaker，为 Map 结果传递并隐藏内部游标字段，避免非唯一排序值上的同值行丢失；同时修复 `limit(0)` ([#248](https://github.com/Kronos-orm/Kronos-orm/issues/248))。
- 🐛 修复自定义生成主键的 upsert 冲突目标推断，并将聚合存在性检查改为 `SELECT 1`，使 PostgreSQL 行锁可正常使用 ([#247](https://github.com/Kronos-orm/Kronos-orm/issues/247))。
- 🐛 改进时间类型转换、DDL/默认值渲染、布尔谓词、CREATE TABLE AS SELECT、schema sync、codegen 数据源 setter 与多方言 mutation SQL ([#250](https://github.com/Kronos-orm/Kronos-orm/issues/250))。

#### 升级说明

- 将条件中的 `ifNoValue(...)` 改为普通 Kotlin 控制流：用 `.takeIf(...)` 跳过条件，或用 `if` / `else` 搭配 `true.asSql()` / `false.asSql()` 设置显式 fallback。
- 当排序本身不唯一时，游标分页现在要求模型存在主键或唯一索引。Map 结果会隐藏自动追加的 tie-breaker 列；类型化投影必须选出构造下一个 cursor token 所需的全部字段。

### 0.2.3

- ✨ 使用类型化游标分页 `cursor(pageSize, after)`，返回执行阶段 `CursorResult(hasNext, nextCursor, records)`。
- ✨ 使用 `page(...).withTotal().toList()` / `toMapList()` 返回命名的 `PageResult(total, records, totalPages, pageIndex, pageSize)`；offset page 仍可作为派生 source。
- 🐛 修复派生 source 与 window alias 投影 SQL，确保 `rn`、`userName`、`createTime` 等逻辑输出名可继续用于后续查询层。
- 🧩 改进 IDEA 插件投影体验，包括 `it.` 上的生成投影字段补全，以及 IntelliJ 取消异常的安全处理。
- 📚 将 README、文档宏、发布片段和 AI skill 指南同步到 `0.2.3`。

### 0.2.2

- ✨ 新增基于 metadata 的 KPojo 运行时属性，并刷新生成列访问相关的公开 metadata 使用说明 ([#240](https://github.com/Kronos-orm/Kronos-orm/pull/240))。
- 🐛 修复 compiler/core DSL 对 null-safe 条件、Elvis/selectable set 值、生成键、级联与策略边界场景，以及 DDL metadata 大小写的处理 ([#240](https://github.com/Kronos-orm/Kronos-orm/pull/240))。
- 🐛 将 `kronos-syntax` 作为 `kronos-core` 的 API 依赖发布，使下游项目在未直接声明 `kronos-syntax` 时也能编译暴露 syntax 类型的 DSL 调用 ([#241](https://github.com/Kronos-orm/Kronos-orm/pull/241))。
- 🎨 刷新文档 UI 细节，包括暗色主题语言切换器、Logo 资源和响应式文档布局 ([#238](https://github.com/Kronos-orm/Kronos-orm/pull/238), [#239](https://github.com/Kronos-orm/Kronos-orm/pull/239), [#240](https://github.com/Kronos-orm/Kronos-orm/pull/240))。
- 🔧 为 Ktor、Spring Boot 和 Solon 示例项目加入 smoke test，在 CI 中发布当前构件到 Maven local，对本地发布的 release 版本跳过签名，并将示例同步到当前 metadata API ([#240](https://github.com/Kronos-orm/Kronos-orm/pull/240), [#241](https://github.com/Kronos-orm/Kronos-orm/pull/241))。
- 💪 扩充 projection、condition、set、工具函数、SQL Server 和策略边界场景的 compiler、core、integration 与 example 覆盖 ([#240](https://github.com/Kronos-orm/Kronos-orm/pull/240))。
- 📦 准备稳定版 `0.2.2` 发布，并同步安装片段、文档宏、插件 README 和 AI skill 指南。

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
- 旧 transformer 与 serialization processor API 被一个双向 `ValueCodec` 注册表取代。通过 `Kronos.registerValueCodec` 注册；匹配和转换从 `ValueCodecContext` 获得完整的源类型与目标类型元数据。
- `serializedValueCodec` 将一组编码/解码函数适配到所有 `@Serialize` 字段，包括泛型和嵌套集合类型；不再存在独立的序列化注册表。
- `Field` 构造参数中的 `cascadeIsCollectionOrArray`、`kClass` 和 `superTypes` 合并为 `kType`。现在只有 `kType` 是声明类型身份；`elementKType` 和 `cascadeIsCollectionOrArray` 仍是派生辅助属性，而 `Field.kClass` 已不再属于 API。

#### 功能与修复

- ✨ 在字段、查询任务、序列化字段和 ValueCodec 链路中保留完整 Kotlin `KType`，支持 `List<List<String>>` 等嵌套泛型集合 ([#232](https://github.com/Kronos-orm/Kronos-orm/pull/232))
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

## 使用快照版本

快照版本提供下一稳定版之前的功能，可用于开发和验证。

[![Maven Central Snapshots](https://img.shields.io/badge/Maven%20Central%20Snapshots-v{{ $.kronosSnapshotBadgeVersion() }}-blue?link=https%3A%2F%2Fcentral.sonatype.com%2Fservice%2Frest%2Frepository%2Fbrowse%2Fmaven-snapshots%2Fcom%2Fkotlinorm%2F)](https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/com/kotlinorm/)

在插件和依赖解析中加入快照仓库，再使用快照坐标。

```groovy name="gradle(groovy)" icon="gradle" group="snapshot dependency"
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

```kotlin name="gradle(kts)" icon="gradlekts" group="snapshot dependency"
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

```xml name="maven" icon="maven" group="snapshot dependency"
<project>
  <properties>
    <kronos.version>{{ $.kronosSnapshotVersion() }}</kronos.version>
  </properties>

  <repositories>
    <repository>
      <id>maven-central-snapshots</id>
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
      <version>${kronos.version}</version>
    </dependency>
    <dependency>
      <groupId>com.kotlinorm</groupId>
      <artifactId>kronos-jdbc-wrapper</artifactId>
      <version>${kronos.version}</version>
    </dependency>
  </dependencies>

  <pluginRepositories>
    <pluginRepository>
      <id>maven-central-snapshots</id>
      <url>https://central.sonatype.com/repository/maven-snapshots/</url>
      <releases>
        <enabled>false</enabled>
      </releases>
      <snapshots>
        <enabled>true</enabled>
      </snapshots>
    </pluginRepository>
  </pluginRepositories>

  <build>
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
  </build>
</project>
```
