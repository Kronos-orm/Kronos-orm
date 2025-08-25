# 5. 开发者 API（Helpers 详解）

本节对“每一个” Helper/Util 给出设计思路、输入输出与实现要点，便于你在 Transformer 中复用或扩展。函数签名以源码为准，文档只摘要核心语义与约束。

## 5.1 通用 IR Helper（包：`com.kotlinorm.compiler.helpers`）

- IrMemberAccessExpressionHelper
  - `dispatchReceiverArgument` / `extensionReceiverArgument` / `valueArguments`：
    - 设计：IR 在 K2 下把形参排列到 `owner.parameters`，这些扩展通过匹配 `IrParameterKind` 安全取回对应实参；
    - 约束：仅读取，不改写；若目标形参不存在返回 null 或空集合。
  - `contextArguments`：当前 Kronos 不支持 Context Parameter（仅便捷过滤），切勿在业务中依赖。
- IrValueParameterHelper
  - `dispatchReceiver` / `extensionReceiver` / `valueParameters` / `contextReceiver`：与上面对称，用于从 `List<IrValueParameter>` 侧筛选参数；常用于判断某函数是否拥有扩展接收者等。
- IrCallHelper
  - `operator fun IrFunctionSymbol.invoke(vararg args, types, operator)`：
    - 设计：以“可调用”语法快速构造 `IrCall`/`IrFunctionAccessExpression`；
    - 实现：按序写入 `arguments[i]` 与 `typeArguments[i]`；
    - 注意：确保参数个数与类型参数个数与目标符号一致，否则构建的 IR 在后续阶段会报错。
  - `IrExpression.irCast<T>()`：对调试期 ClassCast 更友好，抛出携带期望/实际类型的信息。
  - `Iterable<T>.findByFqName` / `filterByFqName`：在注解/构造调用集合中按 `classFqName` 检索。
- IrCollectionHelpers
  - `mapGetterSymbol` / `listOfSymbol` / `pairSymbol` / `mutableMapOfSymbol`：常用集合/Pair 的符号定位；
  - `irListOf(type, elements)` / `irMutableMapOf(k,v,pairs)` / `irPairOf(first,second,pair)`：构造集合 IR（处理 vararg 与泛型实参）；
  - 设计要点：K2 下 `listOf`/`mapOf` 需要同时设置 `vararg` 与 `typeArguments`；
  - 限制：仅构造不可变 Map（内部使用 `mapOf`）。
- IrEnumHelper
  - `irEnum(enumClassSymbol, name)`：
    - 设计：将枚举名解析到具体 `IrEnumEntry`，生成 `IrGetEnumValueImpl`；
    - 失败时抛出明确错误，便于定位枚举名变更。
- IrKClassHelper
  - `kFunctionN(n)`：取 `kotlin.FunctionN` 符号（用于构造 `KFunctionN` 类型实参）；
  - `irPrintln`：println 的符号引用（仅测试/调试场景）；
  - `IrClassSymbol.toKClass()`：构造 `KClass<T>` 字面量；
  - `IrClassSymbol.instantiate()`：优先使用“无参或所有参数具备默认值”的构造器生成 new 表达式；失败返回 null。
- IrFunctionHelper
  - `operator fun IrFunction.set(kind, param)`：替换 Dispatch/Extension 接收者形参；用于注入临时函数体时重设形参位置。
- IrTypeHelper
  - `IrType.simple()/sub()`：获取 `IrSimpleType` 与第一个类型实参；
  - `IrClassSymbol.nType`：将 `defaultType` 标为可空（某些 API 需要可空 `KClass<T>?` 等）。
- IrTryCatchHelper
  - `IrTryBuilder.irTry(result, type)` + `.catch{}` + `.finally(expr)` + `.build()`：
    - 设计：以构建器模式拼 try/catch/finally；
    - 默认 catch `Throwable`，支持自定义 `throwableType`；
    - 约束：同一异常类型不可重复 catch；finally 只能设置一次；
    - 内置 `printStackTrace` 快捷体，适合在调试注入时兜底。
- IrRefrenceHelper
  - `referenceFunctions(package, name, className?)` / `referenceClass(fqName)`：便捷查找符号；广泛用于各 Util 的符号定位。

## 5.2 KClassCreatorUtil（包：`com.kotlinorm.compiler.plugin.utils`）

- 设计目的：在编译末尾根据收集到的 `kPojoClasses` 生成一个全局映射 `kClassCreator: (KClass<KPojo>) -> KPojo?`；
- 关键成员：
  - `kPojoClasses: MutableSet<IrClass>`：全局缓存，来源于 `KronosParserTransformer` 的类/类型实参与构造扫描；
  - `initFunctions: MutableSet<Triple<IrPluginContext, IrBuilderWithScope, IrFunction>>`：被 `@KronosInit` 捕获的初始化函数（lambda）；
  - `resetKClassCreator()`：每次 `generate()` 开头清空状态；
  - `buildKClassMapper(function)`：将目标函数体改写为：
    - 构造一个 `lambda(kClass: KClass<KPojo>): KPojo?`；
    - 使用 `when(kClass){ KClass(User)-> User(); KClass(Order)-> Order(); else -> null }` 的等价 IR；
    - 通过全局属性 `com.kotlinorm.utils.kClassCreator` 的 setter 注入该 lambda。
- 实现细节：
  - `kFunctionN`/`toKClass`/`instantiate` 多处协作；
  - 分支列表按 `fqNameWhenAvailable` 去重，保持稳定顺序；
  - 失败分支返回 `null`，不抛异常，兼容增量。

