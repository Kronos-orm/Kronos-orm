# Kronos FIR 投影类型生成蓝图

本文档描述 Kronos 自动投影类型生成机制的整体蓝图。它不按实施阶段展开，而是说明最终系统应如何工作、各组件如何协作、数据如何在 FIR、IR 和 runtime 之间流动。

## 一句话目标

把 DSL 中的字段列表表达式：

```kotlin
user.select { [it.id, it.username] }
```

解释为一个编译期可见的局部 KPojo 投影类型，并把 `select` 的返回类型从：

```kotlin
SelectClause<User, User>
```

细化为：

```kotlin
SelectClause<User, KronosProjection_xxx>
```

从而让默认查询 API 得到静态类型：

```kotlin
val row = user.select { [it.id, it.username] }.queryOneOrNull()
row?.username
```

其中 `row` 是生成投影类型的可空值，`it` 仍然是完整源 DTO `User`。

## 核心抽象

整个功能由四个抽象组成：

```text
字段投影表达式
  -> 投影模型
  -> FIR 局部投影类
  -> DSL 返回类型细化
  -> IR/KPojo 运行时映射
```

### 字段投影表达式

字段投影表达式是用户在 DSL lambda 中写出的字段集合：

```kotlin
[it.id, it.username]
listOf(it.id, it.username)
it.username
```

它不是普通业务集合，也不是运行时 tuple。它在 Kronos DSL 上下文中表示“我要从源 DTO 中投影这些字段”。

### 投影模型

投影模型是 FIR 层对字段列表的结构化描述：

```text
source = User
fields =
  id: Int?
  username: String?
name = KronosProjection_xxx
```

投影模型不关心调用来自 `select`、`forSelect`、`join` 还是 `union`。它只描述字段结构。

### FIR 局部投影类

投影类是由 FIR 插件生成的局部 KPojo 类型：

```kotlin
class KronosProjection_xxx(
    var id: Int? = null,
    var username: String? = null,
) : KPojo
```

它必须在前端类型解析阶段可见，因此不能只在 IR 后端生成。

### DSL 返回类型细化

DSL adapter 决定投影模型如何影响返回类型。对 `select` 而言：

```text
SelectClause<T, T> -> SelectClause<T, Projection>
```

对 `forSelect`、`forReference` 等 DSL，可能只需要字段模型，不一定立即生成返回 DTO。

## 编译器层职责

### FIR 负责什么

FIR 负责所有“源代码必须看得见”的东西：

- 识别 DSL 调用。
- 读取 lambda 返回的字段投影表达式。
- 生成局部投影类 symbol。
- 把 `select` 调用返回类型细化成包含投影类的类型。
- 把局部投影类声明插入 FIR 树。
- 对不支持的投影形式给出编译期诊断。

FIR 不能依赖 IR 后续再补类型。只要用户源码需要 `row.username` 能编译，投影类就必须在 FIR 完成。

### IR 负责什么

IR 负责运行时行为：

- 对生成的投影 KPojo 类补齐 `get/set`、`kronosColumns()`、`kClass()` 等方法体。
- 继续把 select lambda 中的字段表达式转换成 `Field` 列表。
- 让 `KClassMapGenerator` 或等价机制知道如何构造投影类实例。
- 保证最终字节码不使用反射路径。

IR 不负责修改已经确定的用户可见静态类型。

### kronos-core 负责什么

`kronos-core` 提供运行时 API 和执行模型：

- `SelectClause<T, R>` 持有源 DTO 类型 `T` 和投影 DTO 类型 `R`。
- 默认 `queryList()` 返回 `List<R>`。
- 默认 `queryOne()` 返回 `R`。
- 默认 `queryOneOrNull()` 返回 `R?`。
- 投影路径把 `projectionClass: KClass<R>` 传入 wrapper 做结果映射。

为了降低 FIR 改写复杂度，可以提供一个内部 API：

```kotlin
@PublishedApi
internal fun <T : KPojo, R : KPojo> T.selectGeneratedProjection(
    projectionClass: KClass<R>,
    fields: ToSelect<T, Any?>,
): SelectClause<T, R>
```

插件可以把用户源码改写到这个 API，而不是重新参与普通 `select` 重载推断。

## 总体数据流

用户代码：

```kotlin
val row = user.select { [it.id, it.username] }.queryOneOrNull()
```

FIR 处理流程：

```text
1. call resolver 解析到普通 select 函数
2. KronosProjectionCallRefinementExtension.intercept 捕获 select 调用
3. SelectProjectionAdapter 提取 receiver = User、lambda = { [it.id, it.username] }
4. KronosProjectionAnalyzer 读取字段列表
5. KronosProjectionModel 生成 source、fields、projection name
6. KronosProjectionFirClassGenerator 创建局部 class symbol
7. KronosProjectionFirTypeBuilder 构造 SelectClause<User, Projection> 类型
8. intercept 返回 CallReturnType
9. transform 把局部 class 和改写后的 select call 写回 FIR
10. 外层 queryOneOrNull 根据 SelectClause<User, Projection> 推断出 Projection?
```

