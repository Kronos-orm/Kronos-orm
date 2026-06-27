# Evolution

This file records recurring Kronos-ORM development pitfalls and their proven fixes.

## 使用说明

- 每次构建成功后，将遇到的重要问题与解决方案记录到此文件
- 每次修复报错前,先阅读此文件了解历史踩坑情况
- 遵循已有的最佳实践,避免重复犯错

## 记录格式

```markdown
## [日期] - [问题描述]

### 问题症状
[描述问题的具体表现和错误信息]

### 问题原因
[分析问题的根本原因]

### 解决方案
[列出解决该问题的具体步骤]

### 预防措施
[列出如何避免此类问题的最佳实践]
```

## 2026-06-27 - Collection literal tests require the explicit compiler flag

### 问题症状
在 compile-testing 动态编译测试中使用 `select { [it.id, it.name] }`、`reference { [it::id, it::name] }` 或类似 collection literal 语法时，如果没有为动态编译器传入 `-Xcollection-literals`，测试源码无法按 Kotlin 2.4 集合字面量语法解析。

### 问题原因
Kronos 的主 Gradle 编译参数和 compile-testing 动态编译参数是两套配置。根项目 `freeCompilerArgs` 不会自动传递给 `KotlinCompilation.kotlincArguments`。

### 解决方案
在 compile-testing 工具入口中显式设置 `kotlincArguments = ["-Xcollection-literals"]`。Kotlin 2.4.0 正式版已验证 `reference { [it::id, it::name] }` 可以通过动态编译测试。

### 预防措施
新增或修改 compile-testing 用例时，如果源码包含 `[]` 集合字面量，先确认对应测试工具入口传入了 `-Xcollection-literals`；不要把主工程编译参数等同于动态编译参数。

## 2026-06-27 - Field projection plus must not be reused for arithmetic

### 问题症状
将字段列表从 `select { it.a + it.b }` 迁移到 `select { [it.a, it.b] }` 后，如果测试或文档仍保留旧字段列表写法，SQL 会变为 `(... + ...) AS add` 这类真实加法表达式，导致断言失败。

### 问题原因
`+` 已改为真实运算符函数表达式，不再表示字段投影列表。旧测试输入会被正确解析为 `FunctionField("add", ...)`。

### 解决方案
字段投影列表统一迁移为 `[]`、`listOf(...)` 或投影调用；只在真正的算术或字符串拼接场景保留 `+`。

### 预防措施
修改 select/orderBy/groupBy/by/update/upsert/reference/cascade 示例或测试时，先扫描旧字段列表模式，确保 `+` 只表示函数/算术语义。
