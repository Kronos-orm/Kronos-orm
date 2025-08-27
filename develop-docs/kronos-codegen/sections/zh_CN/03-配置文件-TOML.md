# 3. 配置文件（TOML）— 扩展

本章详述配置项的完整结构、校验规则，以及 `extend` 的合并算法。

## 结构

- `[[table]]`（必填，1..N）
  - `name: String`（必填）— 数据库表名。
  - `className: String?`（可选）— Kotlin 类名；默认使用 `tableNamingStrategy` 转换并首字母大写。
- `[strategy]`（可选）
  - `tableNamingStrategy: String?` — `lineHumpNamingStrategy` | `noneNamingStrategy`。
  - `fieldNamingStrategy: String?` — 同上。
  - `createTimeStrategy: String?` — `@CreateTime` 对应的列名。
  - `updateTimeStrategy: String?` — `@UpdateTime` 对应的列名。
  - `logicDeleteStrategy: String?` — `@LogicDelete` 对应的列名。
  - `optimisticLockStrategy: String?` — `@Version` 对应的列名。
  - `primaryKeyStrategy: String?` — `@PrimaryKey` 对应的列名；在 KronosTemplate 中默认按自增处理。
- `[output]`（必填）
  - `targetDir: String`（必填）— `.kt` 输出目录。
  - `packageName: String?`（可选）— 未设置时从 `targetDir` 的 `main/kotlin/` 片段推断。
  - `tableCommentLineWords: Int?`（可选）— 表注释换行宽度；默认 `MAX_COMMENT_LINE_WORDS`。
- `[dataSource]`（必填）
  - `dataSourceClassName: String?` — 默认 `org.apache.commons.dbcp2.BasicDataSource`。
  - `wrapperClassName: String?` — 默认 `com.kotlinorm.KronosBasicWrapper`。
  - 其余任意属性将通过反射映射到 setter：`url`、`username`、`password`、`driverClassName`、连接池参数等。

## 校验

- `table` 必须是列表；每项必须包含 `name`。
- `output.targetDir` 必须非空。
- `dataSource` 必须存在；若默认 DataSource 接受默认值，则可缺省部分键。
- 命名策略仅接受预定义字面量；其它策略可在后续版本扩展。

## extend 合并算法

`readConfig(path)` 伪代码：

```
config = parseToml(path)
while ("extend" in config):
  base = parseToml(config["extend"])  // 找不到/为空则抛错
  log("Config extension found: $extendPath")
  config = base + config  // 右侧优先（子覆盖父）
return config
```

- 支持多级继承。
- 不显式检测循环依赖；请避免互相 extend。

## 示例

- 基础数据库配置 + 项目差异化覆盖。
- 多构建环境（dev/ci/prod）通过多层 extend 叠加。