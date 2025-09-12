# Kronos-orm 版本发布与快照发布（GitHub Actions）

本仓库在 PR 合并到 `main` 分支后，会自动执行发布工作流。

支持两种发布流程：
- 快照发布：当当前工程版本以 `-SNAPSHOT` 结尾时，发布到 Maven Central Snapshots（不改版本号）。
- 正式发布：当当前工程版本不包含 `-SNAPSHOT` 时，发布正式版到 Maven Central，并在发布后立即将工程版本号提升到下一个 `-SNAPSHOT`。

## 摘要
- 版本号由工作流统一维护并保持以下位置一致：
  - build-logic/src/main/kotlin/publishing.gradle.kts → `project.version = "..."`
  - kronos-gradle-plugin/src/main/kotlin/com/kotlinorm/compiler/plugin/KronosGradlePlugin.kt → `version = "..."`
- 使用的发布任务（已在 build-logic 中提供）：
  - 快照：`publishAllToCentralSnapshots`
  - 正式：`publishAllToMavenCentral`

## 触发条件
工作流文件：`.github/workflows/publish.yml`
- 当 PR 被关闭且“已合并”到 `main` 时触发。
- 工作流会从 `publishing.gradle.kts` 中读取当前版本号：
  - 若以 `-SNAPSHOT` 结尾，则执行“快照发布”（不提升版本）。
  - 若不包含 `-SNAPSHOT`，则执行“正式发布”，并在发布后将版本号提升到下一个 `-SNAPSHOT`。

## 必需的 Secrets 配置
请在 GitHub 仓库（或组织级）配置以下 Secrets：
- `MAVEN_CENTRAL_USERNAME`：Sonatype 用户名
- `MAVEN_CENTRAL_PASSWORD`：Sonatype 密码
- `SIGNING_KEY`：ASCII 装甲格式的 GPG 私钥（使用 `gpg --export-secret-keys --armor <KEYID>` 导出）
- `SIGNING_PASSWORD`：该私钥的口令

说明：
- 按当前项目配置，快照发布不需要签名，仅使用 Sonatype 凭证即可。
- 正式发布需要签名；工作流通过内存方式注入签名信息，对应以下 Gradle 属性环境变量：
  - `ORG_GRADLE_PROJECT_signingInMemoryKey`
  - `ORG_GRADLE_PROJECT_signingInMemoryKeyPassword`

## Keystore / 签名最佳实践
仓库中不应保存任何真实的密钥材料，建议全部放入 GitHub Secrets：
- 将 GPG 私钥文本保存为 `SIGNING_KEY`（ASCII 文本，不是文件），口令保存为 `SIGNING_PASSWORD`。
- 工作流会把上述 Secrets 映射为 Gradle/Vanniktech 插件可识别的内存签名属性。

如需为额外的目标仓库（例如阿里云、私有仓库）提供文件形式的凭证：
- 在本地将文件 base64 编码，作为 Secret 保存（例如 `ALIYUN_KEYSTORE_B64`）。
- 在工作流步骤中解码到临时路径（例如 `echo "$ALIYUN_KEYSTORE_B64" | base64 -d > keystore/tmp.gpg`）。
- 通过 `ORG_GRADLE_PROJECT_...` 形式的 Gradle 属性在运行期指向该临时文件。

## 版本号提升逻辑
仓库包含辅助脚本 `.github/scripts/bump-version.sh`，用于在所有必要位置统一更新版本号。

支持的命令：
- `next-snapshot`：补丁号 +1，并追加 `-SNAPSHOT`（例如 0.0.6-SNAPSHOT → 0.0.7-SNAPSHOT）
- `release-from-current`：如果当前为 `*-SNAPSHOT`，则去掉 `-SNAPSHOT`；否则保持不变
- `next-release`：补丁号 +1，生成“下一个正式版本”（无 `-SNAPSHOT`）
- `set <version>`：显式设置版本（如 `set 0.0.7` 或 `set 0.0.8-SNAPSHOT`）

工作流会使用以上命令，确保以下文件中的版本保持一致：
- publishing.gradle.kts
- KronosGradlePlugin.kt

## 使用示例
- 快照发布：将任意 PR 合并到 `main`，标题不含 `[version]`。工作流会提升到下一个 `-SNAPSHOT` 并发布到 Central Snapshots。
- 正式发布：将 PR 合并到 `main`，且标题包含 `[version]`：
  - 自动确定版本：`Fix: ready for release [version]`
  - 显式指定版本：`Release Kronos [version 0.0.7]`

## 本地测试建议
- 仅本地试运行版本号变更：
  - `bash .github/scripts/bump-version.sh next-snapshot`
  - `bash .github/scripts/bump-version.sh release-from-current`
- 发布任务：
  - 快照：`./gradlew publishAllToCentralSnapshots`
  - 正式：`./gradlew publishAllToMavenCentral`

请务必不要将任何真实的凭证或 keystore 文件提交到仓库。