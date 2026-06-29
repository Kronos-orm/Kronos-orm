# Kronos Projection Work Status

更新日期：2026-06-30

## 当前目标

基础 `select` 投影链路已经打通，当前目标从“把投影跑通”转为“稳住投影管线并为下一阶段的子查询 / 窗口函数接入做准备”。

当前可用的基础形态是：

```kotlin
val idRows = user.select { it.id }.queryList()
idRows.firstOrNull()?.id

val rows = user.select { [it.id, it.name] }.queryList()
rows.firstOrNull()?.name
```

`select` lambda 的 `it` 仍然是完整源 KPojo，`queryList/queryOne/queryOneOrNull` 返回生成的投影 KPojo 类型。

## 已完成的方向性改动

- FIR 侧已负责生成稳定的顶层 projection class，带 data-class 形态、`KPojo` 继承、可见属性和稳定命名。
- backend 侧已负责 materialize 真实 IR class，并补齐 `KPojo` fake overrides + body。
- `SelectClause<T, R>` 已携带结果投影类型 `R`，`queryList/queryOne/queryOneOrNull` 返回 `R`。
- `Patch.kt` 保留裸 `select { ... }`，并保留内部 `selectGeneratedProjection` 给编译器插件改写使用。
- `KronosProjectionIrTransformer` 已接入 `ErrorReporter`，并在 materialize 后补 fake overrides，再复用 `KronosIrClassTransformer` 生成方法体。
- `ProjectionBoxTest.generatedSelectProjection` 已通过，说明基础 projection 生成链路已经可用。
- `.agents/skills/kronos-dev-kcp/Evolution.md` 已记录 FIR/IR 过程中遇到的主要坑。

## 当前最新状态

基础投影链路已验收通过，不再停留在“找不到 projection class”这类前端可见性问题上。

当前已经确认的关键结论：

- 不能只靠 backend IR 修补，FIR 必须先把 projection 类型声明出来，IDE/补全才看得到。
- 投影类不能再做成普通 class；需要 data-class 形态，并且带可无参构造的默认值路径。
- `SelectClause` 的结果类型和后续子句 scope 不是同一个概念，后续子查询和窗口函数会继续依赖这条分层。
- 现有 `SubqueryExpression` / `SubqueryTable` / `InSubqueryExpression` AST 已经在 core 里准备好，下一阶段应直接接入现有 `select` / `where` / `having` 管线，而不是新造一套割裂 DSL。

当前更适合接手的下一步不是继续补基础投影，而是：

1. 先把子查询第一场景设计清楚。
2. 再把 `select { ... }` 的 DSL 承载能力向标量子查询、`exists`、`in`、窗口函数逐步扩展。
3. 继续维持“FIR 负责可见声明，backend 负责 body/materialize”的分工。

## 重要注意事项

- 不要回到解析源码文本的方向。
- 不要仅靠 backend IR 手造用户可见类型；用户源码需要 `.id/.name` 在 FIR 前端可解析。
- 生成投影类应保持 data-class 形态，并保留默认值以支持 KPojo 无参构造路径。
- `SelectClause` 结果投影和子句 scope 以后会分开使用，不要把它们重新揉成同一个模型。
- `.tmp-kotlin-src/` 是为查看 Kotlin compiler sources jar 解压出的本地临时目录，不应提交。

## 下一步建议

1. 以子查询第一场景为起点，补 `SELECT` 列表里的标量子查询承载能力。
2. 结合现有 `SubqueryExpression` / `InSubqueryExpression` / `SubqueryTable` AST，评估 DSL 里应当复用 `select { ... }` 的哪些入口。
3. 给子查询、`exists`、窗口函数设计统一的 DSL 形状，避免再出现 `selectFrom(tmp)` 这种割裂接口。
4. 保持 compiler plugin 的 FIR / backend 分工不变，先让类型可见，再补执行体。
