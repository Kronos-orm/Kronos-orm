# 6. 各 Transformer 详解

本节逐一介绍编译器插件中各个 Transformer 的职责、触发条件与核心改写点，帮助你在阅读 IR dump 或扩展插件时快速定位。

## 6.1 KronosParserTransformer（总调度）

- 角色：全局 IR 访问器与分发器。
- 遍历钩子：
  - `visitCall(IrCall)`：
    - 收集类型实参中 `superTypes().any{ it.classFqName == KPojoFqName }` 的类（加入 `kPojoClasses`）；
    - 若被调函数含 `@KronosInit`，抓取 `IrFunctionExpressionImpl.function` 存入 `initFunctions`；
    - 若 fqName 命中 `TypedQuery/SelectFrom*` 规则，则调用 `updateTypedQueryParameters` 注入尾参；
  - `visitFunctionNew(IrFunction)`：按扩展接收者的 `classFqName` 路由至具体 KTable* Transformer（Select/Set/Condition/Sort/Reference）；
  - `visitClassNew`/`visitClassReference`/`visitConstructorCall`：凡 `superTypes` 包含 `KPojo` 的类，均加入 `kPojoClasses`；
- 设计要点：
  - 类型收集与 KTable 改写并行进行，但互不干扰；
  - 仅对命中的 API 做最小注入（避免污染非 Kronos 调用）；
  - 所有注入均维持原表达式执行顺序与副作用。

## 6.2 KTableParserForSelectTransformer（字段选择）

- 触发点：扩展接收者为 `KTableForSelect<*>` 的函数体。
- 遍历钩子：`visitReturn(IrReturn)`，并保持原 `return` 语义不变。
- 注入点与伪 IR：
  - 在 `return` 之前注入 `addFieldList(irFunction, irReturn)`（见 `kTableForSelect.KTableForSelectUtil`）。
  - 伪代码：
    - 原：`{ body ...; return expr }`
    - 后：`{ addFieldList(... from expr ...); return expr }`
- 解析规则（collectFields）：
  - GET_PROPERTY：`it.username`、`User::age` → `Field("username")`；
  - 别名：`it.createTime.as_("time")` → `Field("createTime").setAlias("time")`；
  - 组合：`a + b` 展开为并列字段；`unaryPlus(+x)` 递归展开；
  - 排除：`-Entity::class` 解析为“除去该类所有列”（根据 `analyzeMinusExpression` 返回的 excludes）；
  - 常量：`"literal"/1/true` → `Field(sql = literal, type=CUSTOM_CRITERIA_SQL)`；
- 实现细节与边界：
  - 仅在与当前 `irFunction.symbol` 匹配的 `return` 注入，避免误改嵌套 lambda 的 `return`；
  - 通过 `extensionReceiverArgument/dispatchReceiverArgument` 获取接收者；
  - 产物通过 `addSelectFieldSymbol(receiver, fieldExpr)` 追加至 `KTableForSelect`；
  - 如未来需要支持函数调用（聚合函数）字段，可在 `getFunctionName` 分支扩展。

## 6.3 KTableParserForSetTransformer（更新赋值）

- 触发点：扩展接收者为 `KTableForSet<*>` 的函数体。
- 遍历钩子：`visitBlock(IrBlock)`，仅对 `origin == null` 的顶层块注入，避免重复处理内部语句块；
- 注入点与伪 IR：
  - 在函数块末尾追加 `putFieldParamMap(irFunction)` 的结果（若存在赋值语句会生成 `setValue/setAssign` 调用列表）。
  - 伪代码：
    - 原：`{ stmt... }`
    - 后：`{ stmt...; putFieldParamMap(this) }`
- 解析规则（putParamMapStatements）：
  - `IrStatementOrigin.EQ`：`it.name = v` → `setValue(receiver, Field("name"), v)`；
  - `IrStatementOrigin.PLUSEQ/MINUSEQ`：`it.count += v` → `setAssign(receiver, "+", Field("count"), v)`；
  - 接收者解析优先 `extensionReceiverArgument`，退化到 `dispatchReceiverArgument`。
- 实现细节与边界：
  - 只在最外层 `IrBlock` 注入，防止对 if/when/try 的内部块体多次注入；
  - 若无匹配赋值，不会产生任何注入；
  - 右值保持原 IR，不做求值重排。

