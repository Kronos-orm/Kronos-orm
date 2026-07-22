# 重渲染 SQL 不得重新准备参数

## 症状

一个查询任务已经完成 ValueCodec 参数准备，外层查询随后复用其 AST 和 `paramMap` 再次调用普通 render。即使 `fieldsMap` 为空，raw 参数路径仍会递归集合并处理 runtime Enum，造成二次编码风险和 expanded-list 对象被重建。

## 规则

- 区分首次 render 与仅为外层 SQL 包装进行的 re-render。
- re-render 必须保留 dialect 渲染、used-parameter 过滤、parameterNames 和 `listParameterOccurrences`。
- 已准备的整张参数表必须跳过 `toDatabaseParameterValue`，不要用空 `fieldsMap` 表示“无需转换”。
- 不要把 `PreparedValue` 包在 expanded List 外层；这会破坏 list 参数所需的集合形状。

## 验证

用 scalar 和 `expandAsList` 参数构造已准备的 count task，注册可观测的 Enum codec。外层 count wrapper 应保持 codec 调用次数为零、参数对象不变，并保留正确的 list occurrence。
