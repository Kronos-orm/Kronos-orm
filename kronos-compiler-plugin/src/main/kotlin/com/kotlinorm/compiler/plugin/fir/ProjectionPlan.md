# Kronos FIR 投影类型生成实现计划

本文档描述 `select { [it.id, it.username] }` 自动生成投影 KPojo 类型的具体实现方案。目标不是给 `select` 写一段专用逻辑，而是先建立通用的“字段投影模型 -> 生成投影类型 -> 细化 DSL 返回类型”能力，再逐步接入 `forSelect`、`forReference`、子查询、联表、聚合和 `union`。

## 背景与边界

用户期望：

```kotlin
val row = user.select { [it.id, it.username] }.queryOneOrNull()
row?.username
```

编译后应具备类似效果：

```kotlin
run {
    class KronosProjection_7ad31c(
        var id: Int? = null,
        var username: String? = null,
    ) : KPojo

    user.select<User, KronosProjection_7ad31c>(
        KronosProjection_7ad31c::class
    ) { [it.id, it.username] }
}.queryOneOrNull()
```

必须守住的语义：

- `select` lambda 中的 `it` 始终是源 DTO，例如 `User`，不是投影 DTO。
- 投影 DTO 只影响 `SelectClause<T, R>` 的 `R` 和默认 `queryList()`、`queryOne()`、`queryOneOrNull()` 返回类型。
- `[]` 是 Kotlin 2.4 collection literal。Kronos 不能把它改造成通用 `operator of` 协议，只能在 DSL 上下文中识别它并解释成字段投影。
- 自动生成类型先定位为局部类型，适合局部变量继续点属性；跨函数公开返回类型仍使用手写 DTO。
- 不能依赖反射构造。运行时映射继续走 KPojo 增强、`kClassCreator`、`get/set`。

## 目标文件结构

建议在 `kronos-compiler-plugin/src/main/kotlin/com/kotlinorm/compiler/plugin/fir` 下新增：

```text
fir/
  KronosFirExtensionRegistrar.kt
  KronosProjectionCallRefinementExtension.kt
  KronosProjectionSessionComponent.kt
  model/
    KronosProjectionModel.kt
    KronosProjectionField.kt
    KronosProjectionContext.kt
  analyze/
    KronosProjectionAnalyzer.kt
    KronosProjectionExpressionReader.kt
    KronosProjectionNameGenerator.kt
  generate/
    KronosProjectionFirClassGenerator.kt
    KronosProjectionFirTypeBuilder.kt
  diagnostics/
    KronosProjectionErrors.kt
    KronosProjectionCheckers.kt
```

其中 `diagnostics` 可以延后到第 5 阶段，但错误模型要在第 1 阶段预留，不要靠 `null` 或普通编译器异常表达不支持场景。

## 阶段 1：通用投影模型与字段分析

### 目标

建立与具体 DSL 无关的投影模型。这个模型只回答一件事：某个 DSL lambda 的返回表达式能否被解释成“字段投影列表”，如果能，字段是什么、类型是什么、最终属性名是什么。

### 数据模型

```kotlin
data class KronosProjectionModel(
    val sourceType: ConeKotlinType,
    val sourceClassId: ClassId,
    val fields: List<KronosProjectionField>,
    val generatedName: Name,
    val origin: KronosProjectionOrigin,
    val anchorSource: KtSourceElement?,
)

data class KronosProjectionField(
    val propertyName: Name,
    val columnName: String?,
    val kotlinType: ConeKotlinType,
    val nullable: Boolean,
    val sourcePropertySymbol: FirPropertySymbol?,
    val alias: Name?,
    val expressionKind: KronosProjectionExpressionKind,
)

enum class KronosProjectionExpressionKind {
    PROPERTY,
    ALIASED_PROPERTY,
    FUNCTION,
    ALIASED_FUNCTION,
    RAW_SQL,
    CASCADE_REFERENCE
}
```