## 6.4 KTableParserForConditionTransformer（条件拼装）

- 触发点：扩展接收者为 `KTableForCondition<*>` 的函数体。
- 遍历钩子：`visitReturn(IrReturn)`，只处理当前函数的 `return`（`returnTargetSymbol == irFunction.symbol`）。
- 注入点与伪 IR：
  - 将 `buildCriteria(...)` 的返回值经 `criteriaSetterSymbol(receiver, criteria)` 写回；
  - 伪代码：`{ ...; return expr } -> { setCriteria(buildCriteria(body)); }`（注意返回语义仍由宿主 API 决定，Transformer 不直接 `return`）。
- 解析规则（buildCriteria）：
  - 递归遍历 `IrBlockBody/IrCall/IrWhen/IrReturn/...` 构造树状 `CriteriaIR`；
  - 运算符与方法名映射：`== != > >= < <=`、`and/or/not`、`in/notIn`、`between`、`like`、`isNull/isNotNull`；
  - 参与节点：列名/表名/取值/子条件；
  - 注解影响：`Serialize*`/`Ignore*`/`Cascade*` 改变节点是否参与、是否级联展开等；
- 实现细节与边界：
  - 当 `expr` 为空/常量时会退化生成 ROOT 条件或 `CUSTOM_CRITERIA_SQL`；
  - `setNot` 用于处理 `!`/`not` 取反链；
  - 保留原表达式，以便运行期与调试一致。

## 6.5 KTableParserForSortReturnTransformer（排序）

- 触发点：扩展接收者为 `KTableForSort<*>` 的函数体。
- 遍历钩子：`visitReturn(IrReturn)`。
- 注入点与伪 IR：
  - 在 `return` 前注入 `addFieldSortsIr(irFunction, irReturn)`，逐条生成 `addSortField(...)`；
  - 伪代码：`{ ...; return expr } -> { addSortField(f1); addSortField(f2); return expr }`。
- 解析规则（getSortFields）：
  - GET_PROPERTY：默认 `asc()`；
  - `plus`：拼接多个排序项（左 + 右）；
  - `asc()/desc()`：显式方向；
  - 常量/变量：包装为 `asc()`；
- 实现细节与边界：
  - 通过 `funcName()` 解析方法标识；
  - 使用 `extensionReceiverArgument` 取被 `asc/desc` 修饰的列；
  - 保留用户表达式作为返回值的一部分（不移除原 `expr`）。

## 6.6 KTableParserForReferenceTransformer（表引用/关联）

- 触发点：扩展接收者为 `KTableForReference<*>` 的函数体。
- 遍历钩子：`visitReturn(IrReturn)`。
- 注入点与伪 IR：
  - 在 `return` 前注入 `addReferenceList(irFunction, irReturn)`，对 `IrPropertyReference` 等生成 `addField(...)`；
- 解析规则（collectReferences）：
  - `IrPropertyReference`：`it::prop`、`Entity::prop` 转字段；
  - `unaryPlus`/`PLUS`：拼接多个引用；
  - `IrTypeOperatorCall`：透传内部 `argument`；
- 实现细节与边界：
  - 仅处理 `reference` 语义，不解析别名（若有需要可复用 `getColumnName` 的 alias 能力）；
  - 保留原返回值表达式。

## 6.7 编译末尾：KClass 映射生成

- 回放 `KClassCreatorUtil.initFunctions` 中登记的初始化函数；
- 通过 `buildKClassMapper` 在一个统一位置生成 `kClassCreator` 映射逻辑：
  - 依次匹配 `kPojoClasses` 中的已收集类型，返回实例或 null；
  - 该映射可在运行期查询 `KClass<KPojo>` → `KPojo` 实例，用于框架内部的快速定位与构造。

## 6.8 与 TypedQuery/SelectFrom* 的协作

- 当 `KronosParserTransformer` 捕获到查询执行调用时，会先调用 `updateTypedQueryParameters`：
  - 注入 `isKPojo` 标记与 `superTypes` 列表（字符串形式），方便运行期/后续阶段决定行为；
  - 该步骤与前述 KTable* 变换互不冲突，但通常出现在相同方法体的不同节点上。
