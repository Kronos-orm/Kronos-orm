# IDEA 插件控制流异常必须重新抛出

## 症状

IDEA 日志出现：

```text
Control-flow exceptions (e.g., this class com.intellij.openapi.progress.CeProcessCanceledException) should never be logged.
```

栈里可能能看到 `com.kotlinorm.idea.KronosIdeaSafe.guard`，随后进入 documentation、completion、resolve extension 等插件扩展点。

## 原因

IntelliJ Platform 的取消异常属于控制流，不是普通失败。插件的兜底 `guard` 如果捕获 `Throwable` 后直接 `LOG.warn`，会把 `ProcessCanceledException`、协程 `CancellationException` 等控制流异常吞掉并写日志，IDEA 会把这视为插件错误。

## 处理

`KronosIdeaSafe.guard` 必须先捕获并重新抛出：

- `com.intellij.openapi.progress.ProcessCanceledException`
- `java.util.concurrent.CancellationException`

之后才允许捕获其他 `Throwable` 并返回 fallback。

## 防回归

新增 IDEA 插件扩展点时可以继续用 `KronosIdeaSafe.guard`，但不要在扩展点里单独 `catch Throwable`。如果必须局部兜底，也要先重新抛出上述取消异常。
