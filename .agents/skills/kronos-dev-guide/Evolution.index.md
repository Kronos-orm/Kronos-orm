| 症状/关键词 | 经验记录 |
|---|---|
| ValueCodec；ENCODE；logical input；BasicValueCodec；TemporalValueCodec；String to Int；coercion guard | [ENCODE 类型保护必须保留内置 coercion](evolution/2026-07-21-encode-built-in-coercion-guard.md) |
| JDBC result mapping；ResultColumnMetadata；raw/vendor read；single-pass decode；factory reuse；LOB；SQLXML；PGobject；Oracle LONG | [JDBC 结果必须分离物理读取与逻辑解码](evolution/2026-07-21-jdbc-result-codec-single-pass.md) |
| pagination；count wrapper；prepared parameters；double encode；listParameterOccurrences；empty fieldsMap | [重渲染 SQL 不得重新准备参数](evolution/2026-07-21-rerender-prepared-parameters.md) |
| generated code；`Patch.kt`；detekt；Codacy；static analysis；codegen template | [Generated sources need durable static-analysis policy](evolution/2026-07-20-generated-source-static-analysis.md) |
| non-root JOIN cascade projection；hidden local key；`profile_id`；owner source | [Non-root JOIN cascade must preserve the owner local key](evolution/2026-07-20-join-cascade-owner-local-key.md) |
| runtime `__tableName` override；stale qualifier；select/page；JOIN；UNION；DDL | [Runtime table overrides must rebind qualifiers](evolution/2026-07-20-runtime-table-name-qualifier-binding.md) |
| ValueCodec；`KType`；`starProjectedType`；primitive；boxed；`javaObjectType`；Boolean encode | [运行时 KType 赋值判断必须归一化 primitive boxing](evolution/2026-07-20-runtime-ktype-primitive-boxing.md) |
| TemporalValueSupport；`java.sql`；`NoClassDefFoundError`；GeneratedClassLoader；optional JDBC KType；lazy holder | [Temporal codec 不得提前解析可选 JDBC 类型](evolution/2026-07-21-temporal-optional-jdbc-classloader.md) |
| typed query；`KSelectable.pojo`；generic `Source`；DSL 泛型擦除；member extension；JVM signature clash | [Typed query properties and JVM signatures](evolution/2026-07-19-typed-query-properties-and-jvm-signatures.md) |
| IDEA plugin；`signPlugin`；`verifyPluginSignature`；`publishPlugin`；Marketplace；PEM；`supportsKotlinPluginMode`；Plugin Verifier；`-offline` | [IDEA 插件签名与校验必须使用明确输入](evolution/2026-07-18-idea-plugin-signing-and-verification.md) |
| IDEA plugin；`buildPlugin`；2026.2；EAP；`javaCompiler`；platform build | [IDEA 平台与 Java compiler 必须使用同一正式 build](evolution/2026-07-18-idea-platform-java-compiler-build-alignment.md) |
| condition IR；普通对象属性变列；顶层属性 NPE；`KPojo.value`；嵌套 `value`；函数参数 | [Condition field lowering must classify the receiver](evolution/2026-07-17-condition-field-lowering-source-ownership.md) |
| FIR condition diagnostic；`takeIf`；`takeUnless`；`if`；`when`；Kotlin control flow；`.value` 误报 | [Condition diagnostics must respect Kotlin control flow](evolution/2026-07-17-condition-diagnostics-control-flow-boundary.md) |
| IDEA plugin；projection completion；`it.`；空 selector；window alias；`rn` 补全缺失 | [IDEA 投影补全必须支持空 selector](evolution/2026-07-13-idea-projection-completion-empty-selector.md) |
| derived select；no projection；logical output names；`@Column("user_name")`；`selected columns were [id, name]` | [派生查询外层必须引用逻辑输出名](evolution/2026-07-13-derived-select-logical-output-names.md) |
| IDEA plugin；`Control-flow exceptions`；`CeProcessCanceledException`；`KronosIdeaSafe`；completion contributor | [IDEA 插件控制流异常必须重新抛出](evolution/2026-07-13-idea-plugin-control-flow-exceptions.md) |
| `Kronos example projects`；外部 example；`kronosColumns()`；`__columns`；元数据 API 迁移 | [外部示例元数据 API 漂移](evolution/2026-07-13-external-example-metadata-api-drift.md) |
| SQL Server `total_count` 无列名；Oracle Map `NUMBER` 返回 `BigDecimal` | [分页计数列命名与 Map 输出类型](evolution/2026-07-10-total-count-alias-and-map-column-types.md) |