`propertyName` 是生成投影类属性名。规则：

- `it.username` -> `username`
- `it.username as_ "name"` -> `name`
- `f.count(it.id) as_ "total"` -> `total`
- 无 alias 的函数、表达式、raw SQL 暂不生成投影属性，先诊断失败。

### 分析入口

`KronosProjectionAnalyzer` 对外暴露一个方法：

```kotlin
class KronosProjectionAnalyzer(
    private val session: FirSession,
) {
    fun analyze(context: KronosProjectionContext): KronosProjectionAnalysisResult
}
```

`KronosProjectionContext` 至少包含：

```kotlin
data class KronosProjectionContext(
    val dslKind: KronosProjectionDslKind,
    val callInfo: CallInfo,
    val originalFunctionSymbol: FirNamedFunctionSymbol,
    val sourceType: ConeKotlinType,
    val lambdaExpression: FirAnonymousFunctionExpression,
)
```

`dslKind` 不写死在 model 里，而是由 adapter 传入：

```kotlin
enum class KronosProjectionDslKind {
    SELECT,
    FOR_SELECT,
    FOR_REFERENCE,
    JOIN_SELECT,
    GROUP_SELECT,
    UNION_SELECT
}
```

### 表达式识别规则

第一批只支持：

```kotlin
select { [it.id, it.username] }
select { listOf(it.id, it.username) }
select { it.username }
```

表达式读取逻辑放在 `KronosProjectionExpressionReader`：

```kotlin
fun readReturnedProjectionExpression(lambda: FirAnonymousFunction): FirExpression?
```

需要处理：

- block body 的最后一个表达式。
- expression body。
- `return@select [it.id, it.username]`。
- collection literal 在 FIR 中对应的表达式形态。
- `listOf(...)` 普通调用。
- 单字段 property access。

先不支持：

- `if (...) [it.id] else [it.name]`
- `when`
- 动态拼接列表
- 展开参数
- 局部变量保存字段列表后返回

这些都应该进入诊断：`KRONOS_UNSUPPORTED_PROJECTION_EXPRESSION`。

### 字段解析规则

属性字段：

```kotlin
it.id
```

读取 `FirPropertyAccessExpression` 的 resolved symbol，确认：

- receiver 是 lambda value parameter `it` 或 source receiver。
- property 属于源 DTO 或其可见成员。
- property 类型可转换为生成类属性类型。

别名字段：

```kotlin
it.username as_ "name"
```

短期实现可以先复用现有 IR `FieldAnalysis` 的别名语义作为对照，但 FIR 需要自己读 call：

- callee 是 Kronos alias API。
- 第一个参数或 receiver 是字段表达式。
- alias 是编译期字符串常量。

函数字段：

```kotlin
f.count(it.id) as_ "total"
```

必须有 alias。生成属性类型先按函数返回类型决定；如果无法拿到可靠类型，第一阶段诊断失败，后续再补类型映射表。

### 稳定命名

`KronosProjectionNameGenerator`：

```kotlin
fun generate(modelSeed: KronosProjectionNameSeed): Name
```

hash 输入：

- 源 class id。
- DSL kind。
- 调用点 source offset 或文件路径 + offset。
- 字段属性名、类型 render、alias。

命名格式：

```text
KronosProjection_<8位hex>
```

局部类同一作用域冲突时追加短序号：

```text
KronosProjection_7ad31c_2
```

### 第 1 阶段测试

优先普通 unit test，不急着跑完整 compiler pipeline：

- `select { [it.id, it.username] }` 解析出两个字段。
- `select { it.name }` 解析出一个字段。
- 无 alias 函数字段返回诊断。
- 重名字段返回诊断。

如果 FIR 对象构造过重，可以先写 official box test 验证集成行为，unit test 放到第 2 阶段后补。

## 阶段 2：FIR 调用返回类型细化竖切

### 目标

让第一个端到端类型场景成立：

