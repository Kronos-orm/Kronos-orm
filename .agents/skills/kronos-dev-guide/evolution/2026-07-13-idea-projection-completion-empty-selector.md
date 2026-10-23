# IDEA 投影补全必须支持空 selector

## 症状

在派生 source 或 window alias 场景里，`it.rn` 手写后能解析并显示 hover，但在 `it.` 后触发补全时看不到 `rn` 等动态投影字段。

典型代码在 `SelectSubquerySqlTest.kt`：

```kotlin
ranked.select { [it.userName, it.createTime, it.rn] }
```

## 补全契约

Kotlin completion 在 `it.<caret>` 这种未输入 selector 的位置，不保证 PSI 已经形成 `KtNameReferenceExpression`。补全测试必须直接在空 selector 触发，不能以手写完整字段后的解析或 hover 代替。

`it.r<caret>` 和 `it.<caret>` 要分别看护：前者通常有 selector reference，后者可能只有 `KtDotQualifiedExpression` 且 `selectorExpression == null`。

## 当前实现

KEFS 加载 `kronos-compiler-plugin` 的 FIR 扩展，投影成员由 FIR 声明生成器提供，字段补全和类型解析使用 Kotlin IDE 自带的服务。

生成成员必须保留正确的 owning class symbol 和生成源标识，见 KCP 经验记录 [IDE-generated projection members need an explicit symbol owner](../../kronos-dev-kcp/evolution/2026-10-20-kefs-generated-symbol-ownership.md)。

原独立 IDE 插件的补全 contributor、resolve extension 和系统属性广播桥已由这条 FIR 路径替代。

## 防回归

`kronos-testing/ide/src/test/kotlin/com/kotlinorm/ide/KefsProjectionFixtureTest.kt` 在真实 IDEA 2026.2.3 + KEFS 0.3.12 中执行：

- 对 JOIN、派生查询、window alias 的 `it.<caret>` 调用 stock Kotlin completion 并断言预期字段。
- 对相同查询手写字段并执行 highlighting，断言正确类型且无意外错误。
- 比较实际加载的 FIR 类字节与本次完整 CLI JAR，防止 SNAPSHOT 缓存掩盖回归。
