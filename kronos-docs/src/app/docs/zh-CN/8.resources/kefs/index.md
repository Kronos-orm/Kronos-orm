{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

在 IntelliJ IDEA 中安装由 [Mr3zee](https://github.com/Mr3zee) 开发的 **Kotlin External FIR Support（KEFS）**，加载 Kronos 编译器支持。Kotlin 编辑器分析会识别生成的投影成员，提供属性补全、类型推断和 Kronos DSL 诊断。

## 配置 Gradle 项目

在使用 Kronos 的模块中应用 Gradle 插件并添加运行时依赖：

```kotlin name="build.gradle.kts" icon="gradlekts"
plugins {
    kotlin("jvm") version "2.4.21"
    id("com.kotlinorm.kronos-gradle-plugin") version "{{ $.kronosVersion() }}"
}

repositories { mavenCentral() }

dependencies {
    implementation("com.kotlinorm:kronos-core:{{ $.kronosVersion() }}")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcollection-literals")
    }
}
```

重新加载 Gradle 项目，让 IDEA 导入编译器插件及集合字面量参数。Gradle 项目的编辑器接入由 KEFS 完成；Kotlin Toolchain 项目使用对应的构建模型导入插件。

## 版本兼容表

按下表选择 Kronos ORM、KEFS 和 IntelliJ IDEA 的版本组合。`IDE Kotlin` 是 IDEA 内置编译器的版本，KEFS 用它选择 Kronos 编辑器制品；项目自身的 Kotlin 版本由构建配置管理。

{{ $.kefsCompatibilityTable() }}

可在 IDEA 的 **Find Action** 中运行 `KEFS: Copy Kotlin IDE Version` 查看当前内置编译器版本。Kronos 编辑器制品使用 `<IDE Kotlin>-<Kronos ORM>` 版本形式，例如 `2.4.20-ij262-52-{{ $.kronosVersion() }}`。升级时一起核对表中的组合，并重新加载项目。

## 安装 KEFS

选择兼容表中的版本组合，从 IDEA 插件商店安装对应版本的 [Kotlin External FIR Support](https://plugins.jetbrains.com/plugin/26480-kotlin-external-fir-support)：

```text name="IDEA 设置"
Settings / Preferences -> Plugins -> Marketplace
搜索：Kotlin External FIR Support
Install -> Restart IDE
```

Kotlin Toolchain 项目还需安装 [Kotlin Toolchain 插件](https://kotlin-toolchain.org/latest/getting-started/ide-setup/)，然后打开包含 `project.yaml` 的目录。

## 在 KEFS 中配置 Kronos

打开 `Settings / Preferences -> Tools -> Kotlin External FIR Support -> Artifacts`。

1. 在 **Maven Repositories** 中选择 Maven Central，或添加地址 `https://repo.maven.apache.org/maven2`。
2. 在 **Kotlin Compiler Plugins** 中添加名为 `Kronos` 的插件组。
3. 在 **Coordinates** 中填写 `com.kotlinorm:kronos-compiler-plugin`，不带版本号。
4. 版本匹配选择 **Exact**，关联 Maven Central，并启用当前项目中的插件组。
5. 应用设置，再通过项目使用的构建工具重新加载项目。

KEFS 将项目配置保存到 `.idea/kotlin-plugins.xml`。将此文件提交到版本控制，让其他开发者使用相同配置。

项目构建负责配置 Kronos 编译器依赖，KEFS 会选择匹配的编辑器制品。构建配置见 {{ $.keyword("configuration/compiler-plugins", ["编译器插件"]) }}。

## 确认编译器插件已加载

打开使用 Kronos 查询的 Kotlin 文件，再打开 **KEFS Diagnostics** 工具窗口。Kronos 的状态应为 **Successfully Loaded**，详情中可以查看已加载的 JAR 和版本。

将工具窗口显示的版本与本页兼容表中的 `IDE Kotlin`、Kronos ORM 版本对应起来，再检查加载的制品坐标。

通过 **Find Action**（macOS 为 `⌘⇧A`）可以运行：

- `KEFS: Update Plugins`：检查制品更新。
- `KEFS: Refresh Plugins`：重新加载编译器插件。
- `KEFS: Copy Kotlin IDE Version`：查看 IDEA 内置编译器的准确版本。

本地开发时，在 **Maven Repositories** 添加 **Local (File path)** 仓库，选择编译器兼容制品的 Maven 仓库目录，并在 Kronos 插件组中关联该仓库。`Plugin is Not Requested` 表示尚未触发分析；`Not Found` 时检查关联仓库、构建版本及对应的兼容制品。

## 开发 Kronos 源码

Kronos 仓库包含共享的 [`.idea/kotlin-plugins.xml`](https://github.com/Kronos-orm/Kronos-orm/blob/main/.idea/kotlin-plugins.xml)，已配置 Kronos 插件组及 bootstrap、Maven Central 和 Sonatype snapshot 仓库。

安装官方 Kotlin Toolchain IDE 插件和 KEFS，再准备从源码构建的编译器：

```bash name="仓库 bootstrap" icon="terminal"
scripts/run build bootstrap
```

Toolchain IDE 插件负责导入项目模型，KEFS 负责为编辑器分析加载编译扩展。Bootstrap 会将普通编译器坐标准备到 Maven Local 供项目导入使用，将 KEFS 制品放在 `build/repository/bootstrap`。打开包含 `project.yaml` 的仓库目录并重新加载项目；共享 KEFS 配置使用 `$PROJECT_DIR$` 引用私有仓库。

## 启用集合字面量语法

在项目构建中启用 `-Xcollection-literals`，让编译器和导入后的 IDE 模型识别 `[]` DSL。

```kotlin name="build.gradle.kts" icon="gradlekts"
kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcollection-literals")
    }
}
```

```yaml name="module.yaml"
settings:
  kotlin:
    version: 2.4.21
    freeCompilerArgs: [ -Xcollection-literals ]
```

修改编译参数后重新加载项目。KEFS 的 **General** 页会显示编辑器使用的编译器版本，可与本页兼容表中的 `IDE Kotlin` 对照。

## 使用编辑器分析

编写投影后，通过 Kotlin 标准属性补全访问结果字段：

```kotlin name="projection" icon="kotlin"
val rows = User()
    .select { [it.id, f.length(it.name).alias("nameLength")] }
    .toList()

val length: Int? = rows.first().nameLength
```

`nameLength` 推断为 `Int?`。这些类型也会用于后续查询 lambda 的属性补全和类型检查。

数据库实体生成见 {{ $.keyword("resources/codegen", ["代码生成器"]) }}。

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

有子查询、多次 `.select { ... }`、join 子查询或 insert-select 时，也会继续产生更多类似的结果行类和排序上下文类。业务代码不需要引用这些类名，Kotlin 编辑器分析会把它们用于补全和检查。

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

请求名 `id`、`id`、`id_1` 会解析为 `id`、`id_2`、`id_1`。IDEA 补全和类型推断都使用这些解析后名称。不同值具有不同含义时，优先使用显式 alias。

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

Kronos 编译器诊断会通过 KEFS 显示在 Kotlin 编辑器分析中：

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

项目可以构建，但 IDEA 没有显示 Kronos 补全时，按下面顺序检查。

| 现象 | 检查点 | 修复方式 |
|------|--------|----------|
| `toDataMap()` 或 `__tableName` 表现为默认 `KPojo` 方法 | source set 没有经过 Kronos 编译期支持 | 启用 {{ $.keyword("configuration/compiler-plugins", ["编译器插件"]) }} 后重新导入项目 |
| 投影 alias（如 `nameLength`）缺失 | 检查 KEFS Artifacts 中的 Kronos 插件组 | 启用插件组，选择 Maven Central，再重新加载项目 |
| 构建输出有诊断，编辑器没有提示 | 查看 KEFS 工具窗口中的 Kronos 制品状态 | 更新 KEFS 制品并重新加载项目 |
| `[]` 出现语法错误 | 检查导入后的编译参数 | 在构建中启用 `-Xcollection-literals` 并重新加载项目 |

更多查询、子查询和 INSERT SELECT 规则见 {{ $.keyword("query/subqueries", ["子查询"]) }}。安装与运行问题见 {{ $.keyword("resources/troubleshooting", ["故障排查"]) }}。