```kotlin
val row = user.select { [it.id, it.username] }.queryOneOrNull()
row?.username
```

这一步必须使用 FIR，因为 IR 无法改变前端已经确定的静态类型。

### 注册方式

新增：

```kotlin
class KronosFirExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::KronosProjectionSessionComponent
        +::KronosProjectionCallRefinementExtension
    }
}
```

在 `KronosCompilerPluginRegistrar.registerExtensions` 中注册 FIR registrar：

```kotlin
FirExtensionRegistrarAdapter.registerExtension(KronosFirExtensionRegistrar())
```

具体 API 以 Kotlin 2.4.0 当前可编译签名为准；实现时先做一个空 registrar + smoke test，确认插件加载。

### Call refinement 入口

`KronosProjectionCallRefinementExtension : FirFunctionCallRefinementExtension`：

```kotlin
override fun intercept(
    callInfo: CallInfo,
    symbol: FirNamedFunctionSymbol,
): CallReturnType?
```

识别规则：

- `symbol.callableId` 是 `com.kotlinorm.orm.select.select`。
- 接收者类型是 `KPojo` 子类型。
- 当前调用没有显式投影类型参数，或返回类型仍是 `SelectClause<T, T>`。
- 最后一个参数是 `ToSelect<T, Any?>` lambda。

不要拦截：

- `select<User, UserBrief> { ... }` 手写 DTO 路径。
- `select()` 无字段 lambda。
- 非 KPojo receiver。
- `Pair<T, String>.select(...)`，先放到后续阶段。

### 返回类型构造

当前 runtime 类型已调整为：

```kotlin
SelectClause<T : KPojo, R : KPojo>
```

FIR 要构造：

```kotlin
SelectClause<User, KronosProjection_xxx>
```

伪代码：

```kotlin
val projectionSymbol = sessionComponent.createOrGetProjectionSymbol(model)
val projectionType = projectionSymbol.toConeType(nullable = false)
val sourceType = extractSelectSourceType(callInfo)
val selectClauseType = buildSelectClauseType(sourceType, projectionType)

return CallReturnType(selectClauseType.toFirResolvedTypeRef()) { refinedFunctionSymbol ->
    sessionComponent.bind(refinedFunctionSymbol, model, projectionSymbol)
}
```

`KronosProjectionFirTypeBuilder` 负责封装 Cone type 细节，不让 `intercept` 里散落 compiler API。

### transform 责任

`FirFunctionCallRefinementExtension` 要求 `transform(call, originalSymbol)` 把生成的局部声明放进 FIR 树。目标形态：

```kotlin
run {
    class KronosProjection_xxx(...) : KPojo
    originalSelectCall as SelectClause<User, KronosProjection_xxx>
}
```

实现步骤：

1. 从 `call.calleeReference.resolvedSymbol` 找到在 callback 里绑定的 model。
2. 调用 `KronosProjectionFirClassGenerator.generateLocalClass(model)`。
3. 复制或改写原始 `select` call，使其携带投影 class 参数：
   - 如果 runtime API 保留 `select(projectionClass: KClass<R>, fields: ...)`，需要插入 `Projection::class` 参数。
   - 如果选择新增内部 API，例如 `selectGeneratedProjection(...)`，则改写到内部 API。
4. 返回一个 block expression，statements 包含局部 class 和改写后的 call。

### 推荐 runtime API 配合

为了降低 FIR 改写难度，建议在 `kronos-core` 增加内部 API：

```kotlin
@PublishedApi
internal fun <T : KPojo, R : KPojo> T.selectGeneratedProjection(
    projectionClass: KClass<R>,
    fields: ToSelect<T, Any?>,
): SelectClause<T, R>
```

FIR transform 可以把：

```kotlin
user.select { [it.id, it.username] }
```

改为：

```kotlin
user.selectGeneratedProjection(KronosProjection_xxx::class) { [it.id, it.username] }
```

