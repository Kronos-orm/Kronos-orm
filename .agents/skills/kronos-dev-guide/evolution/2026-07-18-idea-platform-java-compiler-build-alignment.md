# IDEA 平台与 Java compiler 必须使用同一正式 build

## 症状

IDEA 插件已经把平台版本设为 `2026.2`，但本地自动探测仍可能选择 EAP 安装，CI 的 `javaCompiler` 也可能继续使用旧 build。这样本地构建结果与正式发布目标不一致，插件 artifact 失败时还可能被 release workflow 跳过。

## 原因

IntelliJ Platform 依赖与 `com.jetbrains.intellij.java` instrumentation 依赖分别配置。只更新 `ideaVersion` 不会同步更新 `javaCompiler`，隐式本地路径还会优先于远程平台坐标。

## 解决

- 从正式 IDEA 的 `product-info.json` 读取 build number，同时更新平台版本和 `javaCompiler` build。
- 本地自动探测只匹配正式安装目录；没有本地安装时才下载正式平台。
- release workflow 不得忽略 `buildPlugin` 失败，插件 zip 缺失也必须终止发布。

IDEA 2026.2 正式版验证值为 `2026.2 / 262.8665.258`。

## 预防

升级 IDEA 目标版本时同时检查 `ideaVersion`、`javaCompiler`、CI 参数和本地路径示例，并用目标正式安装完整运行一次 `buildPlugin`。最后检查生成 manifest 的 `Platform-Version` 与 `Platform-Build`。
