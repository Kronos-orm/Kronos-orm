# IDEA 投影补全必须支持空 selector

## 症状

在派生 source 或 window alias 场景里，`it.rn` 手写后能解析并显示 hover，但在 `it.` 后触发补全时看不到 `rn` 等动态投影字段。

典型代码在 `SelectSubquerySqlTest.kt`：

```kotlin
ranked.select { [it.userName, it.createTime, it.rn] }
```

## 原因

Kotlin completion 在 `it.<caret>` 这种未输入 selector 的位置，不保证 PSI 已经形成 `KtNameReferenceExpression`。如果 completion contributor 只从 selector reference 反查 receiver，会在真正需要补全的空 selector 场景直接跳过。

`it.r<caret>` 和 `it.<caret>` 要分别看护：前者通常有 selector reference，后者可能只有 `KtDotQualifiedExpression` 且 `selectorExpression == null`。

## 处理

`KronosProjectionCompletionContributor` 的 receiver 提取必须同时支持：

- `KtNameReferenceExpression.qualifiedReceiver()`，覆盖已有 selector 的场景。
- 父级 `KtQualifiedExpression` 且 `selectorExpression == null` 或 text range 包含 completion offset，覆盖 `it.<caret>`。

补全字段仍从 `KronosProjectionIdeBridge.read()` 的 projection model 中取，不能移除 `kaResolveExtensionProvider`，否则 hover/解析能力会缺失。

## 防回归

保留源码级测试断言：

- 存在 `CompletionParameters.projectionCompletionReceiver()`。
- 处理 `qualified.selectorExpression == null`。
- 仍使用 `model.contextFields` / `model.fields` 向 completion result 添加字段。
