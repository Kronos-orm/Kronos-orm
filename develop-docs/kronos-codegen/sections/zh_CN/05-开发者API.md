# 5. 开发者 API（扩展）

本章详细介绍面向开发者的 API 与最佳实践。

## 入口函数

- `init(path: String)` — 从 TOML 初始化；设置 `codeGenConfig` 与 Kronos 策略。
- `TemplateConfig.template(render: KronosTemplate.() -> Unit): List<KronosConfig>` — 为每张表构建渲染上下文。
- `List<KronosConfig>.write()` — 将生成内容写入文件。

## 模板上下文（KronosTemplate）

- `packageName: String` — 根据 OutputConfig 计算。
- `tableName: String`、`className: String` — 源于表配置与命名策略。
- `tableComment: String` — 通过 queryTableComment 获取。
- `fields: List<Field>` — 来自 SqlManager.getTableColumns，包含类型/长度/精度/可空/默认值/主键等信息。
- `indexes: List<KTableIndex>` — 来自 SqlManager.getTableIndexes。
- `imports: MutableSet<String>` — 初始包含 `Table` 与 `KPojo`，辅助函数会按需添加。
- `formatedComment: String` — 按宽度折行的表注释，行前缀 `// `。
- `indent(num: Int)` — 返回空格缩进。
- `operator fun String?.unaryPlus()` — 追加一行内容。

## 辅助算法

- `Field.annotations(): List<String>` — 返回注解字符串列表：
  - 主键：若匹配全局 PK 策略则按 IDENTITY 处理；否则根据 `Field.primaryKey` 输出。
  - 必填：`!nullable && primaryKey == NOT` 输出 `@Necessary`。
  - 默认：`defaultValue != null` 输出 `@Default("value")`。
  - 列类型：当需要保持精确信息时输出 `@ColumnType(type = KColumnType.XXX, length, scale)`。
  - 时间/删除/乐观锁：根据 Kronos 策略字段名决定。
- `List<KTableIndex>.toAnnotations(): String?` — 非空返回 `@TableIndex(...)` 的多行字符串；空则返回 null。
- `Field.kotlinType: String` — DB 类型到 Kotlin 类型映射见 Extensions.kt。

## 使用建议

- 使用 `imports.joinToString` 渲染 import，仅包含实际需要的依赖。
- 不要假设策略存在；交由辅助方法自行决定是否输出注解。
- 统一使用 `indent()` 控制缩进，保持风格一致。