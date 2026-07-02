# Evolution Index

Use this index before opening `Evolution.md`. Match by compiler phase, symptom, stack trace, or keyword, then read only the matching entry with a targeted search such as:

```powershell
Select-String -Path .agents/skills/kronos-dev-kcp/Evolution.md -Pattern "ENTRY TITLE OR UNIQUE ERROR" -Context 0,22
```

If no entry matches, do not open the full evolution log; continue with the relevant KCP reference, source search, compiler dumps, or focused tests. After adding a new `Evolution.md` entry, add one concise row here.

| Area | Keywords / Symptoms | Evolution Entry |
|------|---------------------|-----------------|
| FIR projection scope | generated projection class, local class escapes, CME, source scope | `2026-06-29 - FIR select 投影字段知道得太晚，不能普通插入源码作用域 class` |
| Select receiver/refinement | select lambda receiver, source KPojo, refined result projection | `2026-06-29 - select 必须保留源 KPojo 作为 lambda receiver，同时 refine 结果投影` |
| Result vs context model | projection result, clause context, `KronosSelectResult`, `KronosSelectScope` | `2026-06-29 - 投影结果模型和后续子句上下文模型不是同一个东西` |
| Projection class shape | data class, no-arg constructor, KPojo projection | `2026-06-29 - 生成投影 KPojo 必须是 data-class 形态，并且可无参构造` |
| Internal FIR API | `INVISIBLE_MEMBER`, `INVISIBLE_REFERENCE`, `-Werror`, `ownerGenerator` | `2026-06-29 - 可以用 suppress 直接调用 internal FIR API，但 -Werror 会卡住` |
| Lazy FIR-backed projection | `IrPropertySymbolImpl is unbound`, `Fir2IrLazyClass`, type argument transform | `2026-06-29 - 从 IR type argument 里 transform lazy FIR-backed projection class 会导致 property symbol unbound` |
| Fake getter backend | `Fake override should have at least one overridden descriptor`, FIR generated getter | `2026-06-29 - FIR-generated projection property access 可能以 fake getter 进入 JVM 后端` |
| FIR accessor crash | `FirFakeOverrideGenerator`, `NoSuchElementException`, manual accessor | `2026-06-29 - 手动给 FIR property 加 accessor 可能触发 fake-override substitution 崩溃` |
| Lazy mutation | `Mutation of Fir2Ir lazy elements is not possible`, `AbstractFir2IrLazyFunction.setBody` | `2026-06-29 - Fir2Ir lazy declaration 不能在 IR extension 中 mutate` |
| Mutable IR fake override | `replaceFakeBody`, `replaceFakeProp`, mutable declaration only | `2026-06-29 - backend IR 可以修 fake override，但只能修可变 declaration` |
| Official test failure triage | Gradle only shows `DefaultMultiCauseException`, inspect `TEST-*.xml` | `2026-06-29 - official compiler test 的真实失败要看 XML 报告` |
| FIR call refinement return | `FirFunctionCallRefinementExtension.transform`, must return `FirFunctionCall`, `run` wrapper | `2026-06-29 - FirFunctionCallRefinementExtension.transform 返回值必须仍是 FirFunctionCall` |
| Projection materialization | generated projection, backend materialize, `kClassCreator`, lazy class skip | `2026-06-30 - 生成投影类要在后端物化后再进入 KClassCreator 映射` |
| KPojo val/projection metadata | `val` writable, `fromMapData`, source metadata, official box | `2026-06-30 - KPojo val 属性写入和 projection 元数据需要官方 box 测试覆盖` |
| Operator expressions | `it.score + 10`, old field-list diagnostic, unaryPlus forbidden | `2026-06-30 - 二元运算符字段表达式不能再被旧字段列表诊断误拦截` |
| Incremental kClassCreator | stale init map, new KPojo missing, incremental compile | `2026-06-30 - Kronos.init 调用点生成的 kClassCreator 在增量编译下可能变成旧快照` |
| Moving nested selectable | `wrong parent`, lambda local function, `deepCopyWithSymbols` | `2026-06-30 - 移动嵌套 selectable 调用时不要 deep-copy lambda local function` |
| Tuple IN array store | `ArrayStoreException`, `Field` in `Array<Int?>`, collection literal | `2026-06-30 - Condition tuple IN 不要把 Field 塞进用户侧 Array<T>` |
| Scalar subquery RHS | raw `SelectClause` as Criteria value, wrap as `CriteriaSubqueryValue.Scalar` | `2026-06-30 - Scalar subquery comparison RHS must be wrapped as CriteriaSubqueryValue.Scalar` |
| FIR member filtering | `name.asString().startsWith("<")`, synthetic member, source fields, structured FIR metadata | `2026-07-01 - FIR 成员过滤不要用名字字符串启发式` |
| Compiler test layering | `task.sql`, SQL string in box, renderer assertion, testData boundary | `2026-07-01 - compiler plugin 测试不要断言最终 SQL 字符串` |
| Nested projection source | `IrConstructorSymbolImpl is unbound`, generated projection as Source, next-layer select | `2026-07-02 - generated projection 作为下一层 Source 时不要展开 FIR lazy class` |
