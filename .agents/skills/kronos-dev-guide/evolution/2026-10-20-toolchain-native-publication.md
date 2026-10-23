# Kotlin Toolchain Native 生成与发布

已在 Kotlin Toolchain 0.13.0、Kotlin 2.4.21 的真实 syntax/core/logging 模块上验证。官方 schema 缺少 watchosX64，保留全部 14 个内置 Native targets，并通过项目插件任务调用同版本官方 konanc 构建 watchos_x64。

- 插件 generated fragment descriptor 的 `modifier` 使用 `@jvm`、`@native`；不能省略 `@`。带变量的 descriptor 必须写 `fragment: { modifier: ... }`，不能把字符串直接作为 fragment。
- 新生成 Kotlin source 文件承担 Native entry、dependency aggregate 和 projection metadata anchor。JVM ServiceLoader descriptor 分 main/test 只贡献给 JVM fragment。Native TSV 由发布 finalizer 放入 root metadata JAR，不能放入 JVM ServiceLoader resources。
- 标准 `publish` 不会把 generated resources 自动装入 root metadata JAR。finalizer 同时更新 root `.module` 的 available-at 变体与 metadata JAR 内的 `kotlin-project-structure-metadata.json`；后者的 variant name 不带 `-published`。watchOS x64 的 sourceSets 使用实际存在的 commonMain/nativeMain/appleMain/watchosMain，不能继承 macosMain。
- Native leaf `.module` 的 component 保持指向 root publication，依赖继续使用 root coordinates。Native leaf POM 则包含目标专用依赖，例如 `kronos-syntax-macosx64` 和 `kotlinx-datetime-macosx64`；克隆模板时必须将这些 artifactId 一并转成 watchosx64。Gradle 优先读取 `.module`，只跑 Gradle consumer 会漏掉 POM 错误。
- 新增 watch KLIB、sources JAR 和修改 metadata JAR 后必须重算 `.module` 文件内 size/MD5/SHA1/SHA256/SHA512 与 checksum sidecars，再签名。finalizer 使用单独的 unsigned 输入和最终输出仓库。
- 多模块 finalizer 的输入仓库必须依赖上一个模块的最终仓库：syntax → core → logging。分别克隆最初 stage 会丢失前面补入的变体。声明路径输入可让 `do finalizeNativePublication -m kronos-logging` 自动串起完整链。
- syntax 的初始仓库由前一条独立 `publish` CLI 命令创建，使用 `bootstrapStageRepository` 与 `@Input(inferTaskDependency = false)` 表达跨调用输入；core/logging 保持 `stageRepository` 的普通 `@Input` 来推导前置 finalizer。两种输入均参与文件变化检查。不要对整个 finalizer 输入关闭依赖推导，否则会破坏三模块链，也不要把初始仓库伪装成当前任务的输出。
- 外部 watch KLIB 从 Maven module metadata 选择真实 watchos_x64 API 变体，递归依赖并校验强 digest、size、KLIB native_targets；stdlib 与旧版 stdlib-common 均由根 TOML 指定版本的 SDK 提供。不能把 JVM JAR、macOS KLIB 或开发机缓存路径冒充 watch 依赖。

验证证据：隔离项目真实标准发布、三模块 watch 编译/finalizer 和独立 15 个 root/leaf `.module` checksum/variant/POM 审计通过；全新 GRADLE_USER_HOME 的普通 KGP consumer 只依赖已发布 logging，JVM 两个测试（含 ServiceLoader/registry freeze）及 watch 编译、三个 watch leaf manifest 检查均通过。bridge 七个单元测试通过。官方 SDK 冷下载、SHA256 校验、解压及真实 syntax watch 编译通过。

根命令为 `scripts/run build do finalizeNativePublication -m kronos-logging`，前置是标准发布已经填充 `build/repository/bootstrap`；最终仓库在 `build/tasks/_kronos-logging_finalizeNativePublication@kronos-native/repository`。签名与上传使用最终仓库。

初始仓库输入拆分后，真实 `./kotlin show tasks` 确认 core finalizer 依赖 syntax、logging finalizer 依赖 core，配置输出无 WARNING/ERROR；插件 8 项单元测试通过。证据：`/tmp/kronos-finalizer-task-graph.log`、`/tmp/kronos-warning-api-tests.log`。
