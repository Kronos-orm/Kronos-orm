{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

Kronos IDEA 插件会把 Kronos 编译器插件的信息接入 IntelliJ IDEA。项目已经能通过 Gradle 或 Maven 编译后，安装它可以让编辑器理解生成的 `KPojo` 成员、投影结果类型、子查询形态，以及 Database First 代码生成。

| 能力 | 插件提供什么 |
|------|--------------|
| 项目模型 | 在 IDEA 分析阶段加载随插件打包的 Kronos FIR 编译器插件 |
| 投影文档 | 在 quick documentation 中展示生成的 `KronosSelectResult_*` 和 `KronosSelectContext_*` 形态 |
| 编辑器诊断 | 在编辑器里提示投影、标量子查询、谓词子查询和 INSERT SELECT 的形态错误 |
| Code Generator | 读取 IDEA Database 数据源，预览或写入 `KPojo` 文件 |
| Templates | 将内置 KPojo 模板复制到 `.kronos/templates`，并按项目定制生成代码 |

## 界面概览

投影补全会读取生成的结果行或上下文形态，因此 `nameLength`、`rn`、聚合字段这类 alias 可以直接在编辑器中补全。

<img src="/assets/images/idea-plugin/kronos-idea-projection-completion.png" alt="Kronos IDEA 插件投影补全" style="width: 100%; max-width: 640px; height: auto;" />

在当前查询上下文上打开 quick documentation 时，插件会渲染生成的上下文类，展示 `orderBy { ... }` 等位置可读取的 alias。

<img src="/assets/images/idea-plugin/kronos-idea-projection-context-docs.png" alt="Kronos IDEA 插件投影上下文文档" style="width: 100%; max-width: 640px; height: auto;" />

在局部结果值上打开 quick documentation 时，插件会渲染生成的结果行类，展示字段名和 Kotlin 类型。

<img src="/assets/images/idea-plugin/kronos-idea-projection-docs.png" alt="Kronos IDEA 插件投影文档" style="width: 100%; max-width: 640px; height: auto;" />

`Kronos-ORM` 工具窗口提供 Code Generator 标签页，用于选择 IDEA Database 数据源、选择表、预览生成的 Kotlin，并写入 KPojo 文件。

<img src="/assets/images/idea-plugin/kronos-idea-code-generator.png" alt="Kronos IDEA 插件代码生成器" style="width: 100%; max-width: 640px; height: auto;" />

## 安装 {{ $.title("Kronos-ORM") }}

IDEA 插件会打包为 `kronos-idea-plugin.zip`。可以安装从 GitHub Release 附件下载到本地的 zip，也可以安装本地构建出的 zip。

```text group="Install 1" name="IDEA"
Settings / Preferences -> Plugins -> Install Plugin from Disk...
选择 kronos-idea-plugin.zip
重启 IntelliJ IDEA
```

正式版本会把 IDEA 插件 zip、JVM jar 和 checksum 文件附加到 GitHub Release。GitHub Release notes 会根据已合入的变更自动生成。

```text group="Install 2" name="release artifact"
kronos-idea-plugin-{{ $.kronosVersion() }}.zip
```

从源码构建时，zip 会写入插件分发目录。

```bash group="Install 3" name="source build" icon="terminal"
./gradlew :kronos-idea-plugin:buildPlugin -Pkronos.idea.version=2026.2
```

```text group="Install 3" name="output"
kronos-idea-plugin/build/distributions/kronos-idea-plugin.zip
```

## 匹配项目配置

IDEA 插件负责编辑器分析。命令行构建和 CI 仍然需要 Gradle 或 Maven 编译器插件。

```kotlin group="Project setup 1" name="build.gradle.kts" icon="gradlekts"
plugins {
    kotlin("jvm") version "2.4.0"
    id("com.kotlinorm.kronos-gradle-plugin") version "{{ $.kronosVersion() }}"
}

dependencies {
    implementation("com.kotlinorm:kronos-core:{{ $.kronosVersion() }}")
}
```

安装 IDEA 插件后，重新加载 Gradle 项目或重新导入 Maven 项目，并运行一次 Kotlin 编译。

```bash group="Project setup 2" name="shell" icon="terminal"
./gradlew compileKotlin
```

```text group="Project setup 2" name="output"
[Kronos] Kronos compiler plugin K2 initialized
BUILD SUCCESSFUL
```

IntelliJ IDEA 和 Kotlin 插件需要支持 Kotlin 2.4.0。仓库构建出的正式插件目标 IntelliJ IDEA 版本为 `2026.2`。

## 配置插件

插件安装后会增加 `Kronos-ORM` 工具窗口和设置页面。

| UI 入口 | 用途 |
|---------|------|
| `Kronos-ORM` 工具窗口 | 打开 Kronos IDE 工具和代码生成模板 |
| `Settings / Preferences -> Kronos ORM Setting` | 设置 Kronos config JSON 文件路径 |

设置页会把配置路径保存到 `KronosPluginSettings`，默认值为 `kronos.json`。

```json group="Settings" name="kronos.json"
{
  "dataSources": [
    {
      "dataSourceName": "main",
      "dataSourceUrl": "jdbc:mysql://localhost:3306/kronos_demo",
      "dataSourceUser": "root",
      "dataSourcePassword": "your_password",
      "dataSourceDriver": "com.mysql.cj.jdbc.Driver",
      "default": true
    }
  ],
  "templates": [
    "KPojoTemplate.kts",
    "ServiceTemplate.kts"
  ]
}
```

基于脚本生成实体请使用 {{ $.keyword("resources/codegen", ["代码生成器"]) }}。

## 在 IDEA 中生成 KPojo

从右侧工具窗口栏打开 `Kronos-ORM`。`Code Generator` 标签页读取 IDEA Database Tools 中已经配置的数据源，不需要再维护一份单独的连接列表。

```text group="Code Generator" name="workflow"
1. 在 IDEA Database 中配置数据库连接。
2. 打开 Kronos-ORM -> Code Generator。
3. 选择数据源和一个或多个表。
4. 设置 package name 和 output directory。
5. 选择模板。
6. 点击 Preview 预览生成的 Kotlin。
7. 点击 Generate 写入项目文件。
```

生成器会把表元数据转换为 Kronos 字段：表名变成 `@Table`，主键变成 `@PrimaryKey`，索引变成 `@TableIndex`，SQL 类型转换为 `KColumnType`，数据库返回列注释时会写入生成文件。

```kotlin group="Code Generator" name="output" icon="kotlin"
package com.example.entity

import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo

@Table(name = "tb_user")
data class TbUser(
    var id: Long? = null,
    var name: String? = null,
) : KPojo
```

只有确实要覆盖同名文件时，才启用 `Overwrite existing files`。

## 自定义模板

`Templates` 标签页会列出内置模板和项目模板。使用 `Copy to Project` 可以把内置 KPojo 模板复制到 `.kronos/templates`，再按当前项目修改。

项目模板可使用这些占位符：

| 占位符 | 值 |
|--------|----|
| {{ $.templatePlaceholder("packageName") }} | Code Generator 标签页填写的包名 |
| {{ $.templatePlaceholder("imports") }} | 生成注解和类型需要的 import |
| {{ $.templatePlaceholder("tableComment") }} | 格式化后的表注释 |
| {{ $.templatePlaceholder("generatedAt") }} | 生成时间 |
| {{ $.templatePlaceholder("tableName") }} | 来源表名 |
| {{ $.templatePlaceholder("className") }} | 生成的 Kotlin 类名 |
| {{ $.templatePlaceholder("tableIndexes") }} | 渲染后的 `@TableIndex` 注解 |
| {{ $.templatePlaceholder("fields") }} | 带 Kronos 注解的 Kotlin 属性 |

项目 KPojo 模板需要保留这个标记，IDEA 插件会用它识别支持的模板：

```kotlin group="Template" name="marker" icon="kotlin"
// KRONOS_IDEA_TEMPLATE:KPOJO
```

## 使用编辑器分析

Kronos 有不少能力来自编译期生成信息：`KPojo` 会生成字段元数据和动态访问器，查询 DSL 会生成每个 lambda 可访问的字段，`select { ... }` 会生成查询结果行的临时 `KPojo` 投影类，子查询和 insert-select 还会根据字段形状做诊断。

IntelliJ IDEA 插件会把这些编译期信息交给 IDEA K2 分析。这样，编辑器可以在 `toDataMap()`、`it.nameLength`、`it.rn`、`rows.first().totalAmount`、`insert<Target> { ... }` 这些位置给出补全、类型展示和错误提示。

在生成投影 receiver 上打开 quick documentation 时，插件会渲染对应的生成类形态。你可以直接看到当前结果行有哪些字段，不需要去找内部生成源码。

```kotlin group="Editor Analysis" name="projection" icon="kotlin"
val rows = User()
    .select {
        [it.id, f.length(it.name).alias("nameLength")]
    }
    .toList()

rows.first().nameLength
```

结果类型的 quick documentation 等价于：

```kotlin group="Editor Analysis" name="generated shape" icon="kotlin"
data class KronosSelectResult_UserNameLength(
    var id: Int? = null,
    var nameLength: Int? = null,
) : KPojo
```

如果 Gradle 输出中能正常编译，但编辑器没有补全，请重新加载项目，并确认已安装的插件名为 `Kronos-ORM`、插件 ID 为 `com.kotlinorm.kronos-idea-plugin`。

## 一个查询里的三个 KPojo

下面的例子只需要先理解三个 `KPojo` 形状：一个是用户自己写的表实体，另外两个由 Kronos 在编译期生成。

```kotlin name="three-kpojo-shapes" icon="kotlin"
// 1. 用户自己定义的表 KPojo，也是 User() 查询的输入字段来源
@Table("tb_user")
data class User(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var name: String? = null,
    var status: Int? = null,
) : KPojo

// 2. Kronos 编译期生成：toList() 返回的每一行
// 这里用易读名称展示，业务代码不需要引用真实类名
data class UserNameLengthRow(
    var id: Int? = null,
    var nameLength: Int? = null,
) : KPojo

// 3. Kronos 编译期生成：同一个查询里 orderBy { ... } 能看到的字段
// 这里用易读名称展示，业务代码不需要引用真实类名
data class UserNameLengthOrderContext(
    var id: Int? = null,
    var name: String? = null,
    var status: Int? = null,
    var nameLength: Int? = null,
) : KPojo
```

对应的查询代码如下。看每个 lambda 里的注释即可知道 IDEA 会把 `it` 当成什么类型：

```kotlin name="receiver-in-simple-query" icon="kotlin"
val query = User()
    .select {
        // it: User
        // 这里读取用户表字段，并生成 UserNameLengthRow(id, nameLength)
        [it.id, f.length(it.name).alias("nameLength")]
    }
    .where {
        // it: User
        // 过滤条件读取输入字段，因此这里可以用 id/name/status
        it.status == 1
    }
    .orderBy {
        // it: UserNameLengthOrderContext
        // 排序可以读取输入字段，也可以读取当前 select 生成的 alias
        it.nameLength.desc()
    }

val rows = query.toList()
// rows: List<UserNameLengthRow>

rows.first().id
rows.first().nameLength
```

> **Note**
> IDEA 在解析代码时可能会显示生成投影名称。这些名称只用于编辑器分析，业务代码应使用 DSL 返回值和 alias。

同一个查询的 `where { ... }` 读取输入字段。`nameLength` 是当前 `select` 生成的结果字段，IDEA 会在下面的写法里标红：

```kotlin name="invalid-current-alias-in-where" icon="kotlin"
User()
    .select {
        // it: User
        [it.id, f.length(it.name).alias("nameLength")]
    }
    .where {
        // it: User
        // User 里没有 nameLength
        it.nameLength > 8
    }
    .toList()
```

## 查询结果继续查询

如果把 `query` 继续作为查询输入，新的 lambda 会读取 `UserNameLengthRow`。这时 `nameLength` 已经是输入字段，所以可以用于 `where { ... }`。

```kotlin name="select-from-projection" icon="kotlin"
val query = User()
    .select {
        // it: User
        [it.id, f.length(it.name).alias("nameLength")]
    }

val filtered = query
    .select {
        // it: UserNameLengthRow
        // Kronos 会为这个 select 再生成一个新的结果行类
        [it.id, it.nameLength]
    }
    .where {
        // it: UserNameLengthRow
        it.nameLength > 8
    }
    .toList()
```

有子查询、多次 `.select { ... }`、join 子查询或 insert-select 时，也会继续产生更多类似的结果行类和排序上下文类。业务代码不需要引用这些类名，IDEA 插件会把它们用于补全和检查。

## 窗口函数结果

窗口函数 alias 会进入结果行类，也会进入同一个查询的排序上下文类。先看实际写法和每个 lambda 的 receiver：

```kotlin name="window-query-receiver" icon="kotlin"
@Table("tb_order")
data class Order(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var userId: Int? = null,
    var amount: BigDecimal? = null,
    var status: Int? = null,
    var createTime: LocalDateTime? = null,
) : KPojo

val ranked = Order()
    .select {
        // it: Order
        [
            it.id,
            it.userId,
            it.amount,
            f.rowNumber()
                .over {
                    // 这里继续引用外层 it: Order 的字段组织窗口表达式
                    partitionBy(it.userId)
                    orderBy(it.createTime.desc())
                }
                .alias("rn")
        ]
    }
    .orderBy {
        // it: RankedOrderContext
        it.rn.asc()
    }

val firstOrders = ranked
    .select {
        // it: RankedOrderRow
        [it.id, it.userId, it.amount]
    }
    .where {
        // it: RankedOrderRow
        it.rn == 1
    }
    .toList()
```

如果直接在 `ranked` 的同一个查询里写 `where { it.rn == 1 }`，IDEA 会把 `rn` 标红，因为这个 `where` 的 `it` 仍然是 `Order`。

上面的代码会生成类似这两个类：

```kotlin name="window-result-shapes" icon="kotlin"
// Kronos 编译期生成：ranked.toList() 的结果行
data class RankedOrderRow(
    var id: Int? = null,
    var userId: Int? = null,
    var amount: BigDecimal? = null,
    var rn: Long? = null,
) : KPojo

// Kronos 编译期生成：同一个查询的 orderBy { ... } 上下文
data class RankedOrderContext(
    var id: Int? = null,
    var userId: Int? = null,
    var amount: BigDecimal? = null,
    var status: Int? = null,
    var createTime: LocalDateTime? = null,
    var rn: Long? = null,
) : KPojo
```

## join 中的 receiver

`KSelectable` 可以作为 join 右侧数据源。右侧参数会按右侧查询生成的结果行提供补全。

```kotlin name="join-selectable-source" icon="kotlin"
val orderTotals = Order()
    .select {
        // it: Order
        [it.userId, f.sum(it.amount).alias("totalAmount")]
    }
    .groupBy {
        // it: Order
        it.userId
    }

User()
    .join(orderTotals) { user, totals ->
        // user: User
        // totals: OrderTotalRow，由 orderTotals 自动生成
        leftJoin { user.id == totals.userId }
            .select { [user.id, user.name, totals.totalAmount] }
    }
    .toList()
```

右侧结果行可以理解为：

```kotlin name="join-right-result-shape" icon="kotlin"
data class OrderTotalRow(
    var userId: Int? = null,
    var totalAmount: BigDecimal? = null,
) : KPojo
```

## insert-select 中的 receiver

`insert<Target> { ... }` 的 lambda 读取源查询生成的结果行。IDEA 会基于这个结果行提示可用字段，并检查 values 数量和类型。

```kotlin name="insert-select-target" icon="kotlin"
data class UserOrderSummary(
    var userId: Int? = null,
    var orderCount: Long? = null,
) : KPojo
```

```kotlin name="insert-select-source" icon="kotlin"
val orderCounts = Order()
    .select {
        // it: Order
        [it.userId, f.count(it.id).alias("orderCount")]
    }
    .groupBy {
        // it: Order
        it.userId
    }

orderCounts
    .insert<UserOrderSummary> {
        // it: OrderCountRow，由 orderCounts 自动生成
        [it.userId, it.orderCount]
    }
    .execute()
```

源结果行可以理解为：

```kotlin name="insert-select-source-shape" icon="kotlin"
data class OrderCountRow(
    var userId: Int? = null,
    var orderCount: Long? = null,
) : KPojo
```

下面这些写法会在编辑器中提示：

```kotlin name="insert-select-invalid-count" icon="kotlin"
orderCounts
    .insert<UserOrderSummary> {
        // UserOrderSummary 需要 userId 和 orderCount 两个值
        [it.userId]
    }
```

```kotlin name="insert-select-invalid-type" icon="kotlin"
orderCounts
    .insert<UserOrderSummary> {
        // userId 需要 Int?，orderCount 需要 Long?
        [it.orderCount, it.userId]
    }
```

## 标量子查询生成的字段

标量子查询作为 select item 时，也会变成结果行类上的一个属性。

```kotlin name="scalar-select-item" icon="kotlin"
val users = User()
    .select { u ->
        // u: User
        [
            u.id,
            Order()
                .select {
                    // it: Order
                    it.amount
                }
                .where {
                    // it: Order，可以和外层 u: User 比较
                    it.userId == u.id
                }
                .orderBy {
                    // it: Order
                    it.createTime.desc()
                }
                .limit(1)
                .alias("lastAmount")
        ]
    }

val rows = users.toList()
// rows 的元素包含 id 和 lastAmount
```

这会生成类似：

```kotlin name="scalar-result-shape" icon="kotlin"
data class UserLastAmountRow(
    var id: Int? = null,
    var lastAmount: BigDecimal? = null,
) : KPojo
```

下面这些写法会报错，本质原因都是无法生成明确的单个属性：

```kotlin name="scalar-invalid-examples" icon="kotlin"
// 缺少属性名，无法生成结果类字段
User().select { u ->
    [u.id, Order().select { it.amount }.where { it.userId == u.id }.limit(1)]
}

// 没有 limit(1)，不能保证是单值
User().select { u ->
    [
        u.id,
        Order()
            .select { it.amount }
            .where { it.userId == u.id }
            .alias("lastAmount")
    ]
}

// 返回多列，不能放进一个属性
User().select { u ->
    [
        u.id,
        Order()
            .select { [it.amount, it.status] }
            .where { it.userId == u.id }
            .limit(1)
            .alias("lastOrder")
    ]
}
```

## 谓词子查询报错

插件也会提示 `IN`、`EXISTS`、`ANY`、`SOME`、`ALL` 和 row-value tuple 的形态问题。下面这个写法是合法的：

```kotlin name="predicate-valid" icon="kotlin"
User()
    .select()
    .where { user ->
        // user: User
        user.id in Order().select {
            // it: Order
            it.userId
        } && exists(
            Order().select().where {
                // it: Order，可以引用外层 user: User
                it.userId == user.id
            }
        )
    }
```

下面这些写法会在编辑器中报错：

```kotlin name="predicate-invalid-examples" icon="kotlin"
// 单值 IN 右侧只能返回一列
User().select().where {
    it.id in Order().select { [it.userId, it.status] }
}

// row-value tuple 左右列数必须一致
User().select().where {
    [it.id, it.status] in Order().select { it.userId }
}

// 单元素 tuple 不合法，应该写 it.id in query
User().select().where {
    [it.id] in Order().select { it.userId }
}

// any/some/all 右侧也必须是单列查询
Order().select().where {
    it.status > any(Order().select { [it.status, it.amount] })
}
```

## alias 为什么必须写

直接字段有天然字段名：

```kotlin name="direct-field-name" icon="kotlin"
User().select { [it.id, it.name] }
```

可以生成：

```kotlin name="direct-field-result-shape" icon="kotlin"
data class UserIdNameRow(
    var id: Int? = null,
    var name: String? = null,
) : KPojo
```

函数和表达式没有天然属性名：

```kotlin name="missing-alias" icon="kotlin"
User().select { [it.id, f.length(it.name)] }
```

Kronos 无法判断第二个字段应该生成成 `length`、`nameLength` 还是其他名字，所以 IDEA 会提示你写 alias：

```kotlin name="valid-alias" icon="kotlin"
User().select { [it.id, f.length(it.name).alias("nameLength")] }
```

重复请求输出名需要使用 Kotlin 标准 `UnsafeProjectionOverride` opt-in。没有 opt-in 时，IDEA 会在冲突投影项上报错：

```kotlin name="duplicate-projection-field" icon="kotlin"
User().select { [it.id, it.id] }
```

opt-in 后，Kronos 会保留全部值。第一次出现保留原名，后续值使用 `_1`、`_2` 等后缀；分配后缀前会先保留全部显式请求名。

```kotlin name="opted-in-duplicate-projection" icon="kotlin"
import com.kotlinorm.annotations.UnsafeProjectionOverride

@OptIn(UnsafeProjectionOverride::class)
val rows = User()
    .select { [it.id, it.id, it.name.alias("id_1")] }
    .toList()

rows.first().id
rows.first().id_2
rows.first().id_1
```

请求名 `id`、`id`、`id_1` 会解析为 `id`、`id_2`、`id_1`。IDEA 补全和生成投影文档都使用这些解析后名称。不同值具有不同含义时，优先使用显式 alias。

alias 可以复用输入字段名。alias 本身以及 `where`、`groupBy`、`having` 等 Source 子句不需要 opt-in：

```kotlin name="source-conflict" icon="kotlin"
User()
    .select { [it.id, f.length(it.name).alias("name")] }
    .where { it.name != null }
```

`orderBy` 读取 select 后的 Context。只有它实际读取被遮蔽的 `name` 时，IDEA 才要求 `UnsafeProjectionOverride`；opt-in 后，该 Context 名称解析为 Selected 值及其 Kotlin 类型。

```kotlin name="opted-in-context-shadow" icon="kotlin"
@OptIn(UnsafeProjectionOverride::class)
val ordered = User()
    .select { [it.id, f.length(it.name).alias("name")] }
    .orderBy { it.name.desc() }
```

如果本意就是替换字段，可以先移除 Source 字段再恢复名称。`select { [it - it.name, f.length(it.name).alias("name")] }` 不需要 opt-in。完整分配和 Context 规则见 {{ $.keyword("query/projection", ["投影"]) }}。

## receiver 速查

下面的表格只是对上面例子的补充：

| 写法 | lambda 里的 `it` / 参数 |
|------|------------------------|
| `User().select { ... }` | `User` |
| `User().where { ... }` | `User` |
| `User().groupBy { ... }` | `User` |
| `User().having { ... }` | `User` |
| `User().select { ... }.orderBy { ... }` | 原始输入字段 + 当前 select alias 组成的排序上下文类 |
| `query.select { ... }` | `query` 上一次 select 生成的结果行类 |
| `query.where { ... }` | `query` 当前输入的结果行类 |
| `User().join(query) { left, right -> ... }` | `left` 是 `User`，`right` 是 `query` 的结果行类 |
| `query.insert<Target> { ... }` | `query` 的结果行类 |
| 标量子查询内部的 `where { ... }` | 子查询表实体；可以引用外层 lambda 参数 |

## 支持的诊断场景

IDEA 插件会把这些 Kronos DSL 规则直接显示在编辑器中：

| 场景 | 为什么会报错 |
|------|--------------|
| 非直接字段投影缺少 `.alias("name")` | 结果类字段没有名字 |
| 重复请求输出名但没有 `UnsafeProjectionOverride` | 只有显式 opt-in 后才会保留全部值并分配确定性后缀 |
| `orderBy` 未 opt-in 就读取被 Selected 值遮蔽的 Source 名称 | opt-in 后，select 后 Context 会把该名称解析为 Selected 值和类型 |
| 同一个查询的 `where` / `having` 访问当前 select alias | 过滤条件读取输入字段 |
| 标量子查询返回多列 | 一个属性只能承接一个值 |
| 标量子查询缺少 `.limit(1)` | 不能保证只返回一个值 |
| `field in query` 右侧返回多列 | 单值谓词左右列数不一致 |
| `[a, b] in query` 左右列数不一致 | row-value tuple 形状不一致 |
| `[a] in query` | 单列成员判断应写 `a in query` |
| insert-select values 数量或类型不匹配 | 源结果类字段无法映射到目标表可插入字段 |

## 排查编辑器行为

当 Gradle 或 Maven 可以通过，但 IDEA 没有显示 Kronos 补全时，按下面顺序检查。

| 现象 | 检查点 | 修复方式 |
|------|--------|----------|
| `toDataMap()` 或 `__tableName` 表现为默认 `KPojo` 方法 | source set 没有经过 Kronos 编译期支持 | 启用 {{ $.keyword("configuration/compiler-plugins", ["编译器插件"]) }} 后重新导入项目 |
| `nameLength` 这类投影 alias 没有补全 | 项目导入完成前已安装插件，IDE 模型较旧 | 重新加载 Gradle/Maven，并重新打开 Kotlin 文件 |
| 构建输出有诊断，但编辑器没有提示 | IDEA 使用的 Kotlin 插件或项目模型不匹配 | 更新到支持 Kotlin 2.4.0 的 IDEA/Kotlin 插件，必要时清理缓存 |
| Codegen UI 找不到数据源配置 | 配置路径没有指向目标 JSON 文件 | 在 `Kronos ORM Setting` 中设置 `Config File` |

更多查询、子查询和 INSERT SELECT 规则见 {{ $.keyword("query/subqueries", ["子查询"]) }}。安装与运行问题见 {{ $.keyword("resources/troubleshooting", ["故障排查"]) }}。
