# Kronos Projection Work Status

更新日期：2026-06-29

## 当前目标

先打通最基础的自动 select 投影链路：

```kotlin
val idRows = user.select { it.id }.queryList()
idRows.firstOrNull()?.id

val rows = user.select { [it.id, it.name] }.queryList()
rows.firstOrNull()?.name
```

期望 `select` lambda 的 `it` 仍然是完整源 KPojo，`queryList/queryOne/queryOneOrNull` 返回生成的投影 KPojo 类型。

## 已完成的方向性改动

- 新增 FIR projection call refinement / declaration generation 相关文件。
- 新增 IR projection transformer 临时衔接逻辑，用于基础 select 竖切。
- `kronos-core` 的 `SelectClause<T, R>` 已携带结果投影类型 `R`，`queryList/queryOne/queryOneOrNull` 返回 `R`。
- `Patch.kt` 已保留裸 `select { ... }`，并新增内部 `selectGeneratedProjection` 给编译器插件改写使用。
- compiler plugin module 已关闭 `allWarningsAsErrors`，允许当前阶段使用 FIR internal API + suppress。
- `.agents/skills/kronos-dev-kcp/Evolution.md` 已记录 FIR/IR 过程中遇到的主要坑。

## 当前最新状态

`ProjectionBoxTest.generatedSelectProjection` 的上一轮基线失败为：

```text
MISSING_DEPENDENCY_CLASS: Cannot access class
'com.kotlinorm.generated.projection.KronosSelectResult_6ec22c25'
```

该失败来自 official compiler test 的 `NoFirCompilationErrorsHandler`，完整细节在：

```text
kronos-compiler-plugin/build/test-results/test/TEST-com.kotlinorm.compiler.ProjectionBoxTest.xml
```

最新实验方向是按 `FirFunctionCallRefinementExtension` 的契约，让 `transform` 生成类似：

```kotlin
run {
    class KronosSelectResult_xxx(...)
    originalSelectCall as SelectClause<Source, KronosSelectResult_xxx>
}
```

当前代码已经开始构造 resolved `kotlin.run` 外壳，并把 projection class 放入 lambda block。该实验尚未完成验证；上一次 `compileKotlin` 在用户中断前正在重跑。

## 重要注意事项

- 不要回到解析源码文本的方向。
- 不要仅靠 backend IR 手造用户可见类型；用户源码需要 `.id/.name` 能在 FIR 前端解析。
- 生成投影类最终必须接近：

```kotlin
data class KronosSelectResult_xxx(
    var id: Int? = null,
    var name: String? = null,
) : KPojo
```

- 生成类必须具备主构造函数和默认值，从而满足 KPojo 无参构造路径。
- `.tmp-kotlin-src/` 是为查看 Kotlin compiler sources jar 解压出的本地临时目录，不应提交。

## 下一步建议

1. 先让 `:kronos-compiler-plugin:compileKotlin` 通过。
2. 再跑 `ProjectionBoxTest.generatedSelectProjection`，查看 XML 中的完整错误。
3. 若 `run` 外壳方向成立，继续清理 top-level dynamic declaration registry 的临时逻辑。
4. 若 `run` 外壳暴露 local class 类型逃逸或 FIR wrapper 结构问题，再回到 declaration generation cache/提前声明方案。
