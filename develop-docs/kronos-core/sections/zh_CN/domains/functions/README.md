# 函数系统与 Transformer

函数系统用于将“函数调用表达式”映射为不同数据库的 SQL 片段。

- 核心组件：
  - FunctionManager：注册/选择 FunctionBuilder；
  - FunctionBuilder（com.kotlinorm.interfaces）：
    - support(field: FunctionField, db: DBType): Boolean
    - transform(field, dataSource): String
  - FunctionField（com.kotlinorm.beans.dsl）：描述函数名、参数、别名等；
- 内置 builder：
  - PolymerizationFunctionBuilder（聚合）
  - MathFunctionBuilder（数学）
  - StringFunctionBuilder（字符串）
- 扩展点：
  - FunctionManager.registerFunctionBuilder() 可注册自定义 builder，实现跨数据库的函数适配；
- 与编译期 Transformer 的关系：
  - 编译期（kronos-compiler-plugin）可将 DSL 的函数表达式构造成 FunctionField；
  - 运行时由 FunctionManager 选择合适的 builder 基于 dbType 生成方言 SQL。