这样不用参与普通 `select` overload resolution 的二次复杂推断。

### session component

`KronosProjectionSessionComponent` 保存本 session 内的数据：

```kotlin
class KronosProjectionSessionComponent(session: FirSession) :
    FirExtensionSessionComponent(session) {

    fun createOrGetProjection(model: KronosProjectionModel): FirRegularClassSymbol
    fun bind(callSymbol: FirNamedFunctionSymbol, model: KronosProjectionModel, projection: FirRegularClassSymbol)
    fun lookup(callSymbol: FirNamedFunctionSymbol): BoundProjection?
    fun owns(symbol: FirRegularClassSymbol): Boolean
    fun restore(call: FirFunctionCall, name: Name): FirRegularClassSymbol?
}
```

不要使用全局 mutable map。FIR/IDE 会多 session、多轮分析。

### 第 2 阶段测试

新增 official box test：

```text
kronos-compiler-plugin/testData/box/projection/selectLocalProjection.kt
```

测试代码：

```kotlin
fun box(): String {
    val user = User()
    val clause = user.select { [it.id, it.username] }
    val row = clause.queryOneOrNull()
    row?.username
    return "OK"
}
```

再加一个编译型测试，证明 receiver 是源 DTO：

```kotlin
user.select {
    val sourceOnly: Int? = it.age
    [it.id, it.username]
}
```

## 阶段 3：生成投影类的 KPojo 能力与运行时映射

### 目标

生成的投影类不仅要在前端可见，还要在运行时能被 Kronos 当作 KPojo 映射。

### FIR 生成类形态

生成 local class：

```kotlin
data class KronosProjection_xxx(
    var id: Int? = null,
    var username: String? = null,
) : KPojo
```

FIR class 至少需要：

- class kind: regular class。
- origin: plugin。
- visibility: local。
- modality: final。
- super type: `com.kotlinorm.interfaces.KPojo`。
- primary constructor。
- `var` properties。
- 默认值 `null` 或类型默认值。

注意：如果直接生成 data class 的 FIR 过重，可以第一版生成普通 class + var properties + no-arg constructor。KPojo 映射只需要可实例化和可 set，不强制 data class。

### IR 增强衔接

现有 `KronosParserTransformer.visitClassNew()` 会识别实现 `KPojo` 的类并调用 `KronosIrClassTransformer`。需要确认：

- FIR 生成的 local class 会进入 FIR-to-IR 并出现在 IR module traversal 中。
- `visitClassNew()` 能看到它的 KPojo supertype。
- `KronosIrClassTransformer` 能处理 plugin origin/local class。

如果现有 transformer 跳过 local/plugin origin，需要放开条件或增加专门路径。

### 构造与 kClassCreator

当前用户强调“不需要反射，参考 queryList 和 kClassCreator”。所以运行时应走：

- `Projection::class` 传入 `SelectClause<T, Projection>`。
- KPojo 增强生成 `kClass()`、`get/set`、`kronosColumns()`。
- `KClassMapGenerator` 收集生成投影类构造 lambda。

需要检查 `KClassMapGenerator` 当前收集逻辑：

- 是否只收集源码显式 KPojo。
- 是否能收集局部类。
- 是否能收集 FIR plugin 生成类。

如果不能，补一个 `ProjectionKClassCreatorCollector` 或扩展现有收集逻辑，确保：

```kotlin
kClassCreator[Projection::class] = { Projection() }
```

### SQL alias 对齐

JDBC wrapper 是按列 label 写入 KPojo 属性的。生成属性名必须和 SQL select alias 对齐：

- 普通字段：`it.username` 生成属性 `username`，SQL alias 若列名不同应 alias 为 `username`。
- `@Column("user_name") var username` 必须 select 出 label `username`，否则 wrapper 拿到 `user_name` 会 set 失败。
- 函数字段必须强制 alias，例如 `count(it.id) as_ "total"`。