IR 处理流程：

```text
1. FIR-to-IR 后，局部 Projection class 进入 IR module
2. KronosParserTransformer 识别它实现 KPojo
3. KronosIrClassTransformer 给它生成 KPojo 方法体
4. SelectTransformer 继续把 [it.id, it.username] 转成 Field 列表
5. KClassMapGenerator 收集 Projection::class -> { Projection() }
6. 运行时 queryOneOrNull 使用 Projection::class 映射结果
```

运行时流程：

```text
1. SelectClause.build 生成 SQL
2. SQL select item 使用属性名作为 label
3. JDBC wrapper 获取 ResultSet label/value
4. wrapper 通过 kClassCreator 创建 Projection 实例
5. wrapper 通过 KPojo set(name, value) 写入属性
6. queryOneOrNull 返回 Projection?
```

## 组件图

```text
KronosFirExtensionRegistrar
  |
  +-- KronosProjectionSessionComponent
  |     |
  |     +-- 保存 projection model
  |     +-- 保存 refined call symbol -> projection symbol
  |     +-- 判断 ownsSymbol / restoreSymbol
  |
  +-- KronosProjectionCallRefinementExtension
        |
        +-- KronosProjectionDslAdapterRegistry
              |
              +-- SelectProjectionAdapter
              +-- ForSelectProjectionAdapter
              +-- ForReferenceProjectionAdapter
              +-- JoinProjectionAdapter
              +-- UnionProjectionAdapter
        |
        +-- KronosProjectionAnalyzer
        |     |
        |     +-- KronosProjectionExpressionReader
        |     +-- KronosProjectionNameGenerator
        |
        +-- KronosProjectionFirClassGenerator
        +-- KronosProjectionFirTypeBuilder
```

## DSL adapter 模型

每个 DSL adapter 都实现同一组职责：

```kotlin
interface KronosProjectionDslAdapter {
    val kind: KronosProjectionDslKind

    fun matches(symbol: FirNamedFunctionSymbol, callInfo: CallInfo): Boolean

    fun extractContext(
        symbol: FirNamedFunctionSymbol,
        callInfo: CallInfo,
    ): KronosProjectionContext?

    fun refineReturnType(
        originalReturnType: ConeKotlinType,
        sourceType: ConeKotlinType,
        projectionType: ConeKotlinType,
    ): FirResolvedTypeRef

    fun rewriteCall(
        call: FirFunctionCall,
        projectionSymbol: FirRegularClassSymbol,
        model: KronosProjectionModel,
    ): FirFunctionCall
}
```

这样通用分析器不需要知道 `select` 的返回类型结构，也不需要知道 `forReference` 是否真的要生成 DTO。adapter 决定“字段模型如何应用到某个 DSL”。

## select 的最终形态

输入源码：

```kotlin
user.select { [it.id, it.username] }
```

FIR 语义：

```kotlin
run {
    class KronosProjection_xxx(
        var id: Int? = null,
        var username: String? = null,
    ) : KPojo

    user.selectGeneratedProjection(
        KronosProjection_xxx::class
    ) { [it.id, it.username] }
}
```

静态类型：

```kotlin
SelectClause<User, KronosProjection_xxx>
```

调用链：

```kotlin
user.select { [it.id, it.username] }
    .where { it.age > 18 }
    .queryList()
```

类型语义：

- `select` lambda 中 `it` 是 `User`。
- `where` lambda 中 `it` 仍是 `User`。
- `queryList()` 返回 `List<KronosProjection_xxx>`。

这点非常关键：链式条件、排序、分组仍围绕源表 DTO 工作，只有最终结果类型是投影 DTO。

## forSelect 与 forReference 的定位

`forSelect` 和 `forReference` 应复用字段投影模型，但不必默认生成结果 DTO。

### forSelect

`forSelect` 常见用途是构造可复用字段选择片段、子查询字段、联表选择项。它需要：

- 读取 `[]` 字段列表。
- 生成统一字段模型。
- 在被外层 select/join/subquery 消费时决定是否生成投影类型。

### forReference

`forReference` 更多是字段引用 DSL，例如 cascade 或关联字段声明。它需要：

- 复用 `[]` / `listOf` / 单字段解析能力。
- 保留属性引用信息。
- 默认不生成 DTO，除非外层 DSL 明确需要 typed projection。

## join、group、union 的扩展规则

### join

join 投影可能来自多个源 DTO：

```kotlin
select { [user.id, dept.name] }
```

蓝图要求：

- projection field 记录来源表/来源 DTO。
- 重名字段必须 alias。
- SQL label 必须等于生成属性名。
- 生成类型属性顺序等于 select 列顺序。

### group / aggregate

聚合字段必须有稳定属性名：

```kotlin
select { [it.departmentId, f.count(it.id) as_ "total"] }
```

规则：

