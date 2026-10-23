| 症状/关键词 | 经验记录 |
|---|---|
| CI；SDK cache；ImageVersion；duplicate caches；10GB；cache eviction | [Share compatible SDK caches within a storage budget](evolution/2026-10-22-ci-sdk-cache-budget.md) |
| Windows；SQLite consumer；ProcessBuilder；bash；WSL；no installed distributions；gradlew.bat | [Select the platform Gradle wrapper in external consumers](evolution/2026-10-22-windows-gradle-consumer-wrapper.md) |
| main.kts；Import；公共脚本变化仍执行旧代码；compiled scripts cache | [Fingerprint imported script contents](evolution/2026-10-22-main-kts-import-cache.md) |
| Kotlin Toolchain 0.13；KGP API；More than a single variant；Gradle plugin api-version | [KGP API variants require upstream Toolchain disambiguation](evolution/2026-10-22-toolchain-kgp-api-variants.md) |
| Windows；main.kts；KOTLIN_OPTS；嵌套脚本；Unknown option；父脚本重复执行 | [Clear inherited Kotlin SDK arguments before nested Windows scripts](evolution/2026-10-22-windows-kotlin-script-arguments.md) |
| IDEA SDK；JDK annotations not found；KotlinArtifacts；frontend-split；Toolchain IDE tests | [Preserve the IDEA SDK runtime layout in editor tests](evolution/2026-10-21-ide-sdk-runtime-layout.md) |
| Android SDK；setup-android；Failed to find package tools；API 26/35；platform-tools | [Android SDK setup must request platform-tools explicitly](evolution/2026-10-21-android-sdk-retired-tools.md) |
| main.kts；--jvm-args；javaagent；single quotes；spaces；Windows backslashes | [Toolchain JVM arguments use their own quote grammar](evolution/2026-10-21-toolchain-jvm-argument-quoting.md) |
| kotlinx-datetime 0.8；Instant弃用；kotlin.time.Instant；纳秒；时区；descriptor兼容 | [Use kotlin.time.Instant in platform temporal codecs](evolution/2026-10-21-kotlin-time-instant-codec.md) |
| Windows；Kotlin Toolchain；Git Bash；GNU tar；Cannot connect to C；System32；PATH | [Windows Toolchain wrapper must resolve the native system tar](evolution/2026-10-21-windows-toolchain-native-tar.md) |
| Native SQLite；KGP 2.4.21；JUnit classname；required suite did not execute；macosX64Test | [Gradle Native JUnit suite names include the target task](evolution/2026-10-21-native-junit-suite-prefix.md) |
| Maven deploy-file 3.1.4；KLIB becomes JAR；snapshot metadata；empty module classifier；extension | [Maven snapshot deployment must retain the primary file extension](evolution/2026-10-20-maven-native-snapshot-extension.md) |
| Kotlin Toolchain；watchosX64；Native finalizer；generated fragment；POM target依赖；metadata manifest | [Toolchain Native generation and publication](evolution/2026-10-20-toolchain-native-publication.md) |
| Kotlin Toolchain 0.13；local SNAPSHOT；Unable to download checksums；maven-metadata-local.xml；IDE import | [Toolchain local SNAPSHOTs need a Maven descriptor](evolution/2026-10-20-toolchain-local-snapshot-descriptor.md) |
| SQLite；last_insert_rowid；WITHOUT ROWID；DESC；changes；total_changes；RETURNING；DDL；ASCII identifiers | [SQLite rowid validation and affected counters](evolution/2026-10-18-sqlite-rowid-and-change-counters.md) |
| Native metadata；nativeHeap.free；NativePtr；rawValue；pthread_mutexattr_settype；UInt/Int | [Native POSIX metadata types](evolution/2026-10-18-native-posix-metadata-types.md) |
| Native SQLite；AndroidX 2.6.2；TEXT；embedded NUL；toKStringFromUtf16；getBlob | [Native TEXT NUL read preservation](evolution/2026-10-18-native-sqlite-text-nul.md) |
| Kotlin/Native；composite consumer；linkDebugTest；Metaspace；lost daemon message | [Native consumer link metaspace budget](evolution/2026-10-18-native-composite-link-metaspace.md) |
| KMP gate；JUnit XML；Kotlin scripts；DOCTYPE；XXE；Android nested reports | [KMP report guard parser and execution checks](evolution/2026-10-18-kmp-report-xml-parser.md) |
| ValueCodec；ENCODE；logical input；BasicValueCodec；TemporalValueCodec；String to Int；coercion guard | [ENCODE 类型保护必须保留内置 coercion](evolution/2026-07-21-encode-built-in-coercion-guard.md) |
| JDBC result mapping；ResultColumnMetadata；raw/vendor read；single-pass decode；factory reuse；LOB；SQLXML；PGobject；Oracle LONG | [JDBC 结果必须分离物理读取与逻辑解码](evolution/2026-07-21-jdbc-result-codec-single-pass.md) |
| pagination；count wrapper；prepared parameters；double encode；listParameterOccurrences；empty fieldsMap | [重渲染 SQL 不得重新准备参数](evolution/2026-07-21-rerender-prepared-parameters.md) |
| generated code；`Patch.kt`；detekt；Codacy；static analysis；codegen template | [Generated sources need durable static-analysis policy](evolution/2026-07-20-generated-source-static-analysis.md) |
| non-root JOIN cascade projection；hidden local key；`profile_id`；owner source | [Non-root JOIN cascade must preserve the owner local key](evolution/2026-07-20-join-cascade-owner-local-key.md) |
| runtime `__tableName` override；stale qualifier；select/page；JOIN；UNION；DDL | [Runtime table overrides must rebind qualifiers](evolution/2026-07-20-runtime-table-name-qualifier-binding.md) |
| ValueCodec；`KType`；`starProjectedType`；primitive；boxed；`javaObjectType`；Boolean encode | [运行时 KType 赋值判断必须归一化 primitive boxing](evolution/2026-07-20-runtime-ktype-primitive-boxing.md) |
| TemporalValueSupport；`java.sql`；`NoClassDefFoundError`；GeneratedClassLoader；optional JDBC KType；lazy holder | [Temporal codec 不得提前解析可选 JDBC 类型](evolution/2026-07-21-temporal-optional-jdbc-classloader.md) |
| typed query；`KSelectable.pojo`；generic `Source`；DSL 泛型擦除；member extension；JVM signature clash | [Typed query properties and JVM signatures](evolution/2026-07-19-typed-query-properties-and-jvm-signatures.md) |
| condition IR；普通对象属性变列；顶层属性 NPE；`KPojo.value`；嵌套 `value`；函数参数 | [Condition field lowering must classify the receiver](evolution/2026-07-17-condition-field-lowering-source-ownership.md) |
| FIR condition diagnostic；`takeIf`；`takeUnless`；`if`；`when`；Kotlin control flow；`.value` 误报 | [Condition diagnostics must respect Kotlin control flow](evolution/2026-07-17-condition-diagnostics-control-flow-boundary.md) |
| IDEA plugin；projection completion；`it.`；空 selector；window alias；`rn` 补全缺失 | [IDEA 投影补全必须支持空 selector](evolution/2026-07-13-idea-projection-completion-empty-selector.md) |
| derived select；no projection；logical output names；`@Column("user_name")`；`selected columns were [id, name]` | [派生查询外层必须引用逻辑输出名](evolution/2026-07-13-derived-select-logical-output-names.md) |
| `Kronos example projects`；外部 example；`kronosColumns()`；`__columns`；元数据 API 迁移 | [外部示例元数据 API 漂移](evolution/2026-07-13-external-example-metadata-api-drift.md) |
| SQL Server `total_count` 无列名；Oracle Map `NUMBER` 返回 `BigDecimal` | [分页计数列命名与 Map 输出类型](evolution/2026-07-10-total-count-alias-and-map-column-types.md) |
| H2；`MERGE`；`WHEN MATCHED AND`；`RETURNING`；`FOR SHARE`；precision metadata；`DOUBLE PRECISION`；schema sync | [H2 方言兼容性与精度元数据](evolution/2026-07-23-h2-dialect.md) |
| DM8；native identity；generated keys；`prepareStatement(String, String[])`；generated system index；unique constraint；healthcheck；license | [DM8 Native-Mode Dialect Support](evolution/2026-07-23-dm8-native-mode.md) |