若当前 `SelectClause.fieldsToSelectItems()` 只在 `field.name != field.columnName` 时 alias，需要验证投影字段路径下全部字段都能得到属性名 label。必要时给 `SelectClause` 增加 projection mode：

```kotlin
private val projectionResultClass: KClass<R>
private val forceProjectionAliases: Boolean
```

在自动投影时开启 alias。

### 第 3 阶段测试

official box test：

```kotlin
fun box(): String {
    val row = User().select { [it.id, it.username] }.queryOneOrNull()
    return "OK"
}
```

compile-only 测试不够，还要验证生成 KPojo 方法：

```kotlin
val projection = createProjectionSomehow()
projection["username"] = "Ada"
if (projection.username != "Ada") return "Fail"
```

如果局部生成类型不好直接实例化，就通过查询 mock wrapper 返回一行数据，验证 `row?.username`。

## 阶段 4：通用 DSL adapter 接入

### 目标

把阶段 1 的 `KronosProjectionModel` 作为公共能力，接入更多 DSL。每个 adapter 只负责三件事：

1. 判断自己是否处理某个 call。
2. 提取源 DTO 类型和投影 lambda。
3. 告诉 call refinement 哪个返回类型位置要替换为投影类型。

### Adapter 接口

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

第一批 adapter：

- `SelectProjectionAdapter`
- `ForSelectProjectionAdapter`
- `ForReferenceProjectionAdapter`

后续 adapter：

- `JoinSelectProjectionAdapter`
- `GroupSelectProjectionAdapter`
- `UnionProjectionAdapter`

### select adapter

输入：

```kotlin
T.select(fields: ToSelect<T, Any?>): SelectClause<T, T>
```

输出：

```kotlin
SelectClause<T, Projection>
```

改写：

```kotlin
T.selectGeneratedProjection(Projection::class, fields)
```

### forSelect adapter

`forSelect` 的重点是复用字段模型，不一定立即生成最终 query 返回类型。它可能只生成：

- 可复用字段列表。
- 子查询 select item。
- join projection metadata。

第一步只做字段模型共享：

```kotlin
forSelect { [it.id, it.username] }
```

与 `select` 使用同一 `KronosProjectionAnalyzer`，但 adapter 可以选择不生成 class，只返回 `Field` model。若后续某个调用需要静态返回类型，再触发 class generation。

### forReference adapter

`forReference` 多数场景是字段引用而不是 DTO 投影：

```kotlin
forReference { [it::id, it::orders] }
```

这里应复用表达式读取和字段去重逻辑，但默认不生成投影 KPojo。只有出现“引用结果需要作为 typed projection 返回”时再启用 class generation。

### join / group / union 规则

join：

- 跨表字段必须带来源。
- 重名字段要求 alias。
- 生成属性名不能重复。

group / aggregate：

- 分组字段可以无 alias。
- 聚合字段必须 alias。

union：

- 多个 select 的 projection field 数量、顺序、兼容类型必须一致。
- 不一致给 FIR diagnostic。

### 第 4 阶段测试

按 adapter 分文件：

```text
testData/box/projection/forSelectFieldModel.kt
testData/box/projection/forReferenceFieldModel.kt
testData/box/projection/joinProjectionRequiresAlias.kt
testData/diagnostics/projection/unionProjectionMismatch.kt
```

## 阶段 5：诊断、测试体系、文档与维护

### 诊断设计

新增 diagnostics：

```text
KRONOS_UNSUPPORTED_PROJECTION_EXPRESSION
KRONOS_PROJECTION_FIELD_REQUIRES_ALIAS
KRONOS_PROJECTION_DUPLICATE_PROPERTY
KRONOS_PROJECTION_SOURCE_NOT_KPOJO
KRONOS_PROJECTION_UNION_MISMATCH
KRONOS_PROJECTION_INTERNAL_ERROR
```

诊断文案要直接告诉用户怎么改：

