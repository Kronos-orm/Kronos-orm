# 异常与国际化

简图：
```mermaid
flowchart LR
  A[运行时检测] --> B[exceptions/*]
  B --> C[错误抛出]
  C --> D[i18n Noun 提示]
```

主要功能：
- 统一定义与抛出常见异常：NoDataSourceException、InvalidParameterException、InvalidDataAccessApiUsageException 等；
- 通过 i18n Noun 提供一致的错误消息文本。

为什么这样设计：
- 将错误与消息解耦，便于国际化与统一管理；
- 让调用层可以根据异常类型采取不同恢复策略。

使用示例（伪代码）：
```
if (dataSource == null) throw NoDataSourceException(Noun.noDataSourceMessage)
```