- 普通分组字段可直接生成属性。
- 函数字段必须 alias。
- raw SQL 字段必须 alias。

### union

union 要求多个分支投影结构兼容：

```text
字段数量一致
字段顺序一致
字段类型可兼容
属性名一致或可映射
```

不兼容时应给 FIR diagnostic，不要拖到运行时报 SQL 或映射错误。

## 生成类命名

生成类名应稳定、可调试、避免冲突：

```text
KronosProjection_<hash>
```

hash 输入：

- 文件路径或 source id。
- 调用点 offset。
- DSL kind。
- 源 DTO class id。
- 字段名、alias、类型签名。

同一个作用域内冲突时追加序号：

```text
KronosProjection_7ad31c_2
```

生成类是局部实现细节，不承诺成为公开 API 名称。

## SQL label 策略

自动投影必须保证 ResultSet label 能写入生成属性：

```kotlin
@Column("user_name")
var username: String?
```

投影字段：

```kotlin
[it.username]
```

SQL 应类似：

```sql
select user_name as username
```

而不是只返回 `user_name`。否则 wrapper 会按 `user_name` 调用 `set("user_name", value)`，而生成投影类属性叫 `username`，映射会失败。

策略：

- 自动投影路径强制所有普通字段 alias 为 projection property name。
- 手写 DTO 路径保持兼容，但建议也逐步统一 alias 行为。
- 函数和 raw SQL 字段没有 alias 时直接诊断失败。

## 诊断原则

投影生成失败时要尽量给编译期诊断，而不是：

- 返回 `Any?`。
- 悄悄退回 `SelectClause<T, T>`。
- 让运行时报无法 set 字段。
- 抛 compiler internal exception。

典型诊断：

```text
KRONOS_UNSUPPORTED_PROJECTION_EXPRESSION
KRONOS_PROJECTION_FIELD_REQUIRES_ALIAS
KRONOS_PROJECTION_DUPLICATE_PROPERTY
KRONOS_PROJECTION_SOURCE_NOT_KPOJO
KRONOS_PROJECTION_UNION_MISMATCH
```

文案要告诉用户如何改：

```text
函数投影字段需要 alias，例如 f.count(it.id) as_ "total"。
投影字段名 id 重复，请使用 alias 区分。
自动投影只能用于 KPojo 源类型。
```

## 与手写 DTO 投影的关系

自动投影和手写 DTO 是并存关系。

自动投影：

```kotlin
val row = user.select { [it.id, it.username] }.queryOneOrNull()
```

适合函数内部临时使用，调用方不需要知道生成类型名。

手写 DTO：

```kotlin
fun getBrief(): UserBrief? {
    return user.select<User, UserBrief> { [it.id, it.username] }.queryOneOrNull()
}
```

适合公开 API、跨模块返回、稳定模型。

插件不应该为了自动投影删除或弱化手写 DTO 路径。

## 测试蓝图

官方 testData 应成为投影能力的主要测试入口：

```text
kronos-compiler-plugin/testData/box/projection/
kronos-compiler-plugin/testData/diagnostics/projection/
```

box 测试覆盖：

- `select { [it.id, it.username] }` 可编译。
- `row?.username` 可编译。
- lambda receiver 仍是源 DTO。
- `where/orderBy/groupBy` 链式调用仍以源 DTO 为 receiver。
- runtime wrapper 能映射生成投影类型。

diagnostics 测试覆盖：

- 函数字段无 alias。
- 字段重名。
- 非 KPojo 源。
- union 投影不兼容。
- 不支持的动态表达式。

包含 collection literal 的测试必须确保启用：

```text
-Xcollection-literals
```

不能假设主工程 Gradle 参数会自动传入官方 compiler test 或动态编译测试。

## 维护边界

`FirFunctionCallRefinementExtension` 属于 Kotlin 不稳定 API。所有直接依赖该 API 的代码必须集中在少数文件：

```text
KronosProjectionCallRefinementExtension
KronosProjectionFirTypeBuilder
KronosProjectionFirClassGenerator
```

Kotlin 版本升级时优先检查这些文件和相关 official testData。

不要在业务分析逻辑中散落 compiler API 细节。`KronosProjectionAnalyzer` 应尽量产出稳定的 Kronos 自有 model，让 DSL adapter 和 runtime 衔接可以长期保持。

## 最终体验

用户可以在内部逻辑中写：

```kotlin
val users = user.select { [it.id, it.username] }
    .where { it.age > 18 }
    .queryList()

users.firstOrNull()?.username
```

也可以在公开 API 中保留明确 DTO：

```kotlin
data class UserBrief(
    var id: Int? = null,
    var username: String? = null,
) : KPojo

fun queryBriefs(): List<UserBrief> {
    return user.select<User, UserBrief> { [it.id, it.username] }.queryList()
}
```

这两种写法共享同一套字段投影语义，区别只在于结果类型是编译器生成的局部类型，还是用户声明的稳定 DTO。