- “函数投影字段需要 alias，例如 `f.count(it.id) as_ "total"`。”
- “投影字段名 `id` 重复，请使用 alias 区分。”
- “自动投影只能用于 KPojo 源类型。”

### 测试分层

保留现有 kctfork 测试，新增官方 testData：

```text
kronos-compiler-plugin/testData/box/projection/
kronos-compiler-plugin/testData/diagnostics/projection/
```

推荐测试顺序：

1. `selectLocalProjection.kt`
   - 证明插件能生成局部类型。
2. `selectProjectionKeepsSourceReceiver.kt`
   - 证明 `it` 是源 DTO。
3. `selectProjectionQueryType.kt`
   - 证明 `queryOneOrNull()` 默认返回投影类型。
4. `selectProjectionRuntimeMapping.kt`
   - 证明 runtime set/get 可用。
5. diagnostics 负例。

所有包含 `[]` 的动态编译测试必须显式带 `-Xcollection-literals`。官方 test runner 也需要确认 compiler args，不能只依赖主工程 Gradle 参数。

### 与现有测试迁移的关系

`kronos-compiler-plugin/testData` 已经是官方 compiler test infrastructure 的迁移目录。投影功能应优先放在 `testData`，runner 放在 `com.kotlinorm.compiler.official`：

```kotlin
@Test
fun selectLocalProjection() {
    runBoxTest("projection/selectLocalProjection")
}
```

如果 FIR dump 对调试有价值，再追加 `.fir.txt`、`.fir.ir.txt`。不要一开始广泛生成 golden，避免 Kotlin patch 升级时维护成本爆炸。

### 文档更新

功能完成后同步：

- `README.md`
- `kronos-docs` 中 select / join / subquery / AI usage 相关页面。
- `.agents/skills/kronos-dev-guide`
- `.agents/skills/kronos-dev-kcp`
- `Evolution.md`

文档需要明确两种写法：

```kotlin
// 自动局部投影，适合函数内部继续使用
val row = user.select { [it.id, it.username] }.queryOneOrNull()

// 手写 DTO，适合公开 API 返回类型
fun getBrief(): UserBrief? {
    return user.select<User, UserBrief> { [it.id, it.username] }.queryOneOrNull()
}
```

### 风险与回退方案

风险 1：`FirFunctionCallRefinementExtension` 是不稳定 API。

回退：

- 把所有直接 API 调用封装在 `KronosProjectionCallRefinementExtension` 和 `KronosProjectionFirTypeBuilder`。
- Kotlin 升级时只集中改这些文件。

风险 2：局部 FIR class 不能被现有 KPojo IR transformer 正确增强。

回退：

- 第一版生成普通显式 `get/set/kronosColumns/kClass`，跳过通用 KPojo transformer。
- 后续再统一。

风险 3：`kClassCreator` 不收集局部投影类。

回退：

- 在自动投影路径传入专用 constructor lambda，而不是只传 `KClass`。
- 例如新增 `SelectClause<T, R>(projectionFactory: () -> R)`。
- 这会影响 core API，只有在 `kClassCreator` 无法稳定支持局部类时使用。

风险 4：SQL label 与生成属性名不一致。

回退：

- 自动投影路径强制所有 select item alias 为生成属性名。
- 手写 DTO 路径保持现有行为。

## 里程碑验收

阶段 1 完成：

- 有通用 projection model。
- 能解析基础字段列表。

阶段 2 完成：

- `select { [it.id, it.username] }` 的返回类型被 FIR 细化。
- `row?.username` 编译通过。

阶段 3 完成：

- 自动投影类型能被 KPojo runtime 映射。
- 不使用反射。

阶段 4 完成：

- `forSelect`、`forReference` 至少复用字段模型。
- 新 DSL 不复制 select 专用解析逻辑。

阶段 5 完成：

- official testData 覆盖正例和负例。
- 文档、skill、Evolution 同步。
