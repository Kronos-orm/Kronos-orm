---
name: "Bug report / 缺陷反馈"
about: "Report a reproducible problem in Kronos ORM"
title: "[Bug] <short summary>"
labels: [bug]
assignees: []
---

## Description / 问题描述
A clear and concise description of what the bug is. / 对问题的简要说明。

## Affected module(s) / 影响模块
- [ ] kronos-core
- [ ] kronos-logging
- [ ] kronos-jdbc-wrapper
- [ ] kronos-codegen
- [ ] kronos-compiler-plugin
- [ ] kronos-gradle-plugin / kronos-maven-plugin
- [ ] other: ___________________

## Environment / 运行环境
- Kotlin version: 
- JDK version: 
- Gradle/Maven version: 
- OS: 
- Database type (DBType): 
- JDBC driver / DataSource: 

## Reproduction steps / 复现步骤
1. 
2. 
3. 

Provide a minimal reproduction if possible. 最小可复现仓库或代码片段（建议）：

```kotlin
// minimal code here
```

If SQL is involved, include the named SQL and parsed result: 若涉及 SQL，请附命名 SQL 与解析结果
```sql
-- named SQL
```

## Expected behavior / 期望行为
What you expected to happen. / 期望发生什么。

## Actual behavior / 实际行为
What actually happened, including stack traces. / 实际发生了什么，包含堆栈。

```
<logs or stacktrace>
```

## Additional context / 附加信息
- Does it reproduce with NoneDataSourceWrapper? 是否在未配置数据源（NoneDataSourceWrapper）场景下复现：
- Related to NamedParameterUtils / FunctionManager / TaskEventPlugin? 是否与命名参数解析/函数系统/事件插件相关：
- Screenshots or diagrams if helpful. 截图或示意图。

## Checklist / 提交前检查
- [ ] I have read the CONTRIBUTING.md / 已阅读贡献指南
- [ ] I can reproduce this issue with the minimal example / 最小复现可重现
- [ ] I included environment details and DB type / 已提供环境与数据库类型