## 5.3 查询任务辅助（KQueryTaskUtil）
- 目标：为 `TypedQuery/SelectFrom*` 注入两个尾参：`isKPojo:Boolean` 与 `superTypes: List<String>`。
- `fqNameOfTypedQuery`/`fqNameOfSelectFromsRegexes`：匹配点清单；
- `updateTypedQueryParameters(irCall)`：
  - 取 `irCall.typeArguments[0]` 作为查询类型，收集其自身及 `superTypes()` 的 `classFqName`；
  - `isKPojo` 判定是否包含 `KPojoFqName`；
  - 以 `irListOf(String, ...strings)` 构造字面量列表；
  - 写回 `irCall.arguments[args.size-2]` 与 `[...] -1`；务必保持顺序，避免与宿主 API 尾参错位。

## 5.4 KTableForSelect 工具（`utils.kTableForSelect`）
- Definitions：
  - 常量 `KTABLE_FOR_SELECT_CLASS`；符号 `addSelectFieldSymbol`、`aliasSymbol`。
- Util：
  - `addFieldList(irFunction, irReturn)`：针对 `return` 前注入 `addField(Field(...))*`；
  - `collectFields(irFunction, element)`：
    - 识别：`GET_PROPERTY`、`PLUS/MINUS`、`unaryPlus`、`as_`、`IrConst`、`IrPropertyReference`；
    - `-Entity::class` 解析排除列集合；
    - 常量以 `KColumnType.CUSTOM_CRITERIA_SQL` 生成 Field；
  - 产物：一组 `Field`/`Field(alias)` 的 IR 表达式。

## 5.5 KTableForCondition 工具（`utils.kTableForCondition`）
- Definitions：条件节点/函数名解析等（详见源码 `KTableForConditionDefinitions.kt`）。
- Util：
  - `updateCriteriaIr(irFunction)`：将 `buildCriteria(...)` 返回值通过 `criteriaSetterSymbol` 赋给接收者；
  - `buildCriteria(irFunction, element, setNot=false, noValueStrategyType=null)`：
    - 深度优先解析 `IrBlockBody/IrCall/IrWhen/IrReturn/IrConst/...`；
    - 支持关系运算、`and/or/not`、`in/notIn/between/like/isNull` 等；
    - 解析列名（`getColumnName`）、表名（`getTableName`）、值（`getColumnOrValue`）；
    - 结合注解：`SerializeAnnotationsFqName`、`IgnoreAnnotationsFqName`、`CascadeAnnotationsFqName` 等影响节点生成；
    - 返回 `IrVariable`（中间临时变量）供后续 `setCriteria` 使用。

## 5.6 KTableForSort 工具（`utils.kTableForSort`）
- Definitions：`addSortFieldSymbol`、`createAscSymbol`、`createDescSymbol`；
- Util：
  - `getSortFields(irFunction, element)`：
    - 识别 `GET_PROPERTY` 默认 asc、`plus` 链接、`asc()/desc()` 明确方向、常量/变量（按 asc 包装）；
  - `addFieldSortsIr(irFunction, irReturn)`：将上一步的条目转成 `addSortField(...)` 注入。

## 5.7 KTableForSet 工具（`utils.kTableForSet`）
- Definitions：`setValueSymbol`（二参重载）与 `setAssignSymbol`（+=/-=）；
- Util：
  - `putParamMapStatements(irFunction, receiver, element)`：
    - 识别 `EQ/PLUSEQ/MINUSEQ` 起源；将左侧列、操作符、右值组装为 IR；
  - `putFieldParamMap(irFunction)`：对整个函数体收集后统一注入。

## 5.8 KTableForReference 工具（`utils.kTableForReference`）
- Definitions：`addRefFieldSymbol`；
- Util：
  - `collectReferences(irFunction, element)`：识别 `IrPropertyReference`、`unaryPlus` 与 `+` 拼接，将跨表/别名引用收集为字段表达式；
  - `addReferenceList(irFunction, irReturn)`：批量注入。

## 5.9 其它工具（`plugin.utils` 根目录）
- `IrKDocUtil`：
  - 读取源文件并通过 `sourceElement`/offset 反向抽取声明前的注释，`getKDocString()` 生成 `IrExpression`；
  - 用于把表/字段注释注入到生成的函数/属性中。
- `IrKronosFieldUtil`：
  - `getColumnName(IrExpression|IrProperty)`、`getFunctionName(IrCall)`、`getTableName(IrExpression|IrClass)`、`getColumnOrValue`、`irFieldOrNull` 等；
  - 设计：统一从 `IrCall`/`IrPropertyReference` 中恢复 Kronos 的列/函数语义；
  - 限制：当类型不明时回退到字符串/常量形式。
- `IrKronosCommonStragety`：
  - 读取全局策略 getter（`createTimeStrategySymbol` 等）、并结合表级/列级注解裁决有效策略；
  - 返回 `KronosCommonStrategy` 构造调用的 IR；支持禁用（enabled=false）。
- `IrNewClassUtil`：
  - 负责在 KPojo 类上注入/生成 getter/setter、toMap/fromMap、表元信息（表名/注释/索引）、字段集合、策略元信息等；
  - 广泛使用于 `KronosIrClassNewTransformer` 路径（类级改写）。
- `IrSqlTypeUtil`：
  - `kColumnTypeSymbol`、`kotlinTypeToKColumnType` 映射；
- `LRUCache`：
  - 简单的最近最少使用缓存，工具层按需使用。

> 建议：新增 Transformer 时，优先复用上述 Helper；若某 Kotlin 版本变更 IR API，可在 Helper 层隔离修复，避免在多处复制粘贴复杂 IR 代码。